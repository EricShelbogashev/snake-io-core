package model.cache

import java.time.Instant
import java.util.concurrent.TimeUnit

class TimebasedCache<K, V>(private val millis: Long = TimeUnit.MINUTES.toMillis(1)) {
    private val data = mutableMapOf<K, Pair<V, Instant>>()
    private var lastInvalidate: Instant = Instant.now()

    @Synchronized
    fun store(key: K, value: V) {
        val now = Instant.now()
        val item = value to now
        data[key] = item

        if (lastInvalidate.plusMillis(millis).isBefore(now)) {
            invalidateAll()
            lastInvalidate = now
        }
    }

    @Synchronized
    fun size() = data.size

    @Synchronized
    fun load(key: K): V? {
        val res = data[key] ?: return null
        if (res.second.plusMillis(millis).isBefore(Instant.now())) {
            data.remove(key)
        }
        return res.first
    }

    @Synchronized
    fun toList(): List<V> {
        val now = Instant.now()
        if (lastInvalidate.plusMillis(millis).isBefore(now)) {
            invalidateAll()
            lastInvalidate = now
        }
        return data.values.map {
            return@map it.first
        }
    }

    private fun invalidateAll() {
        val array = data.keys.toList()
        for (key in array) {
            load(key)
        }
    }
}