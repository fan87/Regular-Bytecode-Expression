import me.fan87.regbex.RegbexPattern
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LdcInsnNode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CapturedGroupTest {

    @Test
    internal fun capturedGroupTestA() {
        val instructions = InsnList().apply {
            add(LdcInsnNode("A"))
            add(LdcInsnNode("B"))
            add(LdcInsnNode("C"))
            add(LdcInsnNode("D"))
            add(LdcInsnNode("MID"))
            add(LdcInsnNode("MID 2"))
            add(LdcInsnNode("A"))
            add(LdcInsnNode("B"))
            add(LdcInsnNode("C"))
            add(LdcInsnNode("D"))
        }

        val matcher = RegbexPattern {
            thenGroup("1") {
                thenLazyAmountOf(4) {
                    thenLdcString()
                }
            }
            thenAny()
            thenAny()
            thenCapturedGroup("1")
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals(4, matcher.group("1")!!.size)
    }
    @Test
    internal fun capturedGroupTestB() {
        val instructions = InsnList().apply {
            add(LdcInsnNode("A"))
            add(LdcInsnNode("B"))
            add(LdcInsnNode("C"))
            add(LdcInsnNode("D"))
            add(LdcInsnNode("MID"))
            add(LdcInsnNode("MID 2"))
            add(LdcInsnNode("A"))
            add(LdcInsnNode("B"))
            add(LdcInsnNode("C"))
            add(LdcInsnNode("E"))
        }

        val matcher = RegbexPattern {
            thenGroup("1") {
                thenLazyAmountOf(4) {
                    thenLdcString()
                }
            }
            thenAny()
            thenAny()
            thenCapturedGroup("1")
        }.matcher(instructions)

        assertFalse(matcher.next(0))
    }

}