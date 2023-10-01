package model.state.impl

import model.Context
import model.LobbyController
import model.api.v1.dto.*
import model.cache.TimebasedCache
import model.state.State
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit

class LobbyState(private val context: Context) : State, LobbyController {
    private var gameAnnouncementListener: ((announcements: List<Announcement>) -> Unit)? = null
    private var announcementsCache = TimebasedCache<InetSocketAddress, Announcement>(TimeUnit.SECONDS.toMillis(3))
    private var announcementsListenTask: Timer? = null

    init {
        context.connectionManager.setOnJoinAccepted { playerId: Int ->
            println("ПИЗДАААА $playerId")
        }
    }

    override fun newGame(playerName: String, gameName: String, config: GameConfig) {
        context.stateHolder.change(
            MasterMatchState(context, playerName, gameName, config)
        )
    }

    override fun joinGame(address: InetSocketAddress, playerName: String, gameName: String) {
        context.connectionManager.send(
            Join(
                address = address,
                senderId = 0,
                playerName = playerName,
                playerType = PlayerType.HUMAN,
                gameName = gameName,
                nodeRole = NodeRole.NORMAL
            )
        )
    }

    override fun watchGame(address: InetSocketAddress, playerName: String, gameName: String) {
        context.connectionManager.send(
            Join(
                address = address,
                senderId = 0,
                playerName = playerName,
                playerType = PlayerType.HUMAN,
                gameName = gameName,
                nodeRole = NodeRole.VIEWER
            )
        )
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