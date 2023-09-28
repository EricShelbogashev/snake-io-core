package api.v1.controller

import api.v1.Mapper
import api.v1.dto.*
import me.ippolitov.fit.snakes.SnakesProto.GameMessage
import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.NetworkInterface
import kotlin.Error

@Suppress("USELESS_IS_CHECK")
class GameNetController(
    private val multicastGroup: InetSocketAddress,
    private val recieveSocket: DatagramSocket,
    private val commonSocket: DatagramSocket,
    private val networkInterface: NetworkInterface
) : Closeable {
    private var msgSeq: Long = 0
    private val multicastSupported: Boolean = networkInterface.supportsMulticast()
    private val recieveTask: Thread
    private var onAckHandler: ((ack: Ack) -> Unit)? = null
    private var onAnnouncementHandler: ((announcement: Announcement) -> Unit)? = null
    private var onErrorHandler: ((error: Error) -> Unit)? = null
    private var onGameStateHandler: ((gameState: GameState) -> Unit)? = null
    private var onJoinHandler: ((join: Join) -> Unit)? = null
    private var onPingHandler: ((ping: Ping) -> Unit)? = null
    private var onRoleChangeHandler: ((roleChange: RoleChange) -> Unit)? = null
    private var onSteerHandler: ((steer: Steer) -> Unit)? = null
    private var onOtherHandler: ((message: Message) -> Unit)? = null
    private var cachedState: Game? = null

    init {
        if (multicastSupported) {
            commonSocket.joinGroup(multicastGroup, networkInterface)
        }

        recieveTask = Thread {
            while (true) {
                val gameMessage = try {
                    recieve()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    continue
                }

                dispatch(gameMessage)
            }
        }
        recieveTask.start()
    }

    private fun dispatch(gameMessage: Message) = when (gameMessage) {
        is Ack -> (onAckHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Announcement -> (onAnnouncementHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Discover -> announcement(
            Announcement(
                gameMessage.address,
                gameMessage.senderId,
                if (cachedState != null) arrayOf(cachedState!!) else arrayOf()
            )
        )
        is Error -> (onErrorHandler ?: onOtherHandler)?.invoke(gameMessage)
        is GameState -> (onGameStateHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Join -> (onJoinHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Ping -> (onPingHandler ?: onPingHandler)?.invoke(gameMessage)
        is RoleChange -> (onRoleChangeHandler ?: onOtherHandler)?.invoke(gameMessage)
        is Steer -> (onSteerHandler ?: onOtherHandler)?.invoke(gameMessage)
        else -> {
            System.err.println(
                "Undefined message from ${gameMessage.address}, senderId=${gameMessage.senderId}, seq=$msgSeq"
            )
        }
    }

    fun setOnOtherHandler(onOtherHandler: ((message: Message) -> Unit)?) {
        this.onOtherHandler = onOtherHandler
    }

    fun setOnAckHandler(onAckHandler: ((ack: Ack) -> Unit)?) {
        this.onAckHandler = onAckHandler
    }

    fun setOnAnnouncementHandler(onAnnouncementHandler: ((announcement: Announcement) -> Unit)?) {
        this.onAnnouncementHandler = onAnnouncementHandler
    }

    fun setOnErrorHandler(onErrorHandler: ((error: Error) -> Unit)?) {
        this.onErrorHandler = onErrorHandler
    }

    fun setOnGameStateHandler(onGameStateHandler: ((gameState: GameState) -> Unit)?) {
        this.onGameStateHandler = onGameStateHandler
    }

    fun setOnJoinHandler(onJoinHandler: ((join: Join) -> Unit)?) {
        this.onJoinHandler = onJoinHandler
    }

    fun setOnRoleChangeHandler(onRoleChangeHandler: ((roleChange: RoleChange) -> Unit)?) {
        this.onRoleChangeHandler = onRoleChangeHandler
    }

    fun setOnPingHandler(onPingHandler: ((ping: Ping) -> Unit)?) {
        this.onPingHandler = onPingHandler
    }

    fun setOnSteerHandler(onSteerHandler: ((steer: Steer) -> Unit)?) {
        this.onSteerHandler = onSteerHandler
    }

    // TODO: выбрасывать одноименную ошибку для удобства

    private fun send(address: InetSocketAddress, message: GameMessage) {
        if (address.address.isMulticastAddress && !multicastSupported) {
            throw IllegalArgumentException("multicast is not allowed for interface ${networkInterface.name}")
        }
        val byteArray = message.toByteArray()
        val packet = DatagramPacket(byteArray, byteArray.size, address)
        commonSocket.send(packet)
    }

    fun ping(ping: Ping) {
        ping.msgSeq = msgSeq++
        val protoPing = Mapper.toProtoPing(ping)
        send(ping.address, protoPing)
    }

    fun steer(steer: Steer) {
        val protoDirection = Mapper.toProtoDirection(steer.direction)

        val protoSteer = GameMessage.SteerMsg.newBuilder()
            .setDirection(protoDirection)
            .build()

        val message = GameMessage.newBuilder()
            .setSteer(protoSteer)
            .setMsgSeq(msgSeq++)
            .setSenderId(steer.senderId)
            .build()

        send(steer.address, message)
    }

    fun ack(ack: Ack) {
        val protoAck = GameMessage.AckMsg.newBuilder().build()
        val message = GameMessage.newBuilder()
            .setAck(protoAck)
            .setMsgSeq(msgSeq++)
            .setSenderId(ack.senderId)
            .setReceiverId(ack.receiverId)
            .build()

        send(ack.address, message)
    }

    fun announcement(announcement: Announcement) {
        val protoAnnouncement = Mapper.toProtoAnnouncement(announcement)

        val message = GameMessage.newBuilder()
            .setAnnouncement(protoAnnouncement)
            .setMsgSeq(msgSeq++)
            .setSenderId(announcement.senderId)
            .build()

        send(announcement.address, message)
    }

    fun discover(discover: Discover) {
        val protoDiscover = GameMessage.DiscoverMsg.newBuilder().build()

        val message = GameMessage.newBuilder()
            .setDiscover(protoDiscover)
            .setMsgSeq(msgSeq++)
            .setSenderId(discover.senderId)
            .build()

        send(discover.address, message)
    }

    private fun join(join: Join) {
        val protoJoin = Mapper.toProtoJoin(join)

        val message = GameMessage.newBuilder()
            .setJoin(protoJoin)
            .setMsgSeq(msgSeq++)
            .setSenderId(join.senderId)
            .build()

        send(join.address, message)
    }

    fun error(error: api.v1.dto.Error) {
        val protoError = Mapper.toProtoError(error)

        val protoMessage = GameMessage.newBuilder()
            .setError(protoError)
            .setMsgSeq(msgSeq++)
            .setSenderId(error.senderId)
            .build()

        send(error.address, protoMessage)
    }

    fun roleChange(roleChange: RoleChange) {
        val protoRoleChange = Mapper.toProtoRoleChange(roleChange)

        val message = GameMessage.newBuilder()
            .setRoleChange(protoRoleChange)
            .setMsgSeq(msgSeq++)
            .setSenderId(roleChange.senderId)
            .setReceiverId(roleChange.receiverId)
            .build()

        send(roleChange.address, message)
    }

    fun state(gameState: GameState) {
        val protoState = Mapper.toProtoState(gameState)

        val message = GameMessage.newBuilder()
            .setState(protoState)
            .setMsgSeq(msgSeq++)
            .setSenderId(gameState.senderId)
            .build()

        send(gameState.address, message)
    }

    private fun recieve(): Message {
        val buffer = ByteArray(4 * 1024)
        val packet = DatagramPacket(buffer, buffer.size)
        recieveSocket.receive(packet)

        val parsed = GameMessage.parseFrom(buffer)
        return Mapper.toMessage(InetSocketAddress(packet.address, packet.port), parsed)
    }

    override fun close() {
        commonSocket.leaveGroup(multicastGroup, networkInterface)
    }
}