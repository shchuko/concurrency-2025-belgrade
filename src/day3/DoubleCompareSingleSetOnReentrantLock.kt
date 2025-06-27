@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import java.util.concurrent.atomic.*
import java.util.concurrent.locks.*
import kotlin.concurrent.*

// This implementation never stores `null` values.
class DoubleCompareSingleSetOnReentrantLock<E : Any>(initialValue: E) : DoubleCompareSingleSet<E> {
    private val a = AtomicReference<E>()
    private val aLock = ReentrantLock()

    private val b = AtomicReference<E>()

    init {
        a.set(initialValue)
        b.set(initialValue)
    }

    override fun getA(): E = aLock.withLock {
        return a.get()
    }

    override fun dcss(
        expectedA: E, updateA: E, expectedB: E
    ): Boolean = aLock.withLock {
        val curA = a.get()
        if (curA !== expectedA) return false
        if (b.get() !== expectedB) return false
        a.set(updateA)
        return true
    }

    override fun setB(value: E) {
        b.set(value)
    }

    override fun getB(): E {
        return b.get()
    }
}