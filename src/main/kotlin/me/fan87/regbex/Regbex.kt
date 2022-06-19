package me.fan87.regbex

import me.fan87.regbex.utils.InstructionEqualChecker
import me.fan87.regbex.utils.MethodArgumentsTypeReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

internal open class RegbexMatchElement {
    override fun toString(): String {
        return javaClass.simpleName
    }
}
internal data class CustomCheck(var check: (instruction: AbstractInsnNode) -> Boolean): RegbexMatchElement()

internal data class GroupBegin(var name: String): RegbexMatchElement()
internal open class GroupEnd: RegbexMatchElement()

internal open class CheckWithoutMovingPointerBegin : RegbexMatchElement()
internal open class CheckWithoutMovingPointerEnd: RegbexMatchElement()

internal open class StartOfInstructions : RegbexMatchElement()
internal open class EndOfInstructions: RegbexMatchElement()

internal data class CapturedGroup(var name: String): RegbexMatchElement()

internal data class LazyAmountOfBegin(var range: IntProgression): RegbexMatchElement() {
    var endIndex: Int = 0
}
internal open class LazyAmountOfEnd : RegbexMatchElement()

internal data class GreedyAmountOfBegin(var range: IntProgression): RegbexMatchElement() {
    var endIndex: Int = 0
}
internal open class GreedyAmountOfEnd: RegbexMatchElement()


class Regbex {


    internal val elements = ArrayList<RegbexMatchElement>()

    ////////// Basic Functions (Requires Implementation) //////////

    /**
     * A named capture group. Everything inside this block will be captured, and will be accessible via [RegbexMatcher.group(String)][RegbexMatcher.group]
     */
    fun thenGroup(name: String, regbex: RegbexBuilder) {
        elements.add(GroupBegin(name))
        elements.addAll(regbex.getRegbex().elements)
        elements.add(GroupEnd())
    }

    /**
     * A custom check to check if an instruction matches or not
     */
    fun thenCustomCheck(check: (instruction: AbstractInsnNode) -> Boolean) {
        elements.add(CustomCheck(check))
    }

    /**
     * Amount of [regbex]. Will match as few as possible (lazy)
     * Equivalent to `{x,y}?` in regular expression
     */
    fun thenLazyAmountOf(range: IntProgression, regbex: RegbexBuilder) {
        val element = LazyAmountOfBegin(range)
        elements.add(element)
        elements.addAll(regbex.getRegbex().elements)
        element.endIndex = elements.size
        elements.add(LazyAmountOfEnd())
    }

    /**
     * Expect an already captured named group
     */
    fun thenCapturedGroup(name: String) {
        elements.add(CapturedGroup(name))
    }

    /**
     * Amount of [regbex]. Will match as much as possible (greedy)
     * Equivalent to `{x,y}` in regular expression
     */
    fun thenAmountOf(range: IntProgression, regbex: RegbexBuilder) {
        val element = GreedyAmountOfBegin(range)
        elements.add(element)
        elements.addAll(regbex.getRegbex().elements)
        element.endIndex = elements.size
        elements.add(GreedyAmountOfEnd())
    }

    /**
     * Check without increasing the index of current match. Could be captured, but since it won't increase the index,
     * the matched group will always be the same. Being used to do [thenAnd]
     */
    fun thenCheckWithoutMovingPointer(regbex: RegbexBuilder) {
        elements.add(CheckWithoutMovingPointerBegin())
        elements.addAll(regbex.getRegbex().elements)
        elements.add(CheckWithoutMovingPointerEnd())
    }

    /**
     * Assert that it's the start of instructions (Index = 0). With [thenEndOfInstructions] at the end, and this at the front,
     * it will check if the entire instructions list matches.
     * @see thenEndOfInstructions
     */
    fun thenStartOfInstructions() {
        elements.add(StartOfInstructions())
    }

    /**
     * Assert that it's the end of instructions (index = size - 1)
     * @see thenStartOfInstructions
     */
    fun thenEndOfInstructions() {
        elements.add(EndOfInstructions())
    }

    ////////// Aliases //////////

    /**
     * Expect any instruction
     */
    fun thenAny() { // .
        thenCustomCheck { true }
    }

    /**
     * Fixed amount of [regbex] (without a range, but only 1 number), will match as few as possible (lazy)
     * Equivalent to `{x}?` in regular expression
     * @see thenLazyAmountOf
     */
    fun thenLazyAmountOf(amount: Int, regbex: RegbexBuilder) { // {x}?
        thenLazyAmountOf(amount..amount, regbex)
    }

