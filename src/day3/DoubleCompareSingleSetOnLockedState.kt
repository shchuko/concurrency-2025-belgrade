@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class DoubleCompareSingleSetOnLockedState<E : Any>(initialValue: E) : DoubleCompareSingleSet<E> {
    private val a = AtomicReference<Any>()
    private val b = AtomicReference<E>()

    init {
        a.set(initialValue)
        b.set(initialValue)
    }

    override fun getA(): E {
        while (true) {
            val curA = a.get()
            if (curA !== LOCKED) {
                return curA as E
            }
        }
    }

    override fun dcss(
        expectedA: E, updateA: E, expectedB: E
    ): Boolean {
        // try lock A
        var curA: Any
        while (true) {
            curA = a.getAndSet(LOCKED)

            when (curA) {
                // the lock is held by someone else, try again
                LOCKED -> continue

                // we successfully acquired the lock, and the A is expected
                expectedA -> {
                    if (b.get() == expectedB) {
                        // B is also expected -> update A and return true
                        a.set(updateA)
                        return true
                    }

                    // B is not expected -> reset A to the previous value and return false
                    a.set(curA)
                    return false
                }

                // we successfully acquired the lock, but the A is not expected -> release the lock and return false
                else -> {
                    a.set(curA)
                    return false
                }
            }
        }
    }

    override fun setB(value: E) {
        b.set(value)
    }

    override fun getB(): E {
        return b.get()
    }
}

private val LOCKED = "Locked"
