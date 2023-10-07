@file:Suppress("JoinDeclarationAndAssignment")

package model.state.impl

import model.Context
import model.GameController
import model.api.v1.dto.*
import mu.KotlinLogging
import mu.Marker
import java.net.InetSocketAddress

class NormalMatchState(
    context: Context,
    playerId: Int,
    gameName: String,
    config: GameConfig,
    private val playerName: String
) : ViewMatchState(context, playerId, gameName, config),
    GameController {
    private val logger = KotlinLogging.logger {}

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
        logger.info("NormalMatchState::onNodeRemoved () address=$address, role=$role, deputy=${deputy}, me=${me}")
        if (role == NodeRole.MASTER && deputy == me) {
            if (gameState == null) {
                throw IllegalStateException("игровое состояние не найдено, вероятно, смена ролей произошла до получения информации об игровом поле")
            }
            context.stateHolder.change(
                MasterMatchState(
                    context,
                    playerName,
                    gameName,
                    config,
                    gameState!!,
                    me
                )
            )
        } else super.onNodeRemoved(address, role)
    }

    override fun onRoleChanged(address: InetSocketAddress, role: NodeRole) {
        if (role == NodeRole.DEPUTY) {
            deputy = me
        } else {
            super.onRoleChanged(address, role)
        }
    }
}
