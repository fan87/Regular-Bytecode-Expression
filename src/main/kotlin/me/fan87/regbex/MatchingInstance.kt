package me.fan87.regbex

import me.fan87.regbex.utils.InstructionEqualChecker
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode

internal class MatchingInstance constructor(
    val parentStartIndex: Int,
    val startIndex: Int,
    var nestedLevel: Int,
    val elements: MutableList<RegbexMatchElement>,
    val matcher: RegbexMatcher,
    var goBack: (amount: Int) -> Unit,
    var onFailed: () -> Unit,
    var onSuccess: (matched: RegbexRegion, captured: ArrayList<RegbexRegion>, capturedNamed: HashMap<String, RegbexRegion>) -> Boolean,
    var onProvided: (index: Int, instruction: AbstractInsnNode, last: Boolean) -> Unit = {_, _, _ -> },
    var onEndOfFile: () -> Unit = {},
    var environmentCapturedNamed: HashMap<String, RegbexRegion> = HashMap(),
    var environmentCaptured: ArrayList<RegbexRegion> = ArrayList(),
) {

    val matched = RegbexRegion(startIndex, startIndex)
    val capturedNamed = HashMap<String, RegbexRegion>()
    val captured = ArrayList<RegbexRegion>()

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

    private fun success(matched: RegbexRegion, captured: ArrayList<RegbexRegion>, capturedNamed: HashMap<String, RegbexRegion>): Boolean {
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
        //println("${getIndent()}[B] Index: $index ($last)")
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
        //println("${getIndent()}[A] Index: $index")

        val currentElement = currentElement()

        onProvided(index, instruction, last)
        matched.end = index
        //println(getIndent() + "Check ($currentElementIndex): ${currentElement?.javaClass?.simpleName}")

        if (currentElement is CustomCheck) {
            if (currentElement.check(instruction)) {
                val next = nextElement()
                if (next == null) {
                    success(matched, captured, capturedNamed)
                }
                //println(getIndent() + "Passed CustomCheck $index")
                return
            }
            //println(getIndent() + "Failed CustomCheck $index")
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
            instance.environmentCapturedNamed = HashMap(capturedNamed)
            instance.environmentCaptured = ArrayList(captured)
            instance.onSuccess = { matched, captured, capturedNamed ->
                waiting.clear()
                this.matched.end = matched.end
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
            //println(getIndent() + "Started nested group instance ${currentElement.name}")
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
            instance.environmentCapturedNamed = HashMap(capturedNamed)
            instance.environmentCaptured = ArrayList(captured)
            var startIndex = index
            var counter = 0
            // Exclusive
            var lastEndIndex = -1


            instance.onFailed = onFailed@{
                if (counter < currentElement.range.first) {
                    failed()
                    return@onFailed
                }
                if (lastEndIndex == -1) {
                    failed()
                    return@onFailed
                }
                waiting.clear()
                this.matched.end += lastEndIndex - index
                for (mutableEntry in capturedNamed) {
                    this.capturedNamed[mutableEntry.key] = mutableEntry.value
                }
                for (abstractInsnNodes in captured) {
                    this.captured.add(abstractInsnNodes)
                }
                this.captured.add(matched)
                instance.matched.end = instance.matched.start

                goBack(lastEndIndex)
                instance.hasFailed = false
                instance.hasSucceeded = true
            }
            instance.onEndOfFile = {
                instance.failed()
            }
            instance.onSuccess = onSuccess@{ matched, captured, capturedNamed ->
                counter++
                if (counter > currentElement.range.last) {
                    if (lastEndIndex == -1) {
                        failed()
                    } else {
                        waiting.clear()
                        this.matched.end += lastEndIndex - index
                        for (mutableEntry in capturedNamed) {
                            this.capturedNamed[mutableEntry.key] = mutableEntry.value
                        }
                        for (abstractInsnNodes in captured) {
                            this.captured.add(abstractInsnNodes)
                        }
                        this.captured.add(matched)
                        instance.matched.end = instance.matched.start

                        goBack(lastEndIndex)
                        instance.hasSucceeded = true
                    }
                    return@onSuccess false
                }
                if (counter in currentElement.range) {
                    lastEndIndex = startIndex + (matched.end - matched.start)
                }
                instance.currentElementIndex = 0
                instance.hasSucceeded = false

                false
            }
            waiting.add(instance)
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
                instance.environmentCapturedNamed = HashMap(capturedNamed)
                instance.environmentCaptured = ArrayList(captured)
                instance.onFailed = {
                    waiting.remove(instance)
                }

                instance.onSuccess = { matched, captured, capturedNamed ->
                    waiting.clear()
                    this.matched.end = matched.end
                    for (mutableEntry in capturedNamed) {
                        this.capturedNamed[mutableEntry.key] = mutableEntry.value
                    }
                    for (abstractInsnNodes in captured) {
                        this.captured.add(abstractInsnNodes)
                    }
                    this.captured.add(matched)

                    true
                }
                //println(getIndent() + "Started nested or instance $i")
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
        if (currentElement is And) {
            var i = -1
            nextElement()
            var succeeded = 0
            val expected = currentElement.regbex.size
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
                instance.environmentCapturedNamed = HashMap(capturedNamed)
                instance.environmentCaptured = ArrayList(captured)
                instance.onFailed = {
                    waiting.remove(instance)
                    failed()
                }

                instance.onSuccess = { matched, captured, capturedNamed ->
                    waiting.remove(instance)

                    for (mutableEntry in capturedNamed) {
                        this.capturedNamed[mutableEntry.key] = mutableEntry.value
                    }
                    for (abstractInsnNodes in captured) {
                        this.captured.add(abstractInsnNodes)
                    }
                    this.captured.add(matched)

                    succeeded++
                    if (succeeded == expected) { // if it's the last one that matches
                        this.matched.end = matched.end
                    }
                    true
                }

                instance.onEndOfFile = {
                    instance.failed()
                }
                waiting.add(instance)


                instance.provideNext(index, instruction, last)
                if (hasFailed) {
                    return
                }
            }

            if (waiting.isEmpty()) {
                if (currentElement() == null) {
                    success(matched, captured, capturedNamed)
                }
            }

            return
        }
        if (currentElement is Not) {
            val instance = MatchingInstance(
                parentStartIndex,
                index,
                nestedLevel + 1,
                ArrayList(currentElement.regbex.elements),
                matcher,
                goBack,
                {  },
                {_, _, _ -> failed(); true}
            )
            instance.environmentCapturedNamed = HashMap(capturedNamed)
            instance.environmentCaptured = ArrayList(captured)
            instance.onFailed = {
                waiting.clear()
                this.matched.end = instance.matched.end
                for (mutableEntry in instance.capturedNamed) {
                    this.capturedNamed[mutableEntry.key] = mutableEntry.value
                }
                for (abstractInsnNodes in instance.captured) {
                    this.captured.add(abstractInsnNodes)
                }
                this.captured.add(matched)
                true
            }
            waiting.add(instance)
            //println(getIndent() + "Started not group instance")
            instance.provideNext(index, instruction, last)
            nextElement()
            if (instance.hasFailed && currentElement() == null) {
                success(matched, captured, capturedNamed)
            }
            return
        }
        if (currentElement is CapturedGroup) {

            val toCheck = ArrayList<AbstractInsnNode>()

            var regbexRegion = capturedNamed[currentElement.name]
            if (regbexRegion == null) {
                regbexRegion = environmentCapturedNamed[currentElement.name]
            }
            if (regbexRegion == null) {
                failed()
                return
            }
            toCheck.addAll(matcher.instructions.subList(matcher.startFindingIndex + regbexRegion.start, matcher.startFindingIndex + regbexRegion.end + 1))

            for (abstractInsnNode in toCheck.reversed()) {
                elements.add(currentElementIndex + 1, CustomCheck{ InstructionEqualChecker.checkEquals(it, abstractInsnNode) })
            }
            nextElement()
            provideNext(index, instruction, last)
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

