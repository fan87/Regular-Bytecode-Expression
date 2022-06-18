package me.fan87.regbex

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode

/**
 * While using [RegbexMatcher.replace], you could add this to tells the engine to replace the instructions to content
 * of specified group with name [groupName]. [groupName] is nullable, if null, the engine will replace it to
 * [RegbexMatcher.matched].
 *
 * If the group is not found, it will handle it as an empty group, and wouldn't throw any exception. If you would like to
 * know if any of the group could not be found, consider checking if group is defined before replacing.
 */
class ReplacePlaceHolder(val groupName: String?): AbstractInsnNode(-1) {
    override fun getType(): Int {
        TODO("Not yet implemented")
    }

    override fun accept(methodVisitor: MethodVisitor?) {
        TODO("Not yet implemented")
    }

    override fun clone(clonedLabels: MutableMap<LabelNode, LabelNode>?): AbstractInsnNode {
        TODO("Not yet implemented")
    }
}