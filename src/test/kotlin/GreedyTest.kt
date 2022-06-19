import me.fan87.regbex.RegbexPattern
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GreedyTest {

    @Test
    internal fun greedyTestA() {
        val instructions = InsnList().apply {
            add(LdcInsnNode("A"))
            add(LdcInsnNode("B"))
            add(LdcInsnNode("C"))
            add(LdcInsnNode("D"))
            add(InsnNode(0))
            add(InsnNode(0))
            add(InsnNode(0))
            add(LdcInsnNode("A"))
            add(InsnNode(0))
            add(InsnNode(0))
            add(LdcInsnNode("D"))
        }

        val matcher = RegbexPattern {
            thenAmountOf(4) {
                thenLdcString()
            }
            thenGroup("fuck") {
                thenAnyAmountOf {
                    thenAny()
                }
            }
            thenLdcString()
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals(instructions.size(), matcher.group().size)
    }
    @Test
    internal fun greedyTestB() {
        val instructions = InsnList().apply {
            add(LdcInsnNode("A"))
            add(LdcInsnNode("C"))
        }

        val matcher = RegbexPattern {
            thenLdcString()
            thenAnyAmountOf {
                thenPushInt(0)
            }
            thenLdcString()
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }

    @Test
    internal fun greedyTestC() {
        val instructions = InsnList().apply {
            add(LdcInsnNode("A"))
            add(LdcInsnNode("B"))
            add(LdcInsnNode("C"))
        }

        val matcher = RegbexPattern {
            thenLdcString()
            thenOptional {
                thenLdcStringEqual("B")
            }
            thenLdcString()
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }

    @Test
    internal fun greedyTestD() {
        val instructions = InsnList().apply {
            add(LdcInsnNode("A"))
            add(LdcInsnNode("B"))
            add(LdcInsnNode("C"))
        }

        val matcher = RegbexPattern {
            thenLdcString()
            thenOptional {
                thenLdcStringEqual("C")
            }
            thenLdcString()
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }

}