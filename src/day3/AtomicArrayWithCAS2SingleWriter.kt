@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.AtomicArrayWithCAS2SingleWriter.Status.*
import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class AtomicArrayWithCAS2SingleWriter<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E = when (val cur = array[index]) {
        is AtomicArrayWithCAS2SingleWriter<E>.CAS2Descriptor -> cur[index]
        else -> cur as E
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        val descriptor = CAS2Descriptor(
            index1 = index1, expected1 = expected1, update1 = update1,
            index2 = index2, expected2 = expected2, update2 = update2
        )
        descriptor.apply()
        return descriptor.status.get() === SUCCESS
    }

    inner class CAS2Descriptor(
        val index1: Int, val expected1: E, val update1: E,
        val index2: Int, val expected2: E, val update2: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            while (true) {
                when (val cur1 = array[index1] as E) {
                    is AtomicArrayWithCAS2SingleWriter<E>.CAS2Descriptor -> {
                        error("CAS2Descriptor is unexpected in the single writer mode")
                    }

                    else -> {
                        if (cur1 != expected1) {
                            status.set(FAILED)
                            return
                        }

                        if (array.compareAndSet(index1, expected1, this)) {
                            val cur2 = array[index2]
                            check(cur2 !is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor) { "CAS2Descriptor is unexpected in B in single writer mode" }

                            if (cur2 != expected2) {
                                status.set(FAILED)
                                array.compareAndSet(index1, this, expected1)
                                return
                            }

                            if (!array.compareAndSet(index2, expected2, this)) {
                                status.set(FAILED)
                                array.compareAndSet(index1, this, expected1)
                                return
                            }

                            status.compareAndSet(UNDECIDED, SUCCESS)
                            array.compareAndSet(index2, this, update2)
                            array.compareAndSet(index1, this, update1)
                            return
                        }
                    }
                }
            }
        }

        operator fun get(index: Int): E {
            val shouldTakeExpected = when (status.get()) {
                UNDECIDED, FAILED -> true
                SUCCESS -> false
            }

            return when (index) {
                index1 -> if (shouldTakeExpected) expected1 else update1
                index2 -> if (shouldTakeExpected) expected2 else update2
                else -> error("Index $index does not match any [$expected1, $expected2]")
            }
        }
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}