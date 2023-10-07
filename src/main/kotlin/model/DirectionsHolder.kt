package model

import model.api.v1.dto.Direction

class DirectionsHolder {
    private val data: MutableMap<Int, Pair<Direction, Long>> = mutableMapOf()

    @Synchronized
    fun request(senderId: Int, direction: Direction, sequenceNumber: Long) {
        data[senderId] = data[senderId]?.let { existingPair ->
            if (sequenceNumber > existingPair.second) {
                Pair(direction, sequenceNumber)
            } else {
                existingPair
            }
        } ?: Pair(direction, sequenceNumber)
    }

    @Synchronized
    fun readAll(): Map<Int, Direction> {
        val result = data.mapValues { it.value.first }
        data.clear()
        return result
    }
}
