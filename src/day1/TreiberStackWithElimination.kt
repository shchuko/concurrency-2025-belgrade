package day1

import java.util.concurrent.*
import java.util.concurrent.atomic.*

open class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()

    private val eliminationArray = AtomicReferenceArray<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }

    protected open fun tryPushElimination(element: E): Boolean {
        val index = randomCellIndex()
        if (!eliminationArray.compareAndSet(index, CELL_STATE_EMPTY, element)) {
            return false
        }

        repeat(ELIMINATION_WAIT_CYCLES) {
            if (eliminationArray.compareAndSet(index, CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) {
                return true
            }
        }

        // getAndSet via compareAndSet
        while (true) {
            val last = eliminationArray.get(index)
            if (eliminationArray.compareAndSet(index, last, CELL_STATE_EMPTY)) {
                // It may happen that another thread has popped the value while we were resetting the state to EMPTY
                // In this case we need to return 'true' answering that elimination successfully worked
                return last == CELL_STATE_RETRIEVED
            }
        }
    }

    override fun pop(): E? = tryPopElimination() ?: stack.pop()

    private fun tryPopElimination(): E? {
        val index = randomCellIndex()
        val value = eliminationArray.get(index)

        if (value == CELL_STATE_RETRIEVED || value == CELL_STATE_EMPTY) {
            return null
        }

        if (eliminationArray.compareAndSet(index, value, CELL_STATE_RETRIEVED)) {
            @Suppress("UNCHECKED_CAST")
            return value as E
        }
        return null
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.length())

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state
        // to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}