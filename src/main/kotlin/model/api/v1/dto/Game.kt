package model.api.v1.dto

class Game(
    val gameName: String,
    val config: GameConfig,
    val canJoin: Boolean,
    var players: Array<Player>
) {
    override fun toString(): String {
        return "Game(config=$config, canJoin=$canJoin, players=${players.contentToString()})"
    }
}