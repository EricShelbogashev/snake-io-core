package model.controller

import model.state.game.GameConfig

interface LobbyController : Controller {
    fun newGame(config: GameConfig)
    fun joinGame(gameName: String)
    fun watchGame(gameName: String)
    fun setGameAnnouncementListener(listener: OnGameAnnouncementListener)
    fun removeGameAnnouncementListener()
}