    /**
     * Fixed amount of [regbex] (without a range, but only 1 number). Will match as much as possible (greedy)
     * Equivalent to `{x}` in regular expression
     * @see thenAmountOf
     */
    fun thenAmountOf(amount: Int, regbex: RegbexBuilder) { // {x}
        thenAmountOf(amount..amount, regbex)
    }

    // Greedy Operators
    /**
     * Then any amount of matches, will match as much as possible (greedy).
     * Equivalent to `*` in regular expression
     */
    fun thenAnyAmountOf(regbex: RegbexBuilder) { // *
        thenAmountOf(0..Int.MAX_VALUE, regbex)
    }
    /**
     * Then at least one match, will match as much as possible (greedy).
     * Equivalent to `+` in regular expression
     */
    fun thenAtLeastOneOf(regbex: RegbexBuilder) { // +
        thenAmountOf(1..Int.MAX_VALUE, regbex)
    }
    /**
     * Then optional (0-1), will match if possible (greedy)
     * Equivalent to `?` in regular expression
     */
    fun thenOptional(regbex: RegbexBuilder) { // ?
        thenAmountOf(0..1, regbex)
    }

    // Lazy
    /**
     * Then any amount of matches, will match as few as possible (lazy)
     * Equivalent to `*?` in regular expression
     */
    fun thenLazyAnyAmountOf(regbex: RegbexBuilder) { // *?
        thenLazyAmountOf(0..Int.MAX_VALUE, regbex)
    }
    /**
     * Then at least one match, will match as few as possible (lazy)
     * Equivalent to `+?` in regular expression
     */
    fun thenLazyAtLeastOneOf(regbex: RegbexBuilder) { // +?
        thenLazyAmountOf(1..Int.MAX_VALUE, regbex)
    }

    /**
     * And check the same thing. The index pointer will be moved to where the last condition is ended
     */
    fun thenAnd(vararg regbexs: RegbexBuilder) {
        val toList = regbexs.toList()
        for (regbex in toList.dropLast(1)) {
            thenCheckWithoutMovingPointer(regbex)
        }
        for (regbex in toList.drop(toList.size - 1)) {
            elements.addAll(regbex.getRegbex().elements)
        }
    }

    ////////// Advanced Matching //////////

    /**
     * Expect an instruction with [opcode]
     */
    fun thenOpcodeCheck(opcode: Int) {
        thenCustomCheck { it.opcode == opcode }
    }

    /**
     * Expect an instruction with type [type] (Equal, not assignable from)
     */
    fun thenTypeCheckEqual(type: Class<out AbstractInsnNode?>) {
        thenCustomCheck { it.javaClass == type }
    }

    /**
     * Expect an instruction with type [T] (Equal, not assignable from)
     */
    inline fun <reified T> thenTypeCheckEqual() {
        thenCustomCheck { it.javaClass == T::class.java }
    }

    /**
     * Expect an instruction with type [type] (Assignable From)
     */
    fun thenTypeCheckAssignableFrom(type: Class<out AbstractInsnNode?>) {
        thenCustomCheck { type.isAssignableFrom(it.javaClass) }
    }

    /**
     * Expect an instruction with type [T] (Assignable From)
     */
    inline fun <reified T> thenTypeCheckAssignableFrom() {
        thenCustomCheck { it is T }
    }

    /**
     * Expect an exact same instruction
     */
    fun thenEqual(instruction: AbstractInsnNode) {
        thenCustomCheck { InstructionEqualChecker.checkEquals(instruction, it) }
    }

    /**
     * Expect exact same list of instructions
     */
    fun thenEqual(list: Iterable<AbstractInsnNode>) {
        for (abstractInsnNode in list) {
            thenEqual(abstractInsnNode)
        }
    }

    //<editor-fold desc="Var Node" defaultstate="collapsed">
    /**
     * Expect a [VarInsnNode] with var number
     */
    fun thenVarNode(varNumber: Int) {
        thenCustomCheck {
            it is VarInsnNode && it.`var` == varNumber
        }
    }

    /**
     * Expect a [VarInsnNode] with var number and opcode
     */
    fun thenVarNode(varNumber: Int, opcode: Int) {
        thenCustomCheck {
            it.opcode == opcode && it is VarInsnNode && it.`var` == varNumber
        }
    }

    /**
     * Expect a storing [VarInsnNode] with var number
     */
    fun thenVarStoreNode(varNumber: Int) {
        thenCustomCheck {
            it.opcode in 54..58 && it is VarInsnNode && it.`var` == varNumber
        }
    }

