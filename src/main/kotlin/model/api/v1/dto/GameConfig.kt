package model.api.v1.dto

data class GameConfig(
    val width: Int = 40,
    val height: Int = 30,
    val foodStatic: Int = 1,
    val stateDelayMs: Int = 500
) {
    init {
        val errors: MutableList<String> = mutableListOf()
        if (width !in 10..100) {
            errors.add("the field width must be an integer value in a [10, 100] range")
        }
        if (height !in 10..100) {
            errors.add("the field height must be an integer value in a [10, 100] range")
        }
        if (foodStatic <= 0) {
            errors.add("the static food must be a positive integer value")
        }
        if (stateDelayMs <= 0) {
            errors.add("the game state delay must be an integer value in a [10, 100] range")
        }
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException()
        }
    }
}