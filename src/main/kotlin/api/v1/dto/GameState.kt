package api.v1.dto

import java.net.InetSocketAddress

class GameState(
    override var address: InetSocketAddress,
    override var senderId: Int,
    var number: Int,
    var players: Array<Player>,
    var food: Array<Food>,
    var snakes: Array<Snake>
) : Message(address, senderId) {
    override fun toString(): String {
        return "GameState(address=$address, senderId=$senderId, number=$number, players=${players.contentToString()}, food=${food.contentToString()}, snakes=${snakes.contentToString()})"
    }
}