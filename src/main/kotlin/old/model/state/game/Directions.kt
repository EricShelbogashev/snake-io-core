package model.state.game;

import me.ippolitov.fit.snakes.SnakesProto.Direction
import java.net.DatagramPacket

class Directions {
    private val data: MutableMap<Int, Direction> = mutableMapOf()
    private val sequence: MutableMap<Int, Long> = mutableMapOf()

    @Synchronized
    fun request(message: DatagramPacket) {
        val direction: Direction = message.steer.direction
        val senderId = message.senderId
        val newSequenceNumber = message.msgSeq
        if (!data.containsKey(message.senderId)) {
            data[senderId] = direction
        } else {
            if (sequence[senderId]!! < newSequenceNumber) {
                data[senderId] = direction
                sequence[senderId] = newSequenceNumber
            }
        }
    }

    @Synchronized
    fun readAll(): Map<Int, model.state.game.engine.Direction> {
        val result: Map<Int, model.state.game.engine.Direction> = data.mapValues {
            when (it.value) {
                Direction.UP -> model.state.game.engine.Direction.UP
                Direction.RIGHT -> model.state.game.engine.Direction.RIGHT
                Direction.DOWN -> model.state.game.engine.Direction.DOWN
                Direction.LEFT -> model.state.game.engine.Direction.LEFT
            }
        }
        data.clear()
        sequence.clear()
        return result
    }
}
