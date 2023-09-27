package controller;

import api.v1.dto.Direction
import api.v1.dto.Steer

class Directions {
    private val data: MutableMap<Int, Direction> = mutableMapOf()
    private val sequence: MutableMap<Int, Long> = mutableMapOf()

    @Synchronized
    fun request(senderId: Int, direction: Direction, sequenceNumber: Long) {
        if (!data.containsKey(senderId)) {
            data[senderId] = direction
            sequence[senderId] = sequenceNumber
        } else {
            if (sequence[senderId]!! < sequenceNumber) {
                data[senderId] = direction
                sequence[senderId] = sequenceNumber
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
