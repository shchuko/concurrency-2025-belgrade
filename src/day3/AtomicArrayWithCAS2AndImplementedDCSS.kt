@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.AtomicArrayWithCAS2AndImplementedDCSS.Status.*
import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class AtomicArrayWithCAS2AndImplementedDCSS<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E = when (val cur = array[index]) {
        is AtomicArrayWithCAS2AndImplementedDCSS<E>.CAS2Descriptor -> cur[index]
        else -> cur as E
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        val descriptor = if (index1 < index2) {
            CAS2Descriptor(
                index1 = index1, expected1 = expected1, update1 = update1,
                index2 = index2, expected2 = expected2, update2 = update2
            )
        } else {
            CAS2Descriptor(
                index1 = index2, expected1 = expected2, update1 = update2,
                index2 = index1, expected2 = expected1, update2 = update1
            )
        }
        descriptor.apply()
        return descriptor.status.get() === SUCCESS
    }

    inner class CAS2Descriptor(
        val index1: Int,
        val expected1: E,
        val update1: E,
        val index2: Int,
        val expected2: E,
        val update2: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            while (true) {
                when (val value1 = array[index1]) {
                    is AtomicArrayWithCAS2AndImplementedDCSS<E>.CAS2Descriptor -> {
                        value1.installIntoIndex2()
                        value1.doLogicalAndPhysicalApply()
                    }

                    else -> {
                        if (value1 != expected1) {
                            status.set(FAILED)
                            return
                        }

                        if (!dcss(index1, expected1, this, status, UNDECIDED)) {
                            continue
                        }

                        installIntoIndex2()
                        doLogicalAndPhysicalApply()
                        return
                    }
                }
            }
        }

        private fun installIntoIndex2() {
            while (true) {
                when (val value2 = array[index2]) {
                    is AtomicArrayWithCAS2AndImplementedDCSS<E>.CAS2Descriptor -> {
                        // already installed
                        if (value2 === this) {
                            return
                        }

                        value2.installIntoIndex2()
                        value2.doLogicalAndPhysicalApply()
                    }

                    else -> {
                        if (value2 != expected2) {
                            status.compareAndSet(UNDECIDED, FAILED)
                            return
                        }

                        if (status.get() != UNDECIDED) {
                            return
                        }

                        // install yourself and exit on success
                        if (dcss(index2, expected2, this, status, UNDECIDED)) {
                            return
                        }
                    }
                }
            }
        }

        private fun doLogicalAndPhysicalApply() {
            status.compareAndSet(UNDECIDED, SUCCESS)

            val status = status.get()
            check(status != UNDECIDED)

            val desiredValue2 = if (status == SUCCESS) update2 else expected2
            val desiredValue1 = if (status == SUCCESS) update1 else expected1

            array.compareAndSet(index2, this, desiredValue2)
            array.compareAndSet(index1, this, desiredValue1)
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

    // TODO: Please use this DCSS implementation to ensure that
    // TODO: the status is `UNDECIDED` when installing the descriptor.
    fun dcss(
        index: Int,
        expectedCellState: Any?,
        updateCellState: Any?,
        statusReference: AtomicReference<*>,
        expectedStatus: Any?
    ): Boolean =
        if (array[index] == expectedCellState && statusReference.get() == expectedStatus) {
            array[index] = updateCellState
            true
        } else {
            false
        }
}