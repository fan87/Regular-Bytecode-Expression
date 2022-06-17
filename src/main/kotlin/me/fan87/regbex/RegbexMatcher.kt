package me.fan87.regbex

import me.fan87.regbex.utils.InstructionEqualChecker
import org.objectweb.asm.tree.AbstractInsnNode
import java.util.Stack

class RegbexMatcher internal constructor(instructions: Iterable<AbstractInsnNode>, private val pattern: RegbexPattern) {

    val instructions = ArrayList<AbstractInsnNode>()

    init {
        for (instruction in instructions) {
            this.instructions.add(instruction)
        }
    }



    /**
     * Get the inclusive start of a group
     */
    fun groupStart(groupName: String): Int? {
        checkMatched()
        return capturedNamed[groupName]?.start
    }

    /**
     * Get the exclusive end of a group
     */
    fun groupEnd(groupName: String): Int? {
        checkMatched()
        return capturedNamed[groupName]?.end
    }

    fun replaceGroup(groupName: String, replaceTo: Iterable<AbstractInsnNode>): ArrayList<AbstractInsnNode> {
        checkMatched()
        if (group(groupName) == null) {
            return ArrayList(instructions)
        }
        val newInstructions = ArrayList<AbstractInsnNode>()

        for (instruction in instructions.withIndex()) {
            val index = instruction.index
            val insn = instruction.value

            if (index < groupStart(groupName)!! || index >= groupEnd(groupName)!!) {
                newInstructions.add(insn)
                continue
            }
            if (index == groupStart(groupName)!!) {
                newInstructions.addAll(replaceTo)
            }
        }
        return newInstructions
    }

    fun replace(replaceTo: Iterable<AbstractInsnNode>): ArrayList<AbstractInsnNode> {
        checkMatched()
        val newInstructions = ArrayList<AbstractInsnNode>()

        for (instruction in instructions.withIndex()) {
            val index = instruction.index
            val insn = instruction.value

            if (index < startIndex() || index >= endIndex()) {
                newInstructions.add(insn)
                continue
            }
            if (index == startIndex()) {
                newInstructions.addAll(replaceTo)
            }
        }
        return newInstructions
    }

    /**
     * Exclusive
     */
    fun endIndex(): Int {
        checkMatched()
        return matched!!.end
    }

    /**
     * Inclusive
     */
    fun startIndex(): Int {
        checkMatched()
        return matched!!.start
    }

    fun group(): List<AbstractInsnNode> {
        checkMatched()
        return matched!!.getRegion()
    }

    fun group(name: String): List<AbstractInsnNode>? {
        checkMatched()
        val regbexRegion = capturedNamed[name]
        return regbexRegion?.getRegion()
    }

    private fun RegbexRegion.getRegion(): List<AbstractInsnNode> {
        return instructions.subList(this.start, this.end)
    }

    fun pattern(): RegbexPattern {
        return this.pattern
    }

    private fun checkMatched() {
        if (!matchedAny) {
            throw IllegalStateException("Do RegbexMatcher.next() before getting the result")
        }
    }


    fun next(startIndex: Int): Boolean {
        return next0(startIndex, true)
    }

    fun nextOnlyOne(startIndex: Int): Boolean {
        return next0(startIndex, false)
    }

    var debug = true
    var debugMessage = ""

    // State:
    private var elements = pattern.regbex.elements
    private var target: List<AbstractInsnNode> = ArrayList()
    private var instructionIndex = 0
    private var elementIndex = 0
    private var readStack = Stack<ReadStackElement<*>>()
    private var fallbackStack = Stack<Snapshot>()
    private var steps = ArrayList<String>()

    private var reverted = false

    // Return Values:
    private var capturedNamed = HashMap<String, RegbexRegion>()
    private var matched: RegbexRegion? = null
    private var matchedAny = false


