import me.fan87.regbex.RegbexPattern
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupTest {


    
    @Test
    internal fun groupTestA() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenGroup("testGroup") {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            }
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals(2, matcher.group("testGroup")!!.size, "testGroup should have 2 instructions")
    }

    @Test
    internal fun groupTestB() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenGroup("") {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESTATIC
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESTATIC
                }
            }
        }.matcher(instructions)

        assertFalse(matcher.next(0), "Shouldn't match InvokeStatic")
    }

    @Test
    internal fun groupTestC() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenGroup("testGroup") {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals(2, matcher.group("testGroup")!!.size, "testGroup should have 2 instructions")
    }


}