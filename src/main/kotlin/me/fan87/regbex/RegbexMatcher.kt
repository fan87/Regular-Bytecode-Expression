package me.fan87.regbex

import org.objectweb.asm.tree.AbstractInsnNode

class RegbexMatcher internal constructor(instructions: Iterable<AbstractInsnNode>, private val pattern: RegbexPattern) {

    val instructions = ArrayList<AbstractInsnNode>()

    init {
        for (instruction in instructions) {
            this.instructions.add(instruction)
        }
    }


    private var capturedNamed = HashMap<String, RegbexRegion>()
    private var captured = ArrayList<RegbexRegion>()

    private var matched: RegbexRegion? = null
    private var startFindingIndex = 0

    private var matchedAny = false


    fun next(startIndex: Int): Boolean {
        return next(startIndex, Int.MAX_VALUE)
    }

    fun next(startIndex: Int, instanceLimit: Int): Boolean {
        this.startFindingIndex = startIndex
        val regbex = pattern.regbex
        val target = instructions.subList(startIndex, instructions.size)
        val matchingInstances = ArrayList<MatchingInstance>()
        var created = 0
        var index = 0
        while (index in 0 until target.size) {
            var newIndex = index
            val insn = target[index]
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
                }, {}, {_, _, _ -> true})
                matchingInstance.onSuccess = { matched, captured, capturedNamed ->
                    this.matched = matched
                    this.captured = captured
                    this.capturedNamed = capturedNamed
                    this.matchedAny = true
                    if (matchingInstance.parentStartIndex != matched.start) {
                        throw Exception("NO?")
                    }
                    true
                }
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
        matchedAny = false
        return false
    }


    /**
     * Get the inclusive start of a group
     */
    fun groupStart(groupName: String): Int? {
        checkMatched()
        return capturedNamed[groupName]?.start
    }

    /**
     * Get the exclusive end of a group
     */
    fun groupEnd(groupName: String): Int? {
        checkMatched()
        return capturedNamed[groupName]?.end?.plus(1)
    }

    fun replaceGroup(groupName: String, replaceTo: Iterable<AbstractInsnNode>): ArrayList<AbstractInsnNode> {
        checkMatched()
        if (group(groupName) == null) {
            return ArrayList(instructions)
        }
        val newInstructions = ArrayList<AbstractInsnNode>()

        for (instruction in instructions.withIndex()) {
            val index = instruction.index
            val insn = instruction.value

            if (index < groupStart(groupName)!! || index >= groupEnd(groupName)!!) {
                newInstructions.add(insn)
                continue
            }
            if (index == groupStart(groupName)!!) {
                newInstructions.addAll(replaceTo)
            }
        }
        return newInstructions
    }

    fun replace(replaceTo: Iterable<AbstractInsnNode>): ArrayList<AbstractInsnNode> {
        checkMatched()
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
        return newInstructions
    }

    /**
     * Exclusive
     */
    fun endIndex(): Int {
        checkMatched()
        return matched!!.end + startFindingIndex + 1
    }

    /**
     * Inclusive
     */
    fun startIndex(): Int {
        checkMatched()
        return matched!!.start + startFindingIndex
    }

    fun group(): List<AbstractInsnNode> {
        return group(0)!!
    }

    fun group(index: Int): List<AbstractInsnNode>? {
        checkMatched()
        if (captured.size < index) {
            return null
        }
        if (index <= 0) {
            return getRegion(matched)
        }
        return getRegion(captured[index - 1])
    }

    fun group(name: String): List<AbstractInsnNode>? {
        checkMatched()
        return getRegion(capturedNamed[name])
    }

    private fun getRegion(regbexRegion: RegbexRegion?): List<AbstractInsnNode>? {
        if (regbexRegion == null) {
            return null
        }
        checkMatched()
        return instructions.subList(startFindingIndex + regbexRegion.start, startFindingIndex + regbexRegion.end + 1)
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