package api.v1.dto

import model.GameConfig

class Game(
    val gameName: String,
    val config: GameConfig,
    val canJoin: Boolean,
    val players: Array<Player>
) {
    override fun toString(): String {
        return "Game(config=$config, canJoin=$canJoin, players=${players.contentToString()})"
    }
}