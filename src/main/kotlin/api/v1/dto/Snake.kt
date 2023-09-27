package api.v1.dto

open class Snake(
    var direction: Direction,
    var playerId: Int,
    var state: State,
    var points: Array<Coords>
) {
    enum class State {
        ALIVE, ZOMBIE
    }

    override fun toString(): String {
        return "Snake(direction=$direction, playerId=$playerId, state=$state, points=${points.contentToString()})"
    }

}