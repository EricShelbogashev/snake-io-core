package api.v1.dto

open class Food(
    x: Int,
    y: Int
) : Coords(x, y) {
    override fun toString(): String {
        return "Food()"
    }
}