    /**
     * Expect a loading [VarInsnNode] with var number
     */
    fun thenVarLoadNode(varNumber: Int) {
        thenCustomCheck {
            it.opcode in 21..25 && it is VarInsnNode && it.`var` == varNumber
        }
    }
    //</editor-fold>
    //<editor-fold desc="Ldc String" defaultstate="collapsed">
    /**
     * Expect a [LdcInsnNode] with string as [LdcInsnNode.cst]'s type
     */
    fun thenLdcString() {
        thenCustomCheck { it is LdcInsnNode && it.cst is String }
    }

    /**
     * Expect a [LdcInsnNode] with string
     */
    fun thenLdcStringEqual(string: String) {
        thenCustomCheck {
            it is LdcInsnNode && it.cst is String && it.cst == string
        }
    }

    /**
     * Expect a [LdcInsnNode] with string that matches [regex]
     */
    fun thenLdcStringMatches(regex: Regex) {
        thenCustomCheck {
            it is LdcInsnNode && it.cst is String && (it.cst as String).matches(regex)
        }
    }
    //</editor-fold>
    //<editor-fold desc="Method Calls" defaultstate="collapsed">
    /**
     * Then expect a static method call of specified method
     */
    fun thenMethodCall(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp, vararg argsTypes: TypeExp) {
        thenCustomCheck {
            if (it is MethodInsnNode) {
                val reader = MethodArgumentsTypeReader(it.desc)
                ownerType.matches(it.owner) &&
                it.name.matches(methodNamePattern) &&
                returnType.matches(it.desc.split(")")[1]) &&
                argsTypes.size == reader.arguments.size && argsTypes.withIndex().all { argType ->
                    argType.value.matches(reader.arguments[argType.index])
                }
            } else {
                false
            }
        }
    }

    /**
     * Then expect a static method call of specified method
     */
    fun thenStaticMethodCall(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp, vararg argsTypes: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.INVOKESTATIC }}, {thenMethodCall(ownerType, methodNamePattern, returnType, *argsTypes)})
    }

    /**
     * Then expect a virtual (non-static) method call of specified method
     */
    fun thenVirtualMethodCall(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp, vararg argsTypes: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.INVOKEVIRTUAL }}, {thenMethodCall(ownerType, methodNamePattern, returnType, *argsTypes)})
    }
    /**
     * Then expect a static method call of specified method without checking its argument types
     */
    fun thenMethodCallIgnoreArgs(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp) {
        thenCustomCheck {
            if (it is MethodInsnNode) {
                ownerType.matches(it.owner) &&
                it.name.matches(methodNamePattern) &&
                returnType.matches(it.desc.split(")")[1])
            } else {
                false
            }
        }
    }
    /**
     * Then expect a static method call of specified method without checking its argument types
     */
    fun thenStaticMethodCallIgnoreArgs(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.INVOKESTATIC }}, {thenMethodCallIgnoreArgs(ownerType, methodNamePattern, returnType)})
    }
    /**
     * Then expect a virtual (non-static) method call of specified method without checking its argument types
     */
    fun thenVirtualMethodCallIgnoreArgs(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.INVOKEVIRTUAL }}, {thenMethodCallIgnoreArgs(ownerType, methodNamePattern, returnType)})
    }
    //</editor-fold>
    //<editor-fold desc="Fields" defaultstate="collapsed">
    /**
     * Then expect a [FieldInsnNode] with specified field info
     */
    fun thenField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenCustomCheck {
            it is FieldInsnNode && ownerType.matches(it.owner) && it.name.matches(fieldNamePattern) && fieldType.matches(it.desc)
        }
    }
    /**
     * Then expect a static [FieldInsnNode] with specified field info
     */
    fun thenStaticField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETSTATIC || it.opcode == Opcodes.PUTSTATIC }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }
    /**
     * Then expect a virtual (non-static) [FieldInsnNode] with specified field info
     */
    fun thenVirtualField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETFIELD || it.opcode == Opcodes.PUTFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }
    /**
     * Then expect a get [FieldInsnNode] with specified field info
     */
    fun thenGetField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETSTATIC || it.opcode == Opcodes.GETFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }
    /**
     * Then expect a get static [FieldInsnNode] with specified field info
     */
    fun thenGetStaticField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETSTATIC }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }
    /**
     * Then expect a get virtual (non-static) [FieldInsnNode] with specified field info
     */
    fun thenGetVirtualField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }
    /**
     * Then expect a put [FieldInsnNode] with specified field info
     */
    fun thenPutField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.PUTSTATIC || it.opcode == Opcodes.PUTFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }
    /**
     * Then expect a put static [FieldInsnNode] with specified field info
     */
    fun thenPutStaticField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.PUTSTATIC }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }
    /**
     * Then expect a put virtual (non-static) [FieldInsnNode] with specified field info
     */
    fun thenPutVirtualField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.PUTFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }
    //</editor-fold>

    /**
     * Expect a return ([Opcodes.IRETURN], [Opcodes.LRETURN], [Opcodes.FRETURN], [Opcodes.DRETURN], [Opcodes.ARETURN], or [Opcodes.RETURN])(Opcode: 172 ~ 177, inclusive)
     */
    fun thenReturn() {
        thenCustomCheck { it.opcode in 172..177 }
    }

    /**
     * Expect a ALOAD 0
     */
    fun thenThis() {
        thenCustomCheck { it.opcode == Opcodes.ALOAD && (it as VarInsnNode).`var` == 0 }
    }

    /**
     * Expect a push int with specified number. Note that every instruction that will push to the number to the stack
     * will also work even if they are illegal, for example, LdcInsnNode(0)
     */
    fun thenPushInt(number: Int) {
        thenCustomCheck {
            if (it.opcode in Opcodes.ICONST_M1..Opcodes.ICONST_5) {
                return@thenCustomCheck (it.opcode - Opcodes.ICONST_0) == number
            }
            if (it is IntInsnNode) {
                return@thenCustomCheck it.operand == number
            }
            if (it is LdcInsnNode) {
                return@thenCustomCheck it.cst == number
            }
            return@thenCustomCheck false
        }
    }

    /**
     * Expect a Ldc node with value [any]
     */
    fun thenLdc(any: Any) {
        thenCustomCheck { it is LdcInsnNode && it == any }
    }


}

