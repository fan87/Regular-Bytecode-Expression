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
     * Replace the matched result with [replaceTo], and return the replaced list of instructions
     */
    fun replace(replaceTo: Iterable<AbstractInsnNode>): ArrayList<AbstractInsnNode>

    /**
     * Replace captured named group with name [groupName], and return the replaced list of instructions
     */
    fun replaceGroup(groupName: String, replaceTo: Iterable<AbstractInsnNode>): ArrayList<AbstractInsnNode>

    /**
     * Get matched instructions
     */
    fun matched(): List<AbstractInsnNode> {
        return group()
    }

}