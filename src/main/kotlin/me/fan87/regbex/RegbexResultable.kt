package me.fan87.regbex

import org.objectweb.asm.tree.AbstractInsnNode

interface RegbexResultable {

    /**
     * End index of the match (Exclusive)
     */
    fun endIndex(): Int

    /**
     * Start index of the match (Inclusive)
     */
    fun startIndex(): Int

    /**
     * Number of instructions that were matched
     */
    fun matchedSize(): Int

    /**
     * Get matched instructions
     */
    fun group(): List<AbstractInsnNode>

    /**
     * Get content of captured named group. Returns null if the group is not found or not captured
     */
    fun group(name: String): List<AbstractInsnNode>?

    /**
     * Get the pattern of the result
     */
    fun pattern(): RegbexPattern

    /**
     * Get the start of a group (Inclusive)
     */
    fun groupStart(groupName: String): Int?

    /**
     * Get the end of a group (Exclusive)
     */
    fun groupEnd(groupName: String): Int?

    /**
     * Get the real content that's going to be used in replacement.
     */
    fun getRealReplaceContent(instructions: Iterable<AbstractInsnNode>): List<AbstractInsnNode> {
        val output = ArrayList<AbstractInsnNode>()
        for (instruction in instructions) {
            if (instruction is ReplacePlaceHolder) {
                val group = if (instruction.groupName == null) {
                    matched()
                } else {
                    group(instruction.groupName)?:throw NullPointerException("Group with name ${instruction.groupName} is not found!")
                }
                output.addAll(group)
            } else {
                output.add(instruction)
            }
        }
        return output
    }

    /**
     * Calculate the new end index after replacing something (exclusive)
     */
    fun endIndexAfterReplacing(replaceSize: Int): Int {
        return endIndex() + (replaceSize - matchedSize())
    }

    /**
     * Calculate the new end index after replacing something (exclusive)
     */
    fun endIndexAfterReplacing(instructions: ReplaceTarget): Int {
        return endIndex() + (getRealReplaceContent(instructions).size - matchedSize())
    }

    /**
     * Replace the matched result with [replaceTo], and return the replaced list of instructions
     */
    fun replace(replaceTo: ReplaceTarget): ArrayList<AbstractInsnNode>

    /**
     * Replace captured named group with name [groupName], and return the replaced list of instructions
     */
    fun replaceGroup(groupName: String, replaceTo: ReplaceTarget): ArrayList<AbstractInsnNode>

    /**
     * Get matched instructions
     */
    fun matched(): List<AbstractInsnNode> {
        return group()
    }

}

typealias ReplaceTarget = Iterable<AbstractInsnNode>