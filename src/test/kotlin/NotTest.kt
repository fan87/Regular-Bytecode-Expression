import me.fan87.regbex.RegbexPattern
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotTest {


    
    @Test
    internal fun notTestA() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenGroup("testGroup") {
                thenAmountOf(2) {
                    thenNot {
                        thenCustomCheck {
                            it.opcode == Opcodes.INVOKESTATIC
                        }
                    }
                }
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals(2, matcher.group("testGroup")!!.size)
    }

    @Test
    internal fun notTestB() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenGroup("testGroup") {
                thenAmountOf(2) {
                    thenNot {
                        thenCustomCheck {
                            it.opcode == Opcodes.INVOKESTATIC
                        }
                    }
                }
            }
        }.matcher(instructions)

        assertFalse(matcher.next(0))
    }




}