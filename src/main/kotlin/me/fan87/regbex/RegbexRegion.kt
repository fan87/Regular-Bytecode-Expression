package me.fan87.regbex

/**
 * @param start Inclusive start
 * @param end Exclusive End
 */
class RegbexRegion(var start: Int, var end: Int) {

    fun size(): Int {
        return end - start
    }

}