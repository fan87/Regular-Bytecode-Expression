package me.fan87.regbex

import org.objectweb.asm.tree.AbstractInsnNode

class RegbexPattern(val regbex: Regbex) {



    constructor(regbexBuilder: RegbexBuilder) : this(Regbex().apply(regbexBuilder))

    fun matcher(insnList: Iterable<AbstractInsnNode>): RegbexMatcher {
        return RegbexMatcher(insnList, this)
    }

}