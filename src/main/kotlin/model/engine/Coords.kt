package model.engine

import model.api.v1.dto.Direction
import org.jetbrains.annotations.Contract

data class Coords(
    val field: Field,
    override val x: Int,
    override val y: Int
) : model.api.v1.dto.Coords(x, y) {
    @Contract("steps must be positive")
    fun left(steps: Int = 1): Coords {
        val newX = (x - steps + field.config.width) % field.config.width
        return Coords(field, newX, y)
    }

    @Contract("steps must be positive")
    fun right(steps: Int = 1): Coords {
        val newX = (x + steps) % field.config.width
        return Coords(field, newX, y)
    }

    @Contract("steps must be positive")
    fun up(steps: Int = 1): Coords {
        val newY = (y - steps + field.config.height) % field.config.height
        return Coords(field, x, newY)
    }

    @Contract("steps must be positive")
    fun down(steps: Int = 1): Coords {
        val newY = (y + steps) % field.config.height
        return Coords(field, x, newY)
    }

    fun to(direction: Direction): Coords {
        return when (direction) {
            Direction.UP -> up()
            Direction.RIGHT -> right()
            Direction.DOWN -> down()
            Direction.LEFT -> left()
        }
    }

    fun nearest(): Array<Coords> {
        return arrayOf(up(), right(), down(), left())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Coords) return false

        return x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        return 31 * x + y
    }

    override fun toString(): String {
        return "Coords(x=$x, y=$y)"
    }
}
