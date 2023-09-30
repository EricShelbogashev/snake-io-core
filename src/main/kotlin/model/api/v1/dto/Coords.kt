package model.api.v1.dto

open class Coords(
    open val x: Int, open val y: Int
) {
    override fun toString(): String {
        return "Coords(x=$x, y=$y)"
    }
}