import me.fan87.regbex.PrimitiveType
import me.fan87.regbex.RegbexPattern
import me.fan87.regbex.TypeExp
import org.junit.jupiter.api.Assertions
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

    /**
     * This is actually from one of my project, and the bytecode is actually a part of Minecraft
     */
    @Test
    @DisplayName("Minecraft - PatternB (Source: SpookySky)")
    internal fun patternB() {
        var instructions = ArrayList<AbstractInsnNode>().apply {
            val l6 = LabelNode()
            add(VarInsnNode(Opcodes.ALOAD, 0))
            add(TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/gui/GuiScreen"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/Minecraft", "displayGuiScreen", "(Lnet/minecraft/client/gui/GuiScreen;)V"))
//            add(l6)
//            add(LineNumberNode(1259, l5))
            add(VarInsnNode(Opcodes.ALOAD, 0))
            add(IntInsnNode(Opcodes.SIPUSH, 10000))
            add(FieldInsnNode(Opcodes.PUTFIELD, "net/minecraft/client/Minecraft", "leftClickCounter", "I"))
        }

        val pattern = RegbexPattern {
            thenThis()
            thenGroup("GuiScreen") {
                thenOpcodeCheck(Opcodes.CHECKCAST)
            }
            thenGroup("displayGuiScreen") {
                thenOpcodeCheck(Opcodes.INVOKEVIRTUAL)
            }
            thenAmountOf(0..4) {
                thenAny()
            }
            thenThis()
            thenPushInt(10000)
            thenGroup("leftClickCounter") {
                thenOpcodeCheck(Opcodes.PUTFIELD)
            }
        }

        val matcher = pattern.matcher(instructions)
        assertTrue(matcher.next(0, false))
        assertEquals("net/minecraft/client/gui/GuiScreen", (matcher.group("GuiScreen")!![0] as TypeInsnNode).desc)
        assertEquals("displayGuiScreen", (matcher.group("displayGuiScreen")!![0] as MethodInsnNode).name)
        assertEquals("leftClickCounter", (matcher.group("leftClickCounter")!![0] as FieldInsnNode).name)
    }

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