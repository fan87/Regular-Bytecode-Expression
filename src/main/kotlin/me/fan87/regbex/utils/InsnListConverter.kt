package me.fan87.regbex.utils

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList

fun InsnList.toArrayList(): ArrayList<AbstractInsnNode> = ArrayList<AbstractInsnNode>().also { this.forEach { ele -> it.add(ele) } }
fun Iterable<AbstractInsnNode>.toInsnList(): InsnList {
    val out = InsnList()
    for (abstractInsnNode in this) {
        out.add(abstractInsnNode)
    }
    return out
}