package state

import me.ippolitov.fit.snakes.SnakesProto
import Context
import model.controller.GameController
import model.controller.OnGameStateChangeListener

abstract class GameState(protected val context: Context) : State(context),
    GameController {
    private var onGameStateChangeListener: OnGameStateChangeListener? = null

    override fun leaveGame() {
        onGameStateChangeListener = null
    }

    override fun setOnGameStateChangeListener(listener: OnGameStateChangeListener) {
        this.onGameStateChangeListener = listener
    }

    /**
     * Должен вызываться один раз в config.stateDelayMs, если Master
     * и с допустимой задержкой до 0.8*config.stateDelayMs, если нет.
     * */
    protected open fun updateGameState(state: SnakesProto.GameState) {
        onGameStateChangeListener?.updateState(state)
    }
}