package me.fan87.regbex

import org.objectweb.asm.tree.AbstractInsnNode

internal class MatchingInstance constructor(
    val parentStartIndex: Int,
    val startIndex: Int,
    var nestedLevel: Int,
    val elements: List<RegbexMatchElement>,
    val matcher: RegbexMatcher,
    var goBack: (amount: Int) -> Unit,
    var onFailed: () -> Unit,
    var onSuccess: (matched: ArrayList<AbstractInsnNode>, captured: ArrayList<List<AbstractInsnNode>>, capturedNamed: HashMap<String, List<AbstractInsnNode>>) -> Boolean,
    var onProvided: (index: Int, instruction: AbstractInsnNode, last: Boolean) -> Unit = {_, _, _ -> },
    var onEndOfFile: () -> Unit = {}
) {

    val matched = ArrayList<AbstractInsnNode>()
    val capturedNamed = HashMap<String, List<AbstractInsnNode>>()
    val captured = ArrayList<List<AbstractInsnNode>>()

    var currentElementIndex: Int = 0

    val waiting = ArrayList<MatchingInstance>()

    var hasFailed = false
    var hasSucceeded = false

    private fun failed() {
        if (!hasSucceeded && !hasFailed) {
            hasFailed = true
            onFailed()
        } else {
            throw IllegalStateException("Failed: $hasFailed & Succeed: $hasSucceeded, but still attempting to fail")
        }
    }

    private fun success(matched: ArrayList<AbstractInsnNode>, captured: ArrayList<List<AbstractInsnNode>>, capturedNamed: HashMap<String, List<AbstractInsnNode>>): Boolean {
        if (!hasSucceeded && !hasFailed) {
            hasSucceeded = true
            return onSuccess(matched, captured, capturedNamed)
        } else {
            throw IllegalStateException("Failed: $hasFailed & Succeed: $hasSucceeded, but still attempting to succeed")
        }
        return true
    }

    fun endOfFile() {
        onEndOfFile()
        if (waiting.isNotEmpty()) {
            for (matchingInstance in ArrayList(waiting)) {
                matchingInstance.endOfFile()
                if (matchingInstance.hasSucceeded) {
                    if (currentElement() == null) {
                        success(matched, captured, capturedNamed)
                    }
                    return
                }
                if (waiting.isEmpty()) {
                    break
                }
            }
            return
        }
    }

    fun provideNext(index: Int, instruction: AbstractInsnNode, last: Boolean) {
        println("${getIndent()}[B] Index: $index")
        if (waiting.isNotEmpty()) {
            for (matchingInstance in ArrayList(waiting)) {
                matchingInstance.provideNext(index, instruction, last)
                if (matchingInstance.hasSucceeded) {
                    if (currentElement() == null) {
                        success(matched, captured, capturedNamed)
                    }
                    return
                }
                if (waiting.isEmpty()) {
                    break
                }
            }
            return
        }
        println("${getIndent()}[A] Index: $index")

        val currentElement = currentElement()

        onProvided(index, instruction, last)
        matched.add(instruction)
        println(getIndent() + "Check ($currentElementIndex): ${currentElement?.javaClass?.simpleName}")

        if (currentElement is CustomCheck) {
            if (currentElement.check(instruction)) {
                val next = nextElement()
                if (next == null) {
                    success(matched, captured, capturedNamed)
                }
                println(getIndent() + "Passed CustomCheck $index")
                return
            }
            println(getIndent() + "Failed CustomCheck $index")
            failed()
        }
        if (currentElement is Group) {
            val instance = MatchingInstance(
                parentStartIndex,
                index,
                nestedLevel + 1,
                ArrayList(currentElement.regbex.elements),
                matcher,
                goBack,
                onFailed,
                {_, _, _ -> true}
            )
            instance.onSuccess = { matched: ArrayList<AbstractInsnNode>, captured: ArrayList<List<AbstractInsnNode>>, capturedNamed: HashMap<String, List<AbstractInsnNode>> ->
                waiting.clear()
                this.matched.addAll(matched)
                for (mutableEntry in capturedNamed) {
                    this.capturedNamed[mutableEntry.key] = mutableEntry.value
                }
                for (abstractInsnNodes in captured) {
                    this.captured.add(abstractInsnNodes)
                }
                this.captured.add(matched)
                if (currentElement.name != null) {
                    this.capturedNamed[currentElement.name!!] = matched
                }
                true
            }
            waiting.add(instance)
            println(getIndent() + "Started nested group instance ${currentElement.name}")
            instance.provideNext(index, instruction, last)
            nextElement()
            if (instance.hasSucceeded && currentElement() == null) {
                success(matched, captured, capturedNamed)
            }
            return
        }
        if (currentElement is AmountOf) {
            val instance = MatchingInstance(
                parentStartIndex,
                index,
                nestedLevel + 1,
                ArrayList(currentElement.regbex.elements),
                matcher,
                goBack,
                {},
                { _, _, _ -> true})

            var startIndex = index
            var counter = 0
            // Exclusive
            var lastEndIndex = -1


            instance.onFailed = onFailed@{
                println(instance.getIndent() + "Failed: $counter / ${currentElement.range}")
                if (counter < currentElement.range.first) {
                    failed()
                    return@onFailed
                }
                if (lastEndIndex == -1) {
                    failed()
                    return@onFailed
                }
                println(instance.getIndent() + "Removed from onFailed")
                waiting.clear()
                this.matched.addAll(matched)
                for (mutableEntry in capturedNamed) {
                    this.capturedNamed[mutableEntry.key] = mutableEntry.value
                }
                for (abstractInsnNodes in captured) {
                    this.captured.add(abstractInsnNodes)
                }
                this.captured.add(matched)

                goBack(lastEndIndex)
                instance.hasFailed = false
                instance.hasSucceeded = true
            }
            instance.onEndOfFile = {
                instance.failed()
            }
            instance.onSuccess = onSuccess@{ matched, captured, capturedNamed ->
                counter++
                println(instance.getIndent() + "Success: $counter / ${currentElement.range}")
                if (counter > currentElement.range.last) {
                    if (lastEndIndex == -1) {
                        failed()
                    } else {
                        println(instance.getIndent() + "Removed from onSuccess")
                        waiting.clear()
                        this.matched.addAll(matched)
                        for (mutableEntry in capturedNamed) {
                            this.capturedNamed[mutableEntry.key] = mutableEntry.value
                        }
                        for (abstractInsnNodes in captured) {
                            this.captured.add(abstractInsnNodes)
                        }
                        this.captured.add(matched)

                        goBack(lastEndIndex)
                        instance.hasSucceeded = true
                    }
                    return@onSuccess false
                }
                if (counter in currentElement.range) {
                    lastEndIndex = startIndex + matched.size
                }
                instance.currentElementIndex = 0
                instance.hasSucceeded = false
                println(instance.getIndent() + "Succeeded! Going back to loop")

                false
            }
            waiting.add(instance)
            println(getIndent() + "Started nested amountOf instance")
            instance.provideNext(index, instruction, last)
            nextElement()
            return
        }
        if (currentElement is Or) {
            var i = -1
            nextElement()
            for (regbex in currentElement.regbex) {
                i++
                val instance = MatchingInstance(
                    parentStartIndex,
                    index,
                    nestedLevel + 1,
                    ArrayList(regbex.elements),
                    matcher,
                    goBack,
                    {},
                    {_, _, _ -> true}
                )

                instance.onFailed = {
                    waiting.remove(instance)
                }

                instance.onSuccess = { matched: ArrayList<AbstractInsnNode>, captured: ArrayList<List<AbstractInsnNode>>, capturedNamed: HashMap<String, List<AbstractInsnNode>> ->
                    waiting.clear()
                    this.matched.addAll(matched)
                    for (mutableEntry in capturedNamed) {
                        this.capturedNamed[mutableEntry.key] = mutableEntry.value
                    }
                    for (abstractInsnNodes in captured) {
                        this.captured.add(abstractInsnNodes)
                    }
                    this.captured.add(matched)

                    true
                }
                println(getIndent() + "Started nested or instance $i")
                waiting.add(instance)
                instance.provideNext(index, instruction, last)
                if (instance.hasSucceeded) {
                    if (currentElement() == null) {
                        success(matched, captured, capturedNamed)
                    }
                    break
                }
            }


            return
        }


    }


    private fun currentElement(): RegbexMatchElement? {
        if (currentElementIndex >= elements.size) {
            return null
        }
        return elements[currentElementIndex]
    }

    private fun nextElement(): RegbexMatchElement? {
        currentElementIndex++
        return currentElement()
    }

    private fun getIndent(): String {
        return "  ".repeat(nestedLevel)
    }

}