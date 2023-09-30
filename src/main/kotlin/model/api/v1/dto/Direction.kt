package model.api.v1.dto

enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    companion object Utils {
        fun random(): Direction {
            return entries.random()
        }
    }

    fun reverse(): Direction {
        return when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
    }
}