    private fun next0(startIndex: Int, nested: Boolean): Boolean {
        elements = pattern.regbex.elements
        target = instructions.subList(startIndex, instructions.size)

        instructionIndex = startIndex
        elementIndex = 0
        readStack.clear()
        fallbackStack.clear()
        steps.clear()

        capturedNamed.clear()
        matchedAny = false

        reverted = false
        outer@while (true) {
            try {
                while (elementIndex < elements.size) {
                    val currentElement = elements[elementIndex]
                    steps.add("Checking $currentElement")
                    if (currentElement is CustomCheck) {
                        val instruction =
                            nextInstruction("CustomCheck requires one instruction to check if it matches")
                        if (!currentElement.check(instruction)) {
                            failed("CustomCheck has failed")
                        }
                    }
                    if (currentElement is GroupBegin) {
                        readStack.push(ReadStackElement(currentElement, elementIndex, instructionIndex))
                    }
                    if (currentElement is GroupEnd) {
                        val groupBegin = popReadStack<GroupBegin>()
                        capturedNamed[groupBegin.begin.name] = RegbexRegion(groupBegin.instructionIndex, instructionIndex)
                    }
                    if (currentElement is CheckWithoutMovingPointerBegin) {
                        readStack.push(ReadStackElement(currentElement, elementIndex, instructionIndex))
                    }
                    if (currentElement is CheckWithoutMovingPointerEnd) {
                        val groupBegin = popReadStack<CheckWithoutMovingPointerBegin>()
                        instructionIndex = groupBegin.instructionIndex
                    }
                    if (currentElement is CheckWithoutMovingPointerBegin) {
                        readStack.push(ReadStackElement(currentElement, elementIndex, instructionIndex))
                    }
                    if (currentElement is CheckWithoutMovingPointerEnd) {
                        val groupBegin = popReadStack<CheckWithoutMovingPointerBegin>()
                        instructionIndex = groupBegin.instructionIndex
                    }
                    if (currentElement is StartOfInstructions) {
                        if (instructionIndex != 0) {
                            failed("Expected start of instructions, but the instruction index is not 0")
                        }
                    }
                    if (currentElement is EndOfInstructions) {
                        if (instructionIndex < instructions.size) {
                            failed("Expected end of instructions, but the instructions index ($instructionIndex) is less than total instruction size (${instructions.size})")
                        }
                    }
                    if (currentElement is CapturedGroup) {
                        val regbexRegion = capturedNamed[currentElement.name]
                        val region = regbexRegion?.getRegion()
                        if (region == null) {
                            failed("Group is not captured (yet): ${currentElement.name}")
                        }

                        for (abstractInsnNode in region!!) {
                            val instruction =
                                nextInstruction("CapturedGroup requires one more instruction to check")
                            if (!InstructionEqualChecker.checkEquals(abstractInsnNode, instruction)) {
                                failed("CapturedGroup has failed (Expected: ${abstractInsnNode.javaClass.simpleName} with opcode: ${abstractInsnNode.opcode}, but got ${instruction.javaClass.simpleName} with opcode: ${instruction.opcode})")
                            }
                        }
                    }
                    if (currentElement is LazyAmountOfBegin) {
                        readStack.push(ReadStackElement(currentElement, elementIndex, instructionIndex))
                    }
                    if (currentElement is LazyAmountOfEnd) {
                        val groupBegin = popReadStack<LazyAmountOfBegin>()
                        val oldIndex = elementIndex
                        elementIndex = groupBegin.elementIndex + 1
                        if (groupBegin.data["counter"] !is Int) {
                            groupBegin.data["counter"] = 1
                        } else {
                            groupBegin.data["counter"] = groupBegin.data["counter"] as Int + 1
                        }
                        readStack.push(groupBegin)
                        fallbackStack.push(takeSnapshot())  // Repeat (greedy) Branch
                        steps.add("  LazyAmountOf: Added Greedy Fallback Option")
                        readStack.pop()
                        elementIndex = oldIndex // If it's in range, go to lazy branch
                        if (groupBegin.data["counter"] !in groupBegin.begin.range) { // If it's not in range, go back to greedy
                            steps.add("  LazyAmountOf: Jumped to Greedy")
                            failed("Count ${groupBegin.data["counter"]} is not in range: ${groupBegin.begin.range}")
                        }
                    }
                    if (currentElement is GreedyAmountOfBegin) {
                        readStack.push(ReadStackElement(currentElement, elementIndex, instructionIndex))
                    }
                    if (currentElement is GreedyAmountOfEnd) {
                        val groupBegin = popReadStack<GreedyAmountOfBegin>()
                        if (groupBegin.data["counter"] !is Int) {
                            groupBegin.data["counter"] = 1
                        } else {
                            groupBegin.data["counter"] = groupBegin.data["counter"] as Int + 1
                        }
                        elementIndex++
                        fallbackStack.push(takeSnapshot())  // Non-repeating/continue (lazy) Branch
                        // Repeating (Greedy) Branch
                        steps.add("  GreedyAmountOf: Added Lazy Fallback Option")
                        readStack.push(groupBegin)
                        elementIndex = groupBegin.elementIndex
                        if (groupBegin.data["counter"] !in groupBegin.begin.range) { // If it's not in range, go back to lazy
                            steps.add("  GreedyAmountOf: Jumped to Lazy")
                            failed("Count ${groupBegin.data["counter"]} is not in range: ${groupBegin.begin.range}")
                        }
                    }

                    elementIndex++
                }

                matched = RegbexRegion(startIndex, instructionIndex)
                matchedAny = true
                debugMessage = ""
                return true
            } catch (e: RegbexMatchException) {
                debugMessage = "Matching failed at instruction index: $instructionIndex and regbex element index: $elementIndex, ${e.message}\n"
                debugMessage += "==== Elements ====\n"
                for (withIndex in elements.withIndex()) {
                    if (elementIndex == withIndex.index) {
                        debugMessage += ">>> "
                    }
                    debugMessage += "${withIndex.index}.  ${withIndex.value}\n"
                }
                debugMessage += "==== Instructions ====\n"
                for (withIndex in instructions.withIndex()) {
                    if (instructionIndex == withIndex.index) {
                        debugMessage += ">>> "
                    }
                    debugMessage += "${withIndex.index}.  ${withIndex.value}\n"
                }
                debugMessage += "==== Read Stack ====\n"
                for (withIndex in readStack.withIndex()) {
                    debugMessage += "${withIndex.index}.  ${withIndex.value}\n"
                }
                debugMessage += "==== Steps ====\n"
                for (withIndex in steps.withIndex()) {
                    debugMessage += "${withIndex.index}.  ${withIndex.value}\n"
                }

                if (fallbackStack.isNotEmpty()) {
                    useSnapshot(fallbackStack.pop())
                    continue@outer
                }
                if (debug) {
                    System.err.println(debugMessage)
                }
                if (nested) {
                    for (i in startIndex+1 until instructions.size) {
                        if (next0(i, false)) {
                            return true
                        }
                    }
                }
                matchedAny = false
                return false
            } catch (e: Exception) {
                debugMessage = "Matching failed at instruction index: $instructionIndex and regbex element index: $elementIndex due to exception\n"
                debugMessage += "==== Elements ====\n"
                for (withIndex in elements.withIndex()) {
                    if (elementIndex == withIndex.index) {
                        debugMessage += ">>> "
                    }
                    debugMessage += "${withIndex.index}.  ${withIndex.value}\n"
                }
                debugMessage += "==== Instructions ====\n"
                for (withIndex in instructions.withIndex()) {
                    if (instructionIndex == withIndex.index) {
                        debugMessage += ">>> "
                    }
                    debugMessage += "${withIndex.index}.  ${withIndex.value}\n"
                }
                debugMessage += "==== Read Stack ====\n"
                for (withIndex in readStack.withIndex()) {
                    debugMessage += "${withIndex.index}.  ${withIndex.value}\n"
                }
                debugMessage += "==== Steps ====\n"
                for (withIndex in steps.withIndex()) {
                    debugMessage += "${withIndex.index}.  ${withIndex.value}\n"
                }
                if (debug) {
                    System.err.println(debugMessage)
                }
                throw e
            }
        }
    }

