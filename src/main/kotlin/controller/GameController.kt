package controller

import model.GameConfig

interface GameController : Controller {
    fun leaveGame()
    fun setOnGameStateChangeListener(action: (state: api.v1.dto.GameState) -> Unit)
    fun config(): GameConfig
}