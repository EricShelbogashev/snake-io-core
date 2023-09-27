package api.v1.dto

open class Coords(
    val x: Int, val y: Int
) {
    override fun toString(): String {
        return "Coords(x=$x, y=$y)"
    }
}