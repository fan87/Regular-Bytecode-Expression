package me.fan87.regbex

/**
 * @param start Inclusive start
 * @param end Inclusive End
 */
class RegbexRegion(var start: Int, var end: Int) {

    fun size(): Int {
        return end - start + 1
    }

}