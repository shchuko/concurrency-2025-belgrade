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
            curA = a.get()
            // if it was already locked, do one more iteration
            if (curA === LOCKED) {
                continue
            }

            if (curA != expectedA) {
                return false
            }

            // on a successful lock, exit the loop
            if (a.compareAndSet(curA, LOCKED)) {
                break
            }
        }

        // sanity check
        check(a.get() === LOCKED)

        // if A or B does not contain the expected values, reset unlock the A and proceed
        if (curA != expectedA || b.get() != expectedB) {
            a.set(curA)
            return false
        }
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

private val LOCKED = "Locked"
