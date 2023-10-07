package model.state.impl

import model.Context
import model.api.v1.dto.GameConfig
import model.GameController
import model.ViewGameController
import model.api.JoinRequest
import model.api.v1.dto.GameState
import model.api.v1.dto.NodeRole
import model.state.State
import java.net.InetSocketAddress

abstract class MatchState(
    protected val context: Context, val config: GameConfig
) : State, ViewGameController {
    private var onGameStateChangeListener: ((state: GameState) -> Unit)? = null

    override fun initialize() {
        context.connectionManager.setOnNodeRemovedHandler(::onNodeRemoved)
    }

    override fun leaveGame() {
        val newState = LobbyState(context)
        context.stateHolder.change(newState)
    }

    override fun setOnGameStateChangeListener(action: (state: GameState) -> Unit) {
        onGameStateChangeListener = action
    }

    /**
     * Должен вызываться один раз в config.stateDelayMs, если Master
     * и с допустимой задержкой до 0.8*config.stateDelayMs, если нет.
     * */
    protected fun updateGameState(state: GameState) {
        onGameStateChangeListener?.invoke(state)
    }

    protected abstract fun onNodeRemoved(address: InetSocketAddress, role: NodeRole)

    override fun config(): GameConfig {
        return config
    }

    override fun close() {
        onGameStateChangeListener = null
        context.connectionManager.setOnNodeRemovedHandler(null)
    }
}