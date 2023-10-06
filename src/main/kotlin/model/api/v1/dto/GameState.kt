package model.api.v1.dto

import java.net.InetSocketAddress

class GameState(
    address: InetSocketAddress,
    senderId: Int,
    var number: Int,
    var players: Array<Player>,
    var food: Array<Food>,
    var snakes: Array<Snake>
) : Message(address, senderId) {
    private val playerzz: MutableMap<Int, Player> = mutableMapOf()
    private val master: Player
    private val deputy: Player?
    init {
        var masterTmp: Player? = null
        var deputyTmp: Player? = null
        for (player in players) {
            playerzz[player.id] = player
            when (player.role) {
                NodeRole.MASTER -> masterTmp = player
                NodeRole.DEPUTY -> deputyTmp = player
                else -> {}
            }
        }
        if (masterTmp == null) {
            throw IllegalArgumentException("узел мастера должен быть в списке игроков")
        }
        master = masterTmp
        deputy = deputyTmp
    }

    override fun toString(): String {
        return "GameState(address=$address, senderId=$senderId, number=$number, players=${players.contentToString()}, food=${food.contentToString()}, snakes=${snakes.contentToString()})"
    }

    fun master(): Player {
        return master
    }

    fun deputy(): Player? {
        return deputy
    }
}