    private inline fun <reified T : RegbexMatchElement> popReadStack(): ReadStackElement<T> {
        val pop = readStack.pop()
        if (pop.begin !is T) {
            throw IllegalStateException("Illegal Stack: Expected a ${T::class.java.simpleName}, but got ${pop.begin.javaClass.simpleName}")
        }
        return pop as ReadStackElement<T>
    }

    private fun hasNextInstruction(): Boolean {
        return instructionIndex >= instructions.size
    }

    private fun nextInstruction(reason: String): AbstractInsnNode {
        if (hasNextInstruction()) {
            failed("Expected Input, but it's the end of input. Reason: $reason")
        }
        instructionIndex++
        return instructions[instructionIndex - 1];
    }

    private fun nextOptionalInstruction(reason: String): AbstractInsnNode? {
        if (hasNextInstruction()) {
            return null
        }
        instructionIndex++
        return instructions[instructionIndex - 1];
    }

    private fun useSnapshot(snapshot: Snapshot) {
        this.instructionIndex = snapshot.instructionIndex
        this.elementIndex = snapshot.elementIndex
        this.readStack = Stack<ReadStackElement<*>>().apply { addAll(snapshot.readStack) }
        this.reverted = snapshot.reverted
    }
    private fun takeSnapshot(): Snapshot {
        return Snapshot(instructionIndex, elementIndex, Stack<ReadStackElement<*>>().apply { addAll(readStack.map { it.copy(data = HashMap(it.data)) }) }, reverted)
    }

    private fun failed(reason: String): Nothing {
        throw RegbexMatchException(reason)
    }

}

internal data class ReadStackElement<T: RegbexMatchElement>(val begin: T, val elementIndex: Int, val instructionIndex: Int, val data: HashMap<String, Any> = HashMap())
internal data class Snapshot(val instructionIndex: Int, val elementIndex: Int, val readStack: Stack<ReadStackElement<*>>, val reverted: Boolean)

class RegbexMatchException(message: String): Exception(message)