package model.cache

import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TimebasedCache<K, V>(private val cacheDurationMillis: Long = TimeUnit.MINUTES.toMillis(1)) {
    private val data = mutableMapOf<K, Pair<V, Instant>>()
    private val lock = ReentrantLock()
    private var lastInvalidate: Instant = Instant.now()

    @Synchronized
    fun store(key: K, value: V) {
        val now = Instant.now()
        val item = value to now

        lock.withLock {
            data[key] = item

            if (shouldInvalidateCache(now)) {
                invalidateAll(now)
                lastInvalidate = now
            }
        }
    }

    fun size(): Int {
        return data.size
    }

    @Synchronized
    fun load(key: K): V? {
        val cacheEntry = data[key] ?: return null
        val (cachedValue, entryInstant) = cacheEntry

        if (shouldInvalidateCache(Instant.now(), entryInstant)) {
            lock.withLock {
                data.remove(key)
            }
            return null
        }

        return cachedValue
    }

    @Synchronized
    fun toList(): List<V> {
        val now = Instant.now()
        if (shouldInvalidateCache(now)) {
            invalidateAll(now)
            lastInvalidate = now
        }
        return data.values.map { it.first }
    }

    private fun shouldInvalidateCache(now: Instant, entryInstant: Instant? = null): Boolean {
        return entryInstant?.plusMillis(cacheDurationMillis)?.isBefore(now) == true
    }

    private fun invalidateAll(now: Instant) {
        val keysToInvalidate = data.keys.toList()
        for (key in keysToInvalidate) {
            if (shouldInvalidateCache(now, data[key]?.second)) {
                lock.withLock {
                    data.remove(key)
                }
            }
        }
    }
}
