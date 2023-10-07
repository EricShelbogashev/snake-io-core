@file:Suppress("JoinDeclarationAndAssignment")

package model.state.impl

import model.Context
import model.api.v1.dto.*
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class ViewMatchState(
    context: Context,
    val playerId: Int,
    val gameName: String,
    config: GameConfig
) : MatchState(context, config) {
    private companion object {
        const val USED_THREADS_NUMBER = 1
    }

    private val executors = Executors.newScheduledThreadPool(USED_THREADS_NUMBER)
    protected var deputy: Player? = null
    protected lateinit var master: Player
    protected lateinit var me: Player
    private var initialized = false
    protected var gameState: GameState? = null

    private fun gameStateHandle(gameState: GameState) {
        gameState.players.forEach { player ->
            if (player.id == playerId) {
                me = player
            }
            when (player.role) {
                NodeRole.MASTER -> master = player
                NodeRole.DEPUTY -> deputy = player
                else -> {}
            }
        }
        this.gameState = gameState
        initialized = true
        updateGameState(gameState)
    }

    protected open fun onRoleChanged(address: InetSocketAddress, role: NodeRole) {
        if (role == NodeRole.MASTER) {
            if (gameState == null) {
                throw IllegalStateException("смена ролей произошла до получения информации об игровом состоянии, невозможно произвести смену ролей")
            }
            val found = gameState!!.players.find { player ->
                player.address == address
            } ?: throw IllegalStateException("если игрока нет, значит смены ролей произойти не могло")

            found.role = NodeRole.MASTER
            master = found
        }
    }

    override fun onNodeRemoved(address: InetSocketAddress, role: NodeRole) {
        if (role != NodeRole.MASTER) {
            return
        }
        if (deputy != null) {
            master = deputy as Player
            context.connectionManager.trackNode(master.address, NodeRole.MASTER)
        } else {
            context.stateHolder.change(LobbyState(context))
        }
    }

    override fun close() {
        executors.shutdownNow()
        context.connectionManager.setOnGameStateHandler(null)
        context.connectionManager.setOnNodeRemovedHandler(null)
        super.close()
    }

    override fun initialize() {
        context.connectionManager.setOnGameStateHandler(::gameStateHandle)
        context.connectionManager.setStateDelayMs(config.stateDelayMs.toLong())
        context.connectionManager.setOnRoleChangeHandler(::onRoleChanged)
        context.connectionManager.setOnNodeRemovedHandler(::onNodeRemoved)
    }

    override fun gameName(): String = gameName
    override fun currentPlayer(): Player {
        if (!initialized) {
            val future = CompletableFuture.supplyAsync {
                while (!initialized) {
                    Thread.onSpinWait()
                }
                me
            }
            return future.get()
        }
        return me
    }
}
