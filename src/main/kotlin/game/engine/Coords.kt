package game.engine

import api.v1.dto.Direction
import org.jetbrains.annotations.Contract

data class Coords(
    val field: Field, override val x: Int, override val y: Int
): api.v1.dto.Coords(x, y) {

    /**
     * @return  точку, смещенную в тороидальном поле field от текущей на steps влево
     */
    @Contract("steps must be positive")
    fun left(steps: Int = 1): Coords {
        var x1 = x - steps
        if (x1 < 0) {
            x1 += field.config.width
        }
        return Coords(field, x1, y)
    }

    /**
     * @return  точку, смещенную в тороидальном поле field от текущей на steps вправо
     */
    @Contract("steps must be positive")
    fun right(steps: Int = 1): Coords {
        val x1 = (x + steps) % field.config.width
        return Coords(field, x1, y)
    }

    /**
     * @return  точку, смещенную в тороидальном поле field от текущей на steps вверх
     */
    @Contract("steps must be positive")
    fun up(steps: Int = 1): Coords {
        var y1 = y - steps
        if (y1 < 0) {
            y1 += field.config.height
        }
        return Coords(field, x, y1)
    }

    /**
     * @return  точку, смещенную в тороидальном поле field от текущей на steps вниз
     */
    @Contract("steps must be positive")
    fun down(steps: Int = 1): Coords {
        val y1 = (y + steps) % field.config.height
        return Coords(field, x, y1)
    }

    fun to(direction: Direction): Coords {
        return when (direction) {
            Direction.UP -> up()
            Direction.RIGHT -> right()
            Direction.DOWN -> down()
            Direction.LEFT -> left()
        }
    }

    /**
     * @return  все соседние точки для текущей (не по диагонали)
     */
    fun nearest(): Array<Coords> {
        return arrayOf(up(), right(), down(), left())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Coords

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * x + y
    }

    override fun toString(): String {
        return "Coords(x=$x, y=$y)"
    }
}
