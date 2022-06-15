package me.fan87.regbex.utils

import me.fan87.regbex.PrimitiveType

class MethodArgumentsTypeReader(val input: String) {

    val arguments = ArrayList<String>()

    var index = 0

    init {
        val nextChar = nextChar()
        if (nextChar != '(') {
            throw IllegalArgumentException("Expected (, but got $nextChar")
        }

        var read = next()
        while (read != null) {
            arguments.add(read)
            read = next()
        }
    }

    private fun next(): String? {
        val nextChar = nextChar()
        if (nextChar == ')') {
            return null
        }
        for (value in PrimitiveType.values()) {
            if ("$nextChar" == value.jvmName) {
                return "$nextChar"
            }
        }
        if (nextChar == 'L') {
            return "L${nextClassName()};"
        }
        if (nextChar == '[') {
            return "[${next()}"
        }
        throw IllegalArgumentException("Expected I, B, S, I, C, F, J, D, V, L, or [")
    }

    private fun nextClassName(): String {
        var nextChar = nextChar()
        var outputBuffer = ""
        while (nextChar != ';') {
            if (nextChar == ')') {
                throw IllegalArgumentException("Unexpected end of argument type (Expected one \";\" before \")\")")
            }
            outputBuffer += nextChar
            nextChar = nextChar()
        }
        return outputBuffer
    }

    private fun nextChar(): Char {
        index++;
        return input[index - 1]
    }

}