@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.DoubleCompareSingleSetOnDescriptor.Status.*
import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class DoubleCompareSingleSetOnDescriptor<E : Any>(initialValue: E) : DoubleCompareSingleSet<E> {
    private val a = AtomicReference<Any>()
    private val b = AtomicReference<E>()

    init {
        a.set(initialValue)
        b.set(initialValue)
    }

    override fun getA(): E {
        while (true) {
            when (val curA = a.get()) {
                is DoubleCompareSingleSetOnDescriptor<*>.DcssDescriptor -> curA.doLogicalAndPhysicalApply()
                else -> return curA as E
            }
        }
    }

    override fun dcss(expectedA: E, updateA: E, expectedB: E): Boolean {
        val descriptor = DcssDescriptor(expectedA, updateA, expectedB)
        descriptor.apply()
        return descriptor.status.get() == SUCCESS
    }

    private inner class DcssDescriptor(
        val expectedA: E, val updateA: E, val expectedB: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            while (true) {
                when (val curA = a.get()) {
                    // help another thread to update the status
                    is DoubleCompareSingleSetOnDescriptor<*>.DcssDescriptor -> {
                        curA.doLogicalAndPhysicalApply()
                    }

                    expectedA -> {
                        if (!a.compareAndSet(curA, this)) {
                            continue
                        }

                        doLogicalAndPhysicalApply()
                        return
                    }

                    else -> {
                        status.set(FAILED)
                        check(status.get() == FAILED)
                        return
                    }
                }
            }
        }

        fun doLogicalAndPhysicalApply() {
            val newStatus = if (b.get() == expectedB) SUCCESS else FAILED
            status.compareAndSet(UNDECIDED, newStatus)

            val finalStatus = status.get()
            check(finalStatus != UNDECIDED)

            val newA = if (finalStatus == SUCCESS) updateA else expectedA
            a.compareAndSet(this, newA)
        }
    }

    override fun setB(value: E) {
        b.set(value)
    }

    override fun getB(): E {
        return b.get()
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}