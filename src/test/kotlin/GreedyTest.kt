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

}