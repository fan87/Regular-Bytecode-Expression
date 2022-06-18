import me.fan87.regbex.RegbexPattern
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LazyAmountOfTest {

    @Test
    internal fun amountOfTestA() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenLazyAmountOf(2) {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }

    @Test
    internal fun amountOfTestB() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenLazyAmountOf(2) {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            }
        }.matcher(instructions)

        assertFalse(matcher.next(0))
    }

    @Test
    internal fun amountOfTestC() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenLazyAmountOf(2) {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESTATIC
                }
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals(2, matcher.group().size)
    }

    @Test
    internal fun amountOfTestD() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenLazyAmountOf(2) {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            }
        }.matcher(instructions)

        assertFalse(matcher.next(0))
    }
    @Test
    internal fun amountOfTestE() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenLazyAmountOf(2) {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESPECIAL
                }
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals(4, matcher.group().size)
    }

    @Test
    internal fun amountOfTestF() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenLazyAmountOf(2) {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESPECIAL
                }
            }
        }.matcher(instructions)


        assertTrue(matcher.next(0))
        assertEquals(4, matcher.group().size)
        assertEquals(4, matcher.endIndex())
        assertTrue(matcher.next(matcher.endIndex()))
        assertEquals(8, matcher.endIndex())
        assertFalse(matcher.next(matcher.endIndex()))
    }
    @Test
    internal fun amountOfTestG() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKEVIRTUAL
            }
            thenLazyAnyAmountOf {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESPECIAL
                }
            }
            thenCustomCheck {
                it.opcode == Opcodes.INVOKEVIRTUAL
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals(4, matcher.group().size)
    }
    @Test
    internal fun amountOfTestH() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKEVIRTUAL
            }
            thenLazyAmountOf(0..2) {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESPECIAL
                }
            }
            thenCustomCheck {
                it.opcode == Opcodes.INVOKEVIRTUAL
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }
}