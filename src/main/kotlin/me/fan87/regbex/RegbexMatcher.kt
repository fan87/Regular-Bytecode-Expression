package me.fan87.regbex

import org.objectweb.asm.tree.AbstractInsnNode

class RegbexMatcher internal constructor(instructions: Iterable<AbstractInsnNode>, private val pattern: RegbexPattern) {

    val instructions = ArrayList<AbstractInsnNode>()

    init {
        for (instruction in instructions) {
            this.instructions.add(instruction)
        }
    }

    internal var ignoreRules: (AbstractInsnNode) -> Boolean = { false }

    private var capturedNamed = HashMap<String, List<AbstractInsnNode>>()
    private var captured = ArrayList<List<AbstractInsnNode>>()

    private var matched = ArrayList<AbstractInsnNode>()
    private var matchedStartIndex = 0

    private var matchedAny = false

    fun addIgnoreRule(rule: (AbstractInsnNode) -> Boolean) {
        val oldIgnoreRule = this.ignoreRules
        this.ignoreRules = {
            oldIgnoreRule(it) && rule(it)
        }
    }

    fun next(startIndex: Int): Boolean {
        return next(startIndex, Int.MAX_VALUE)
    }

    fun next(startIndex: Int, instanceLimit: Int): Boolean {
        val regbex = pattern.regbex
        val target = instructions.subList(startIndex, instructions.size)
        val matchingInstances = ArrayList<MatchingInstance>()
        var created = 0
        var index = 0
        while (index in 0 until target.size) {
            var newIndex = index
            val insn = target[index]
            if (ignoreRules(insn)) {
                continue
            }
            for (matchingInstance in ArrayList(matchingInstances)) {
                matchingInstance.provideNext(index, insn, index == target.size - 1)
                if (matchedAny) {
                    return true
                }
            }

            if (created < instanceLimit) {
                created++
                val matchingInstance = MatchingInstance(index, index, 0, ArrayList(regbex.elements), this, {
                    newIndex = it
                }, {}, { matched: ArrayList<AbstractInsnNode>, captured: ArrayList<List<AbstractInsnNode>>, capturedNamed: HashMap<String, List<AbstractInsnNode>> ->
                    this.matched = matched
                    this.captured = captured
                    this.capturedNamed = capturedNamed
                    this.matchedAny = true
                    true
                })
                matchingInstance.onFailed = {
                    matchingInstances.remove(matchingInstance)
                }

                matchingInstances.add(matchingInstance)
                matchingInstance.provideNext(index, insn, index == target.size - 1)
            }
            if (matchedAny) {
                return true
            }
            index = newIndex
            index++
        }
        for (matchingInstance in ArrayList(matchingInstances)) {
            matchingInstance.endOfFile()
            if (matchedAny) {
                return true
            }
        }
        return false
    }

    fun replace(replaceTo: Iterable<AbstractInsnNode>) {
        val newInstructions = ArrayList<AbstractInsnNode>()

        for (instruction in instructions.withIndex()) {
            val index = instruction.index
            val insn = instruction.value

            if (index < startIndex() || index >= endIndex()) {
                newInstructions.add(insn)
                continue
            }
            if (index == startIndex()) {
                newInstructions.addAll(replaceTo)
            }
        }
        instructions.clear()
        instructions.addAll(newInstructions)

    }

    /**
     * Exclusive
     */
    fun endIndex(): Int {
        return matchedStartIndex + matched.size
    }

    /**
     * Inclusive
     */
    fun startIndex(): Int {
        return matchedStartIndex
    }

    fun group(index: Int): List<AbstractInsnNode>? {
        checkMatched()
        if (captured.size < index) {
            return null
        }
        if (index <= 0) {
            return matched
        }
        return captured[index - 1]
    }

    fun group(name: String): List<AbstractInsnNode>? {
        checkMatched()
        return capturedNamed[name]
    }

    fun pattern(): RegbexPattern {
        return this.pattern
    }

    private fun checkMatched() {
        if (!matchedAny) {
            throw IllegalStateException("Do RegbexMatcher.next() before getting the result")
        }
    }

}