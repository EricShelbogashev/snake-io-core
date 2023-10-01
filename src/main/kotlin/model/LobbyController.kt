package model

import model.api.v1.dto.Announcement
import model.api.v1.dto.GameConfig
import java.net.InetSocketAddress

interface LobbyController : Controller {
    fun newGame(playerName: String, gameName: String, config: GameConfig)
    fun joinGame(address: InetSocketAddress, playerName: String, gameName: String)
    fun watchGame(address: InetSocketAddress, playerName: String, gameName: String)
    fun setGameAnnouncementsListener(action: (announcements: List<Announcement>) -> Unit)
    fun exit()
}