package model.controller

interface GameController : Controller {
    fun leaveGame()
    fun setOnGameStateChangeListener(listener: OnGameStateChangeListener)
}