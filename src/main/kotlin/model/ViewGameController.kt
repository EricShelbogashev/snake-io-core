package model

import model.api.v1.dto.GameConfig
import model.api.v1.dto.GameState
import model.api.v1.dto.Player

interface ViewGameController : Controller {
    fun leaveGame()
    fun setOnGameStateChangeListener(action: (state: GameState) -> Unit)
    fun config(): GameConfig
    fun gameName(): String
    fun currentPlayer(): Player
}