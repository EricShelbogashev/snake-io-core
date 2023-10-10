@file:Suppress("JoinDeclarationAndAssignment")

package model.state.impl

import model.Context
import model.GameController
import model.api.v1.dto.Direction
import model.api.v1.dto.GameConfig
import model.api.v1.dto.NodeRole
import model.api.v1.dto.Steer
import mu.KotlinLogging
import java.net.InetSocketAddress

class NormalMatchState(
    context: Context,
    playerId: Int,
    gameName: String,
    config: GameConfig,
    private val playerName: String
) : ViewMatchState(context, playerId, gameName, config),
    GameController {

    private val logger = KotlinLogging.logger {  }
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
        if (!initialized) {
            return super.onNodeRemoved(address, role)
        }
        if (role == NodeRole.MASTER && deputy == me) {
            if (gameState == null) {
                throw IllegalStateException("игровое состояние не найдено, вероятно, смена ролей произошла до получения информации об игровом поле")
            }
            promoteToMaster()
        } else super.onNodeRemoved(address, role)
    }

    private fun promoteToMaster() {
        me.role = NodeRole.MASTER
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
    }

    override fun onRoleChanged(address: InetSocketAddress, role: NodeRole) {
        if (!initialized) {
            return context.stateHolder.change(LobbyState(context))
        }
        when (role) {
            NodeRole.DEPUTY -> {
                deputy = me
            }
            NodeRole.MASTER -> {
                promoteToMaster()
            }
            else -> {
                logger.warn { "получен запрос на смену роли с ${me.role} на $role" }
            }
        }
    }
}
