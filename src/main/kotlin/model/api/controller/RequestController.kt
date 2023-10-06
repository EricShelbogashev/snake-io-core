package model.api.controller

import me.ippolitov.fit.snakes.SnakesProto.GameMessage
import model.Mapper
import model.api.v1.dto.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicLong

class RequestController(
    private val socket: DatagramSocket,
    private val networkInterface: NetworkInterface
) {
    private val multicastSupported: Boolean = networkInterface.supportsMulticast()

    private fun send(address: InetSocketAddress, message: GameMessage) : Long {
        if (address.address.isMulticastAddress && !multicastSupported) {
            throw IllegalArgumentException("multicast is not allowed for interface ${networkInterface.name}")
        }
        val byteArray = message.toByteArray()
        val packet = DatagramPacket(byteArray, byteArray.size, address)
        socket.send(packet)
        return message.msgSeq
    }

    fun ping(ping: Ping): Long {
        val protoPing = Mapper.toProtoPing(ping)
        return send(ping.address, protoPing)
    }

    fun steer(steer: Steer): Long {
        val protoDirection = Mapper.toProtoDirection(steer.direction)

        val protoSteer = GameMessage.SteerMsg.newBuilder()
            .setDirection(protoDirection)
            .build()

        val message = GameMessage.newBuilder()
            .setSteer(protoSteer)
            .setMsgSeq(steer.msgSeq)
            .setSenderId(steer.senderId)
            .build()

        return send(steer.address, message)
    }

    fun ack(ack: Ack): Long {
        val protoAck = GameMessage.AckMsg.newBuilder().build()
        val message = GameMessage.newBuilder()
            .setAck(protoAck)
            .setMsgSeq(ack.msgSeq)
            .setSenderId(ack.senderId)
            .setReceiverId(ack.receiverId)
            .build()

        return send(ack.address, message)
    }

    fun announcement(announcement: Announcement): Long {
        val protoAnnouncement = Mapper.toProtoAnnouncement(announcement)

        val message = GameMessage.newBuilder()
            .setAnnouncement(protoAnnouncement)
            .setMsgSeq(announcement.msgSeq)
            .setSenderId(announcement.senderId)
            .build()

        return send(announcement.address, message)
    }

    fun discover(discover: Discover): Long {
        val protoDiscover = GameMessage.DiscoverMsg.newBuilder().build()

        val message = GameMessage.newBuilder()
            .setDiscover(protoDiscover)
            .setMsgSeq(discover.msgSeq)
            .setSenderId(discover.senderId)
            .build()

        return send(discover.address, message)
    }

    fun join(join: Join): Long {
        val protoJoin = Mapper.toProtoJoin(join)

        val message = GameMessage.newBuilder()
            .setJoin(protoJoin)
            .setMsgSeq(join.msgSeq)
            .setSenderId(join.senderId)
            .build()

        return send(join.address, message)
    }

    fun error(error: model.api.v1.dto.Error) : Long {
        val protoError = Mapper.toProtoError(error)

        val protoMessage = GameMessage.newBuilder()
            .setError(protoError)
            .setMsgSeq(error.msgSeq)
            .setSenderId(error.senderId)
            .build()

        return send(error.address, protoMessage)
    }

    fun roleChange(roleChange: RoleChange): Long {
        val protoRoleChange = Mapper.toProtoRoleChange(roleChange)

        val message = GameMessage.newBuilder()
            .setRoleChange(protoRoleChange)
            .setMsgSeq(roleChange.msgSeq)
            .setSenderId(roleChange.senderId)
            .setReceiverId(roleChange.receiverId)
            .build()

        return send(roleChange.address, message)
    }

    fun state(gameState: GameState): Long {
        val protoState = Mapper.toProtoState(gameState)

        val message = GameMessage.newBuilder()
            .setState(protoState)
            .setMsgSeq(gameState.msgSeq)
            .setSenderId(gameState.senderId)
            .build()

        return send(gameState.address, message)
    }
}