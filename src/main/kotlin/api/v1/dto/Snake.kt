package api.v1.dto

class Snake(
    val direction: Direction,
    val playerId: Int,
    val state: State,
    val points: Array<Coords>
) {
    enum class State {
        ALIVE, ZOMBIE
    }
}