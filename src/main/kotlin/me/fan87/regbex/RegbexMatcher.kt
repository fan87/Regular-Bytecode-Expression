package me.fan87.regbex

import me.fan87.regbex.utils.InstructionEqualChecker
import org.objectweb.asm.tree.AbstractInsnNode
import java.util.Stack

/**
 * A matcher with same concept as Java's [Regular Expression Matcher API][java.util.regex.Matcher].
 * You should only be obtaining this object via [RegbexPattern.matcher].
 */
class RegbexMatcher internal constructor(instructions: Iterable<AbstractInsnNode>, private val pattern: RegbexPattern): RegbexResultable {


    private val instructions = ArrayList<AbstractInsnNode>()

    init {
        for (instruction in instructions) {
            this.instructions.add(instruction)
        }
    }



    override fun groupStart(groupName: String): Int? {
        checkMatched()
        return capturedNamed[groupName]?.start
    }

    override fun groupEnd(groupName: String): Int? {
        checkMatched()
        return capturedNamed[groupName]?.end
    }

    override fun replaceGroup(groupName: String, replaceTo: ReplaceTarget): ArrayList<AbstractInsnNode> {
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

    override fun replace(replaceTo: ReplaceTarget): ArrayList<AbstractInsnNode> {
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
                newInstructions.addAll(getRealReplaceContent(replaceTo))
            }
        }
        return newInstructions
    }

    /**
     * Exclusive
     */
    override fun endIndex(): Int {
        checkMatched()
        return matched!!.end
    }

    /**
     * Inclusive
     */
    override fun startIndex(): Int {
        checkMatched()
        return matched!!.start
    }

    override fun matchedSize(): Int {
        checkMatched()
        return matched!!.size()
    }

    override fun group(): List<AbstractInsnNode> {
        checkMatched()
        return matched!!.getRegion()
    }

    override fun group(name: String): List<AbstractInsnNode>? {
        checkMatched()
        val regbexRegion = capturedNamed[name]
        return regbexRegion?.getRegion()
    }

    private fun RegbexRegion.getRegion(): List<AbstractInsnNode> {
        return instructions.subList(this.start, this.end)
    }

    override fun pattern(): RegbexPattern {
        return this.pattern
    }

    private fun checkMatched() {
        if (!matchedAny) {
            throw IllegalStateException("Do RegbexMatcher.next() before getting the result")
        }
    }

    /**
     * This property will be used and be written by next(). If you overwrite it, the next time you call next() won't
     * use the previous matching end index as start matching index
     */
    var currentEndMatchingIndex = 0

    /**
     * Find the next match starts from [currentEndMatchingIndex] (which will be automatically written by this function)
     */
    fun next(): Boolean {
        if (next(currentEndMatchingIndex)) {
            currentEndMatchingIndex = endIndex()
            return true
        }
        return false
    }

    /**
     * Find the next match starts from [startIndex]
     */
    fun next(startIndex: Int): Boolean {
        return next(startIndex, true)
    }

    fun matches(): Boolean {
        val result = next(0, false)
        if (!result) {
            return false
        }
        if (matchedSize() != instructions.size) {
            matchedAny = false
            return false
        }
        return true
    }
    var debug = false
    private var debugMessage = ""

    //<editor-fold desc="Internal" defaultstate="collapsed">
    // State:
    private var elements = pattern.regbex.elements
    private var target: List<AbstractInsnNode> = ArrayList()
    private var instructionIndex = 0
    private var elementIndex = 0
    private var readStack = Stack<ReadStackElement<*>>()
    private var fallbackStack = Stack<Snapshot>()

    private var reverted = false

    // Return Values:
    private var capturedNamed = HashMap<String, RegbexRegion>()
    private var matched: RegbexRegion? = null
    private var matchedAny = false


    fun next(startIndex: Int, moveOnIfFail: Boolean): Boolean {
        elements = pattern.regbex.elements
        target = instructions.subList(startIndex, instructions.size)

        instructionIndex = startIndex
        elementIndex = 0
        readStack.clear()
        fallbackStack.clear()

        capturedNamed.clear()
        matchedAny = false

        reverted = false
        outer@while (true) {
            try {
                run running@{
                    while (elementIndex < elements.size) {
                        val currentElement = elements[elementIndex]
                        if (currentElement is CustomCheck) {
                            val instruction = nextInstruction("CustomCheck requires one more instruction to check")
                            if (instruction == null) {
                                return@running
                            }
                            if (!currentElement.check(instruction)) {
                                failed("CustomCheck has failed")
                                return@running
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
                                return@running
                            }
                        }
                        if (currentElement is EndOfInstructions) {
                            if (instructionIndex < instructions.size) {
                                failed("Expected end of instructions, but the instructions index ($instructionIndex) is less than total instruction size (${instructions.size})")
                                return@running
                            }
                        }
                        if (currentElement is CapturedGroup) {
                            val regbexRegion = capturedNamed[currentElement.name]
                            val region = regbexRegion?.getRegion()
                            if (region == null) {
                                failed("Group is not captured (yet): ${currentElement.name}")
                                return@running
                            }

                            for (abstractInsnNode in region) {
                                val instruction = nextInstruction("CapturedGroup requires one more instruction to check")
                                if (instruction == null) {
                                    return@running
                                }
                                if (!InstructionEqualChecker.checkEquals(abstractInsnNode, instruction)) {
                                    failed("CapturedGroup has failed (Expected: ${abstractInsnNode.javaClass.simpleName} with opcode: ${abstractInsnNode.opcode}, but got ${instruction.javaClass.simpleName} with opcode: ${instruction.opcode})")
                                    return@running
                                }
                            }
                        }
                        if (currentElement is LazyAmountOfBegin) {
                            readStack.push(ReadStackElement(currentElement, elementIndex, instructionIndex))
                            if (0 in currentElement.range) {
                                elementIndex++
                                fallbackStack.push(takeSnapshot())
                                elementIndex = currentElement.endIndex // Go lazy
                            }
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
                            readStack.pop()
                            elementIndex = oldIndex // If it's in range, go to lazy branch
                            if (groupBegin.data["counter"] !in groupBegin.begin.range) { // If it's not in range, go back to greedy
                                failed("Count ${groupBegin.data["counter"]} is not in range: ${groupBegin.begin.range}")
                                return@running
                            }
                        }
                        if (currentElement is GreedyAmountOfBegin) {
                            readStack.push(ReadStackElement(currentElement, elementIndex, instructionIndex))
                            if (0 in currentElement.range) {
                                val oldIndex = elementIndex
                                elementIndex = currentElement.endIndex
                                fallbackStack.push(takeSnapshot())
                                elementIndex = oldIndex
                            }
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
                            readStack.push(groupBegin)
                            elementIndex = groupBegin.elementIndex
                            if (groupBegin.data["counter"] !in groupBegin.begin.range) { // If it's not in range, go back to lazy
                                failed("Count ${groupBegin.data["counter"]} is not in range: ${groupBegin.begin.range}")
                                return@running
                            }
                        }

                        elementIndex++
                    }

                    matched = RegbexRegion(startIndex, instructionIndex)
                    matchedAny = true
                    debugMessage = ""
                    return true
                }
                if (errorMessage != null) {
                    if (debug) {
                        debugMessage = "Matching failed at instruction index: $instructionIndex and regbex element index: $elementIndex, $errorMessage\n"
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
                        System.err.println(debugMessage)
                    }

                    if (fallbackStack.isNotEmpty()) {
                        errorMessage = null
                        useSnapshot(fallbackStack.pop())
                        continue@outer
                    }

                    if (moveOnIfFail) {
                        for (i in startIndex+1 until instructions.size) {
                            if (next(i, false)) {
                                return true
                            }
                        }
                    }
                    matchedAny = false
                    return false
                }
            } catch (e: Exception) {
                if (debug) {
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
        return instructionIndex < instructions.size
    }

    private fun nextInstruction(reason: String): AbstractInsnNode? {
        if (!hasNextInstruction()) {
            failed("Expected Input, but it's the end of input. Reason: $reason")
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

    var errorMessage: String? = null

    private fun failed(reason: String) {
        errorMessage = reason
    }
    //</editor-fold>

}

internal data class ReadStackElement<T: RegbexMatchElement>(val begin: T, val elementIndex: Int, val instructionIndex: Int, val data: HashMap<String, Any> = HashMap())
internal data class Snapshot(val instructionIndex: Int, val elementIndex: Int, val readStack: Stack<ReadStackElement<*>>, val reverted: Boolean)

class RegbexMatchException(message: String): Exception(message)