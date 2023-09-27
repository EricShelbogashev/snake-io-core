package state

import Context
import GameClientPermissionLayer
import api.v1.controller.GameRestController
import api.v1.dto.GameState
import controller.GameController
import game.engine.Field
import model.GameConfig

abstract class MatchState(
    protected val context: Context,
    val config: GameConfig,
    gameClientPermissionLayer: GameClientPermissionLayer
) : State(context, gameClientPermissionLayer),
    GameController {
    private var onGameStateChangeListener: ((state: GameState) -> Unit)? = null
    protected val field = Field(config)
    protected val controller = GameRestController(context.commonSocket)


    override fun leaveGame() {
        onGameStateChangeListener = null
        gameClientPermissionLayer.changeState(LobbyState(context, gameClientPermissionLayer))
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