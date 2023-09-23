package api.v1.dto

import model.GameConfig

class Game(
    val config: GameConfig,
    val canJoin: Boolean,
    val players: Array<Player>
)