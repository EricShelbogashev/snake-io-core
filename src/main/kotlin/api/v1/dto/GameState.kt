package api.v1.dto

import java.net.InetSocketAddress

class GameState(
    override val address: InetSocketAddress,
    override val senderId: Int,
    val number: Int,
    val players: Array<Player>,
    val food: Array<Food>,
    val snakes: Array<Snake>
) : Message(address, senderId)