package me.fan87.regbex

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode

class RegbexPattern(val regbex: Regbex) {


    constructor(regbexBuilder: RegbexBuilder) : this(Regbex().apply(regbexBuilder))

    /**
     * New matcher instance of an instructions list. You may assume the instruction list provided will be cloned,
     * so you won't have to put it into another list to avoid changing, which also means that you could provide
     * an immutable list, and it should work fine.
     */
    fun matcher(insnList: Iterable<AbstractInsnNode>): RegbexMatcher {
        return RegbexMatcher(insnList, this)
    }

    /**
     * New matcher instance of a method's instructions
     */
    fun matcher(method: MethodNode): RegbexMatcher {
        return RegbexMatcher(method.instructions, this)
    }

    /**
     * Replace everything that matches with [insnList]
     */
    fun replaceAll(original: Iterable<AbstractInsnNode>, insnList: ReplaceTarget): List<AbstractInsnNode> {
        var out = ArrayList<AbstractInsnNode>()
        var matcher = matcher(original)
        var index = 0
        while (matcher.next(index)) {
            index = matcher.endIndexAfterReplacing(insnList)
            out = matcher.replace(insnList)
            matcher = matcher(out)
        }
        return out
    }


}