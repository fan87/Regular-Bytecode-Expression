import me.fan87.regbex.PrimitiveType
import me.fan87.regbex.RegbexPattern
import me.fan87.regbex.TypeExp
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ## Code Examples
 * You can read code in this class to understand how it works, how you're going to use it. Tests here are actually
 * things that you may need!
 */
class Examples {

    @Test
    @DisplayName("Extract System.out text and replace")
    internal fun extractSoutAndReplace() {
        var instructions = ArrayList<AbstractInsnNode>().apply {
            add(LdcInsnNode("Some Random Stuff"))
            add(LdcInsnNode("BlahBlahBlah"))
            add(InsnNode(Opcodes.POP))
            add(InsnNode(Opcodes.POP))
            add(FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
            add(LdcInsnNode("Hello, World!"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))
            add(LdcInsnNode("Some Random Stuff"))
            add(LdcInsnNode("BlahBlahBlah"))
            add(InsnNode(Opcodes.POP))
            add(InsnNode(Opcodes.POP))

        }

        val pattern = RegbexPattern {
            // Expect a System.out get field
            thenGetStaticField(TypeExp(System::class.java), Regex.fromLiteral("out"), TypeExp(PrintStream::class.java))

            // Then capture things in this group (Equivalent to `(?<printedMessage>)`
            thenGroup("printedMessage") {
                // Expect a Ldc String that matches `.*, World!`
                thenLdcStringMatches(Regex(".*, World!"))
            }

            // Expect a println
            thenVirtualMethodCallIgnoreArgs(
                TypeExp(PrintStream::class.java), Regex.fromLiteral("println"), TypeExp(
                    PrimitiveType.VOID
                )
            )
        }
        var matcher = pattern.matcher(instructions)

        // If it can find any (which it should be able to)
        assertTrue(matcher.next(0))
        // Assert that it has captured `Hello, World!`
        assertEquals("Hello, World!", (matcher.group("printedMessage")!![0] as LdcInsnNode).cst)
        // Assert that matched group (Equivalent to `$&` or `$0`)
        assertEquals(matcher.group().size, 3)


        // Replace it with Ldc: `Goodbye, World!`
        instructions = matcher.replaceGroup("printedMessage", ArrayList<AbstractInsnNode>().apply {
            add(LdcInsnNode("Goodbye, World!"))
        })
        // Restart the matcher instance with the new instructions
        matcher = pattern.matcher(instructions)
        // Assert that it could find one
        assertTrue(matcher.next(0))
        // Assert that it has captured the replaced ldc: `Goodbye, World!`
        assertEquals("Goodbye, World!", (matcher.group("printedMessage")!![0] as LdcInsnNode).cst)
        // You know the rest
        assertEquals(matcher.group().size, 3)

        // Now this one should fail
        matcher = pattern.matcher(matcher.replaceGroup("printedMessage", ArrayList<AbstractInsnNode>().apply {
            add(LdcInsnNode("Message that doesn't match")) // This message won't match `.*, World!`, so it shouldn't find anything
        }))
        // And as long as it shouldn't match, let's assert false
        assertFalse(matcher.next(0))
    }

}