package controller;

import api.v1.dto.Direction
import api.v1.dto.Steer

class Directions {
    private val data: MutableMap<Int, Direction> = mutableMapOf()
    private val sequence: MutableMap<Int, Long> = mutableMapOf()

    @Synchronized
    fun request(message: Steer) {
        if (!data.containsKey(message.senderId)) {
            data[message.senderId] = message.direction
        } else {
            val newSequenceNumber = message.msgSeq
            if (sequence[message.senderId]!! < newSequenceNumber) {
                data[message.senderId] = message.direction
                sequence[message.senderId] = newSequenceNumber
            }
        }
    }

    @Synchronized
    fun readAll(): Map<Int, Direction> {
        val result: Map<Int, Direction> = data.toMap()
        data.clear()
        sequence.clear()
        return result
    }
}
