package api.v1.controller

import api.v1.Mapper
import api.v1.dto.*
import me.ippolitov.fit.snakes.SnakesProto.GameMessage
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class GameRestController(
    private val socket: DatagramSocket
) {
    private var msgSeq: Long = 0

    private fun send(address: InetSocketAddress, message: GameMessage) {
        val byteArray = message.toByteArray()
        val packet = DatagramPacket(byteArray, byteArray.size, address)
        socket.send(packet)
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

    fun error(error: Error) {
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
}