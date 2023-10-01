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
        context.stateHolder.change(
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
        context.connectionManager.setOnAnnouncementHandler { announcement ->
            synchronized(this) {
                announcementsCache.store(announcement.address, announcement)
            }
        }
        gameAnnouncementListener = action
        startAnnouncementsListen()
    }

    override fun exit() {
        TODO("Not yet implemented")
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

    override fun close() {
        gameAnnouncementListener = null
        context.connectionManager.setOnAnnouncementHandler(null)
        endAnnouncementsListen()
    }

}