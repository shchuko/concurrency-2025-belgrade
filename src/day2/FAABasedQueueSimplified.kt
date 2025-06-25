package day2

import day1.*
import java.util.concurrent.atomic.*
import kotlin.math.*

class FAABasedQueueSimplified<E> : Queue<E> {
    private val infiniteArray = AtomicReferenceArray<Any?>(1024) // conceptually infinite array
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    override fun enqueue(element: E) {
        while (true) {
            val index = enqIdx.getAndIncrement()
            if (infiniteArray.compareAndSet(index.toInt(), null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (shouldNotTryDeque()) {
                return null
            }
            val index = deqIdx.getAndIncrement()
            if (!infiniteArray.compareAndSet(index.toInt(), null, POISONED)) {
                val element = infiniteArray.get(index.toInt()) as E
                infiniteArray.set(index.toInt(), null)
                return element
            }
        }
    }

    private fun shouldNotTryDeque(): Boolean {
        while (true) {
            val enqSnapshot = enqIdx.get()
            val deqSnapshot = deqIdx.get()
            val enqSnapshotAfter = enqIdx.get()

            if (enqSnapshot == enqSnapshotAfter) {
                return enqSnapshot <= deqSnapshot
            }
        }
    }

    override fun validate() {
        for (i in 0 until min(deqIdx.get().toInt(), enqIdx.get().toInt())) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `deqIdx = ${deqIdx.get()}` at the end of the execution"
            }
        }
        for (i in max(deqIdx.get().toInt(), enqIdx.get().toInt()) until infiniteArray.length()) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `enqIdx = ${enqIdx.get()}` at the end of the execution"
            }
        }
    }
}

private val POISONED = Any()
