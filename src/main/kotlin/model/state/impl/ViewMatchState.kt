@file:Suppress("JoinDeclarationAndAssignment")

package model.state.impl

import model.Context
import model.api.v1.dto.GameConfig
import model.api.v1.dto.GameState
import model.api.v1.dto.NodeRole
import model.api.v1.dto.Player
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
    private val logger = KotlinLogging.logger {}
    protected var deputy: Player? = null
    protected lateinit var master: Player
    protected lateinit var me: Player
    private var initialized = false

    init {
        context.connectionManager.setOnGameStateHandler(::gameStateHandle)
        context.connectionManager.setStateDelayMs(config.stateDelayMs.toLong())
    }

    private fun gameStateHandle(gameState: GameState) {
        gameState.players.forEach { player ->
            if (player.id == playerId) {
                me = player
            }
            when (player.role) {
                NodeRole.MASTER -> {
                    master = player
                }

                NodeRole.DEPUTY -> {
                    deputy = player
                }

                else -> {}
            }
        }
        initialized = true
        updateGameState(gameState)
    }

    override fun leaveGame() {
        // TODO: сообщить об уходе
        super.leaveGame()
    }

    // Предполагается, что мастер отвалился
    override fun onNodeRemoved(address: InetSocketAddress, role: NodeRole) {
        if (deputy != null) {
            master = deputy as Player
        } else {
            context.stateHolder.change(LobbyState(context))
        }
    }

    override fun close() {
        logger.debug { "ViewMatchState::close ()" }
        executors.shutdownNow()
        context.connectionManager.setOnGameStateHandler(null)
        super.close()
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
            return future.get(config.stateDelayMs * 3L, TimeUnit.MILLISECONDS)
        }
        return me
    }
}
