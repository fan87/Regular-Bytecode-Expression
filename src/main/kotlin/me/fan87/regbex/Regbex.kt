package me.fan87.regbex

import me.fan87.regbex.utils.InstructionEqualChecker
import me.fan87.regbex.utils.MethodArgumentsTypeReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
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

internal data class LazyAmountOfBegin(var range: IntProgression): RegbexMatchElement()
internal open class LazyAmountOfEnd : RegbexMatchElement()

internal data class GreedyAmountOfBegin(var range: IntProgression): RegbexMatchElement()
internal open class GreedyAmountOfEnd: RegbexMatchElement()


class Regbex {


    internal val elements = ArrayList<RegbexMatchElement>()

    ////////// Basic Functions (Requires Implementation) //////////

    fun thenGroup(name: String, regbex: RegbexBuilder) {
        elements.add(GroupBegin(name))
        elements.addAll(regbex.getRegbex().elements)
        elements.add(GroupEnd())
    }

    fun thenCustomCheck(check: (instruction: AbstractInsnNode) -> Boolean) {
        elements.add(CustomCheck(check))
    }

    fun thenLazyAmountOf(range: IntProgression, regbex: RegbexBuilder) {
        elements.add(LazyAmountOfBegin(range))
        elements.addAll(regbex.getRegbex().elements)
        elements.add(LazyAmountOfEnd())
    }

    fun thenCapturedGroup(name: String) {
        elements.add(CapturedGroup(name))
    }

    fun thenAmountOf(range: IntProgression, regbex: RegbexBuilder) {
        elements.add(GreedyAmountOfBegin(range))
        elements.addAll(regbex.getRegbex().elements)
        elements.add(GreedyAmountOfEnd())
    }

    fun thenCheckWithoutMovingPointer(regbex: RegbexBuilder) {
        elements.add(CheckWithoutMovingPointerBegin())
        elements.addAll(regbex.getRegbex().elements)
        elements.add(CheckWithoutMovingPointerEnd())
    }

    fun thenStartOfInstructions() {
        elements.add(StartOfInstructions())
    }
    fun thenEndOfInstructions() {
        elements.add(EndOfInstructions())
    }

    ////////// Debug Friendly Aliases //////////

    fun thenAny() {
        thenCustomCheck { true }
    }

    ////////// Aliases //////////


    fun thenLazyAmountOf(amount: Int, regbex: RegbexBuilder) {
        thenLazyAmountOf(amount..amount, regbex)
    }

    fun thenAmountOf(amount: Int, regbex: RegbexBuilder) {
        thenAmountOf(amount..amount, regbex)
    }

    // Operators
    fun thenAnyAmountOf(regbex: RegbexBuilder) { // *
        thenAmountOf(0..Int.MAX_VALUE, regbex)
    }
    fun thenAtLeastOneOf(regbex: RegbexBuilder) { // +
        thenAmountOf(1..Int.MAX_VALUE, regbex)
    }
    fun thenOptional(regbex: RegbexBuilder) { // ?
        thenAmountOf(0..1, regbex)
    }
    // Lazy
    fun thenLazyAnyAmountOf(regbex: RegbexBuilder) { // *?
        thenLazyAmountOf(0..Int.MAX_VALUE, regbex)
    }
    fun thenLazyAtLeastOneOf(regbex: RegbexBuilder) { // +?
        thenLazyAmountOf(1..Int.MAX_VALUE, regbex)
    }


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


    fun thenOpcodeCheck(opcode: Int) {
        thenCustomCheck { it.opcode == opcode }
    }

    fun thenTypeCheck(type: Class<out AbstractInsnNode?>) {
        thenCustomCheck { it.javaClass == type }
    }

    fun thenEqual(instruction: AbstractInsnNode) {
        thenCustomCheck { InstructionEqualChecker.checkEquals(instruction, it) }
    }

    fun thenEqual(list: Iterable<AbstractInsnNode>) {
        for (abstractInsnNode in list) {
            thenEqual(abstractInsnNode)
        }
    }

    //<editor-fold desc="Var Node" defaultstate="collapsed">
    fun thenVarNode(varNumber: Int) {
        thenCustomCheck {
            it is VarInsnNode && it.`var` == varNumber
        }
    }
    fun thenVarNode(varNumber: Int, opcode: Int) {
        thenCustomCheck {
            it.opcode == opcode && it is VarInsnNode && it.`var` == varNumber
        }
    }
    fun thenVarStoreNode(varNumber: Int) {
        thenCustomCheck {
            it.opcode in 54..58 && it is VarInsnNode && it.`var` == varNumber
        }
    }
    fun thenVarLoadNode(varNumber: Int) {
        thenCustomCheck {
            it.opcode in 21..25 && it is VarInsnNode && it.`var` == varNumber
        }
    }
    //</editor-fold>
    //<editor-fold desc="Ldc String" defaultstate="collapsed">
    fun thenLdcString() {
        thenCustomCheck { it is LdcInsnNode && it.cst is String }
    }

    fun thenLdcStringEqual(string: String) {
        thenCustomCheck {
            it is LdcInsnNode && it.cst is String && it.cst == string
        }
    }

    fun thenLdcStringMatches(regex: Regex) {
        thenCustomCheck {
            it is LdcInsnNode && it.cst is String && (it.cst as String).matches(regex)
        }
    }
    //</editor-fold>
    //<editor-fold desc="Method Calls" defaultstate="collapsed">
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

    fun thenStaticMethodCall(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp, vararg argsTypes: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.INVOKESTATIC }}, {thenMethodCall(ownerType, methodNamePattern, returnType, *argsTypes)})
    }

    fun thenVirtualMethodCall(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp, vararg argsTypes: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.INVOKEVIRTUAL }}, {thenMethodCall(ownerType, methodNamePattern, returnType, *argsTypes)})
    }
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

    fun thenStaticMethodCallIgnoreArgs(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.INVOKESTATIC }}, {thenMethodCallIgnoreArgs(ownerType, methodNamePattern, returnType)})
    }

    fun thenVirtualMethodCallIgnoreArgs(ownerType: TypeExp, methodNamePattern: Regex, returnType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.INVOKEVIRTUAL }}, {thenMethodCallIgnoreArgs(ownerType, methodNamePattern, returnType)})
    }
    //</editor-fold>
    //<editor-fold desc="Fields" defaultstate="collapsed">
    fun thenField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenCustomCheck {
            it is FieldInsnNode && ownerType.matches(it.owner) && it.name.matches(fieldNamePattern) && fieldType.matches(it.desc)
        }
    }

    fun thenStaticField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETSTATIC || it.opcode == Opcodes.PUTSTATIC }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }

    fun thenVirtualField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETFIELD || it.opcode == Opcodes.PUTFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }

    fun thenGetField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETSTATIC || it.opcode == Opcodes.GETFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }

    fun thenGetStaticField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETSTATIC }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }

    fun thenGetVirtualField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.GETFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }

    fun thenPutField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.PUTSTATIC || it.opcode == Opcodes.PUTFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }

    fun thenPutStaticField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.PUTSTATIC }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }

    fun thenPutVirtualField(ownerType: TypeExp, fieldNamePattern: Regex, fieldType: TypeExp) {
        thenAnd({thenCustomCheck { it.opcode == Opcodes.PUTFIELD }}, {thenField(ownerType, fieldNamePattern, fieldType)})
    }
    //</editor-fold>


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