typealias RegbexBuilder = Regbex.() -> Unit

fun RegbexBuilder.getRegbex(): Regbex {
    return Regbex().also { it.this() }
}

class TypeExp {
    private val matchCheck: (jvmClassName: String) -> Boolean

    fun matches(input: String?): Boolean {
        if (input == null) {
            return false
        } else {
            return matchCheck(input)
        }
    }

    /**
     * Primitive Type
     */
    constructor(primitiveType: PrimitiveType) {
        matchCheck = { it == primitiveType.jvmName }
    }

    /**
     * Array of Type
     */
    constructor(arrayType: TypeExp) {
        matchCheck = { it.startsWith("[") && arrayType.matches(it.substring(1)) }
    }

    /**
     * Class Name (Equals)
     */
    constructor(name: String) {
        val newName = name.replace(".", "/")
        matchCheck = { it == newName || it == "L$newName;" }
    }

    /**
     * Class Name (Must expect an optional `L` in the front, and `;` in the end, so if you want to match `java.lang`,
     * you have to do `L?java/lang/.*;?`)
     */
    constructor(pattern: Regex) {
        matchCheck = { it.matches(pattern) }
    }

    /**
     * Custom Match Function. The input could be primitive type (`I`, `J`, `V`), regular type (`java/lang/String`), or
     * jvm type (`Ljava/lang/String;`), please expect those types as possible input
     */
    constructor(matchFunction: (jvmClassName: String) -> Boolean) {
        this.matchCheck = matchFunction
    }

    constructor(type: Class<*>): this(type.name)

    constructor(vararg or: TypeExp) {
        this.matchCheck = {
            or.any { exp -> exp.matches(it) }
        }
    }
}

enum class PrimitiveType(val sourceName: String, val jvmName: String, val objectType: Class<*>, val primitiveType: Class<*>) {
    BOOLEAN("boolean", "Z", java.lang.Boolean::class.java, java.lang.Boolean.TYPE),
    BYTE("byte", "B", java.lang.Byte::class.java, java.lang.Byte.TYPE),
    SHORT("short", "S", java.lang.Short::class.java, java.lang.Short.TYPE),
    INT("int", "I", java.lang.Integer::class.java, Integer.TYPE),
    CHAR("char", "C", java.lang.Character::class.java, Character.TYPE),
    FLOAT("float", "F", java.lang.Float::class.java, java.lang.Float.TYPE),
    LONG("long", "J", java.lang.Long::class.java, java.lang.Long.TYPE),
    DOUBLE("double", "D", java.lang.Double::class.java, java.lang.Double.TYPE),
    VOID("void", "V", java.lang.Void::class.java, Void.TYPE),
}