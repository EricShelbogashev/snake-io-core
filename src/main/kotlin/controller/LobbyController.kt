package controller

import api.v1.dto.Announcement
import model.GameConfig

interface LobbyController : Controller {
    fun newGame(config: GameConfig)
    fun joinGame(gameName: String)
    fun watchGame(gameName: String)
    fun setGameAnnouncementListener(action: (announcement: Announcement) -> Unit)
    fun removeGameAnnouncementListener()
}