package model

import model.api.v1.dto.Announcement
import model.api.v1.dto.GameConfig

interface LobbyController : Controller {
    fun newGame(playerName: String, gameName: String, config: GameConfig)
    fun joinGame(playerName: String, gameName: String)
    fun watchGame(playerName: String, gameName: String)
    fun setGameAnnouncementsListener(action: (announcements: List<Announcement>) -> Unit)
    fun exit()
}