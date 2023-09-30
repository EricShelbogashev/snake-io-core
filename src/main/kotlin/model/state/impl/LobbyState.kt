package model.state.impl

import model.Context
import model.api.v1.dto.GameConfig
import model.LobbyController
import model.api.v1.dto.Announcement
import model.cache.TimebasedCache
import model.state.State
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit

class LobbyState(private val context: Context) : State, LobbyController {
    private var gameAnnouncementListener: ((announcements: List<Announcement>) -> Unit)? = null
    private var announcementsCache = TimebasedCache<InetSocketAddress, Announcement>(TimeUnit.SECONDS.toMillis(3))
    private var announcementsListenTask: Timer? = null

    override fun newGame(playerName: String, gameName: String, config: GameConfig) {
        removeGameAnnouncementsListener()
        context.gameClientPermissionLayer.changeState(
            MasterMatchState(context, playerName, gameName, config)
        )
    }

    override fun joinGame(playerName: String, gameName: String) {
        TODO("Not yet implemented")
    }

    override fun watchGame(playerName: String, gameName: String) {
        TODO("Not yet implemented")
    }

    override fun setGameAnnouncementsListener(action: (announcements: List<Announcement>) -> Unit) {
        context.gameNetController.setOnAnnouncementHandler { announcement ->
            synchronized(this) {
                announcementsCache.store(announcement.address, announcement)
            }
        }
        gameAnnouncementListener = action
        context.gameNetController.startReceive()
        startAnnouncementsListen()
    }

    private fun startAnnouncementsListen() {
        val lobbyState: LobbyState = this
        announcementsListenTask = with(Timer()) {
            this.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    synchronized(lobbyState) {
                        val list = announcementsCache.toList()
                        gameAnnouncementListener?.invoke(list)
                    }
                }
            }, 0, 1000)
            this
        }
    }

    private fun endAnnouncementsListen() {
        announcementsListenTask?.cancel()
        announcementsListenTask = null
    }

    override fun removeGameAnnouncementsListener() {
        gameAnnouncementListener = null
        context.gameNetController.setOnAnnouncementHandler(null)
        context.gameNetController.endReceive()
        endAnnouncementsListen()
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}