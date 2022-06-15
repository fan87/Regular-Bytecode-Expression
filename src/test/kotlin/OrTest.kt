import me.fan87.regbex.RegbexPattern
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrTest {


    
    @Test
    internal fun orTestA() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenOr (
                {
                    thenCustomCheck {
                        it.opcode == Opcodes.INVOKESTATIC
                    }
                },
                {
                    thenCustomCheck {
                        it.opcode == Opcodes.INVOKEVIRTUAL
                    }
                }
            )
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }

    @Test
    internal fun orTestB() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenOr({
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESTATIC
                }
            }, {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            })
        }.matcher(instructions)

        assertFalse(matcher.next(0))
    }

    @Test
    internal fun orTestC() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenOr({
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESTATIC
                }
            }, {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            })
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }

    @Test
    internal fun orTestD() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenOr({
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESTATIC
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            }, {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            })
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }

    @Test
    internal fun orTestE() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenOr({
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESTATIC
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            }, {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            })
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESPECIAL
            }
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }


    @Test
    internal fun orTestF() {
        val instructions = InsnList().apply {
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESTATIC
            }
            thenOr({
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESPECIAL
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
            }, {
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKEVIRTUAL
                }
                thenCustomCheck {
                    it.opcode == Opcodes.INVOKESPECIAL
                }
            })
            thenCustomCheck {
                it.opcode == Opcodes.INVOKESPECIAL
            }
        }.matcher(instructions)

        assertFalse(matcher.next(0))
    }


}