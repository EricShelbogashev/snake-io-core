@file:Suppress("JoinDeclarationAndAssignment")

package model.state.impl

import model.Context
import model.GameController
import model.api.v1.dto.*
import java.net.InetSocketAddress

class NormalMatchState(
    context: Context,
    playerId: Int,
    gameName: String,
    config: GameConfig
) : ViewMatchState(context, playerId, gameName, config),
    GameController {

    override fun move(direction: Direction) {
        context.connectionManager.send(
            Steer(
                address = master.address,
                senderId = playerId,
                direction = direction
            )
        )
    }

    // Предполагается, что единственная нода, к которой мы подключены - MASTER
    override fun onNodeRemoved(address: InetSocketAddress, role: NodeRole) {
        if (deputy == me) {
            TODO("был заместителем, стал главным")
//            context.stateHolder.change(
//                MasterMatchState(
//
//                )
//            )
        } else super.onNodeRemoved(address, role)
    }
}
