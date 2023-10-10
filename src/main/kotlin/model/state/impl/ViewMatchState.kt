@file:Suppress("JoinDeclarationAndAssignment")

package model.state.impl

import model.Context
import model.api.v1.dto.*
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.time.Instant
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

    private val logger = KotlinLogging.logger {  }
    private val executors = Executors.newScheduledThreadPool(USED_THREADS_NUMBER)
    protected var deputy: Player? = null
    protected lateinit var master: Player
    protected lateinit var me: Player
    protected var initialized = false
    protected var gameState: GameState? = null

    private fun gameStateHandle(gameState: GameState) {
        logger.debug { "получено обновление игрового поля для ${me.name}" }
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
        logger.warn { "смена ролей не имеет место в состоянии зрителя" }
    }

    override fun onNodeRemoved(address: InetSocketAddress, role: NodeRole) {
        if (!initialized) return
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
//        context.connectionManager.setOnGameStateHandler(null)
        context.connectionManager.setOnRoleChangeHandler(null)
        context.connectionManager.setOnNodeRemovedHandler(null)
        super.close()
    }

    override fun initialize() {
        logger.debug { "Режим зрителя включен" }
        context.connectionManager.setOnGameStateHandler(::gameStateHandle)
        context.connectionManager.setStateDelayMs(config.stateDelayMs.toLong())
        context.connectionManager.setOnRoleChangeHandler(::onRoleChanged)
        context.connectionManager.setOnNodeRemovedHandler(::onNodeRemoved)
    }

    override fun gameName(): String = gameName
    override fun currentPlayer(): Player {
        if (!initialized) {
            val future = CompletableFuture.supplyAsync {
                val now = Instant.now().toEpochMilli()
                val maxWaitTime = TimeUnit.SECONDS.toMillis(4)
                while (!initialized) {
                    logger.debug { "while (!initialized)" }
                    Thread.onSpinWait()
                    if (Instant.now().toEpochMilli() - now > maxWaitTime) {
                        context.stateHolder.change(LobbyState(context))
                        return@supplyAsync Player(
                            "",
                            3,
                            NodeRole.NORMAL,
                            PlayerType.HUMAN,
                            0,
                            ",",
                            3
                        )
                    }
                }
                me
            }
            return future.get()
        }
        return me
    }
}
