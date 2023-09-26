package state

import com.google.protobuf.InvalidProtocolBufferException
import me.ippolitov.fit.snakes.SnakesProto.GameMessage
import Context
import GameClientPermissionLayer
import model.controller.LobbyController
import model.controller.OnGameAnnouncementListener
import model.state.client.MasterGameState
import model.state.game.GameConfig
import java.net.DatagramPacket
import java.net.MulticastSocket

class LobbyState internal constructor(
    private val context: Context, private val gameClientPermissionLayer: GameClientPermissionLayer
) : State(context), LobbyController {
    private var listener: OnGameAnnouncementListener? = null
    private var receiveAnnouncementsTask: GameAnnouncementListenerTask? = null

    override fun newGame(config: GameConfig) {
        val state = MasterGameState(context, config)
        receiveAnnouncementsTask?.interrupt()
        gameClientPermissionLayer.changeState(state)
    }

    override fun joinGame(gameName: String) {
        TODO("low-level sending message")
    }

    override fun watchGame(gameName: String) {
        TODO("Not yet implemented")
    }

    override fun setGameAnnouncementListener(listener: OnGameAnnouncementListener) {
        this.listener = listener
        val task = GameAnnouncementListenerTask(context.inputSocket, listener)
        task.start()
        receiveAnnouncementsTask?.interrupt()
        receiveAnnouncementsTask = task
    }

    override fun removeGameAnnouncementListener() {
        receiveAnnouncementsTask?.interrupt()
        receiveAnnouncementsTask = null
        listener = null
    }

    private class GameAnnouncementListenerTask(
        private val inputSocket: MulticastSocket, private val listener: OnGameAnnouncementListener
    ) : Thread() {
        private var buffer = ByteArray(4 * 1024)
        override fun run() {
            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                inputSocket.receive(packet)
                val gameMessage = try {
                    GameMessage.parseFrom(packet.data)
                } catch (e: InvalidProtocolBufferException) {
                    System.err.println(e.message)
                    e.printStackTrace()
                    continue
                }
                val announcement = gameMessage.announcement
                listener.receiveAnnouncement(announcement)
            }
        }
    }
}