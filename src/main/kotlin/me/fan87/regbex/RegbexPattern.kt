package me.fan87.regbex

import org.objectweb.asm.tree.AbstractInsnNode

class RegbexPattern(builder: RegbexBuilder) {

    internal val regbex: Regbex = Regbex()

    init {
        regbex.builder()
    }

    fun matcher(insnList: Iterable<AbstractInsnNode>): RegbexMatcher {
        return RegbexMatcher(insnList, this)
    }

}