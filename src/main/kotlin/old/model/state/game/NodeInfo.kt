package model.state.game;

import me.ippolitov.fit.snakes.SnakesProto

data class NodeInfo(
    val ip: String,
    val port: Int,
    val role: SnakesProto.NodeRole,
    val type: SnakesProto.PlayerType
)