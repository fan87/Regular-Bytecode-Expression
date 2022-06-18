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

class StartEndTest {
    
    @Test
    internal fun startEndTestA() {
        val instructions = InsnList().apply {
            add(FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
            add(LdcInsnNode("Hello, World!"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
        }

        val matcher = RegbexPattern {
            thenStartOfInstructions()
            thenGetStaticField(TypeExp(System::class.java), Regex.fromLiteral("out"), TypeExp(PrintStream::class.java))
            thenGroup("printedMessage") {
                thenLdcString()
            }
            thenVirtualMethodCallIgnoreArgs(TypeExp(PrintStream::class.java), Regex.fromLiteral("println"), TypeExp(PrimitiveType.VOID))
            thenEndOfInstructions()
        }.matcher(instructions)

        assertTrue(matcher.next(0))
        assertEquals("Hello, World!", (matcher.group("printedMessage")!![0] as LdcInsnNode).cst)
        assertEquals(matcher.group().size, 3)
    }
    @Test
    internal fun startEndTestB() {
        val instructions = InsnList().apply {
            add(LdcInsnNode("BEGIN"))
            add(FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
            add(LdcInsnNode("END"))
        }

        val matcher = RegbexPattern {
            thenStartOfInstructions()
            thenGetStaticField(TypeExp(System::class.java), Regex.fromLiteral("out"), TypeExp(PrintStream::class.java))
        }.matcher(instructions)

        assertFalse(matcher.next(0))
    }
    @Test
    internal fun startEndTestC() {
        val instructions = InsnList().apply {
            add(LdcInsnNode("BEGIN"))
            add(FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
            add(LdcInsnNode("END"))
        }

        val matcher = RegbexPattern {
            thenGetStaticField(TypeExp(System::class.java), Regex.fromLiteral("out"), TypeExp(PrintStream::class.java))
            thenEndOfInstructions()
        }.matcher(instructions)

        assertFalse(matcher.next(0))
    }


}