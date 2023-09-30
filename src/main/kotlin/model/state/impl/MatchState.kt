package model.state.impl

import model.Context
import model.api.v1.dto.GameConfig
import model.GameController
import model.api.v1.dto.GameState
import model.state.State

abstract class MatchState(
    private val context: Context, val config: GameConfig
) : State, GameController {
    private var onGameStateChangeListener: ((state: GameState) -> Unit)? = null

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
    protected open fun updateGameState(state: GameState) {
        onGameStateChangeListener?.invoke(state)
    }

    override fun config(): GameConfig {
        return config
    }

    override fun close() {
        onGameStateChangeListener = null
    }
}