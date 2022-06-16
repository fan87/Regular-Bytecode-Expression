package me.fan87.regbex

class RegbexRegion(var start: Int, var end: Int) {

    fun size(): Int {
        return end - start
    }

}