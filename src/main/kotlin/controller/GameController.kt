package controller

import api.v1.dto.Direction
import model.GameConfig

interface GameController : Controller {
    fun leaveGame()
    fun setOnGameStateChangeListener(action: (state: api.v1.dto.GameState) -> Unit)
    fun config(): GameConfig
    fun move(direction: Direction)
}