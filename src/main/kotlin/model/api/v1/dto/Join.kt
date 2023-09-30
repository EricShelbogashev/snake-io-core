package model.api.v1.dto

import java.net.InetSocketAddress

class Join(
    address: InetSocketAddress,
    senderId: Int,
    val playerName: String,
    val gameName: String,
    val playerType: PlayerType = PlayerType.HUMAN,
    val nodeRole: NodeRole
) : Message(address, senderId) {
    init {
        if (nodeRole == NodeRole.DEPUTY || nodeRole == NodeRole.MASTER) {
            throw IllegalArgumentException("the node's role in the join request should only be DEPUTY or MASTER")
        }
    }

    override fun toString(): String {
        return "Join(address=$address, senderId=$senderId, playerName='$playerName', gameName='$gameName', playerType=$playerType, nodeRole=$nodeRole)"
    }
}