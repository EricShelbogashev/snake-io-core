package controller

import api.v1.dto.Announcement
import model.GameConfig

interface LobbyController : Controller {
    fun newGame(playerName: String, gameName: String, config: GameConfig)
    fun joinGame(playerName: String, gameName: String)
    fun watchGame(playerName: String, gameName: String, )
    fun setGameAnnouncementListener(action: (announcement: Announcement) -> Unit)
    fun removeGameAnnouncementListener()
}