package model

import model.api.v1.dto.Direction
import model.api.v1.dto.GameConfig
import model.api.v1.dto.GameState
import model.api.v1.dto.Player

interface GameController : Controller {
    fun leaveGame()
    fun setOnGameStateChangeListener(action: (state: GameState) -> Unit)
    fun move(direction: Direction)
    fun config(): GameConfig
    fun gameName(): String
    fun currentPlayer(): Player
}