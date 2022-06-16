import me.fan87.regbex.PrimitiveType
import me.fan87.regbex.RegbexPattern
import me.fan87.regbex.TypeExp
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndTest {


    
    @Test
    internal fun andTestA() {
        val instructions = InsnList().apply {
            add(FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
            add(LdcInsnNode("Hello, World!"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenGetStaticField(TypeExp(System::class.java), Regex.fromLiteral("out"), TypeExp(PrintStream::class.java))
            thenGroup("printedMessage") {
                thenLdcString()
            }
            thenVirtualMethodCallIgnoreArgs(TypeExp(PrintStream::class.java), Regex.fromLiteral("println"), TypeExp(PrimitiveType.VOID))
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals("Hello, World!", (matcher.group("printedMessage")!![0] as LdcInsnNode).cst)
    }
    @Test
    internal fun andTestB() {
        val instructions = InsnList().apply {
            add(FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
        }

        val matcher = RegbexPattern {
            thenGetStaticField(TypeExp(System::class.java), Regex.fromLiteral("out"), TypeExp(PrintStream::class.java))
        }.matcher(instructions)

        assertTrue(matcher.next(0))
    }


}