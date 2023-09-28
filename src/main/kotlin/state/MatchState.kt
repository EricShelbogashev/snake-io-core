package state

import Context
import api.v1.dto.GameState
import controller.GameController
import model.GameConfig

abstract class MatchState(
    context: Context, val config: GameConfig
) : State(context), GameController {
    private var onGameStateChangeListener: ((state: GameState) -> Unit)? = null

    override fun leaveGame() {
        onGameStateChangeListener = null
        context.gameClientPermissionLayer.changeState(LobbyState(context))
        context.gameNetController.close()
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
}