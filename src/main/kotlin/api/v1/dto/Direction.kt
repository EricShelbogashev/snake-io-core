package api.v1.dto

enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    companion object Utils {
        fun random(): Direction {
            return entries.random()
        }
    }
}