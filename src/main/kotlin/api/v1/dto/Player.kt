package api.v1.dto

class Player(
    val port: Int,
    val role: NodeRole,
    val type: PlayerType,
    val score: Int,
    val ip: String,
    val name: String,
    val id: Int
)