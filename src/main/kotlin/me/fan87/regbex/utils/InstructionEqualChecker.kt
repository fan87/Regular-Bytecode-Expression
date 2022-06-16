package me.fan87.regbex.utils

import org.objectweb.asm.tree.AbstractInsnNode
import java.lang.reflect.Modifier

object InstructionEqualChecker {

    fun checkEquals(first: AbstractInsnNode, second: AbstractInsnNode): Boolean {
        if (first.javaClass != second.javaClass) {
            return false
        }
        for (field in first.javaClass.fields) {
            if (Modifier.isStatic(field.modifiers)) continue
            if (Modifier.isFinal(field.modifiers)) continue
            if (!Modifier.isPublic(field.modifiers)) continue
            if (field[first] != field[second]) {
                println("${field[first]} != ${field[second]}")
                return false
            }
        }
        return true
    }

}