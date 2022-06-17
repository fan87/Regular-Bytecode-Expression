import me.fan87.regbex.RegbexPattern
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomCheckTest {

    @Test
    internal fun customCheckTestA() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKEVIRTUAL
            }
            thenCustomCheck {
                it.opcode == Opcodes.INVOKEVIRTUAL
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }

    @Test
    internal fun customCheckTestB() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
        }.matcher(instructions)

        assertFalse(matcher.next(0), "Shouldn't match InvokeStatic")
    }

}