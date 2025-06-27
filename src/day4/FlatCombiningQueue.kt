package day4

import day1.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

open class FlatCombiningQueue<E : Any> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    override fun enqueue(element: E): Unit = enqueueOrDequeue(task = element) {
        queue.add(element)
    }

    override fun dequeue(): E? = enqueueOrDequeue(task = Dequeue) {
        queue.removeFirstOrNull()
    }

    private inline fun <R> enqueueOrDequeue(task: Any, operation: () -> R): R {
        var occupiedCellIndex: Int? = null
        while (true) {
            // check if some other thread has helped us
            if (occupiedCellIndex != null) {
                val maybeResult = tryLoadResultFromArray(occupiedCellIndex, resetToNullOnNoResult = false)
                if (maybeResult != null) {
                    @Suppress("UNCHECKED_CAST") return maybeResult.value as R
                }
            }

            // try to acquire lock the lock
            if (tryAcquireLock()) {
                val result: R

                // maybe some other thread has helped us? - skip the [operation] then
                val resultFromArray = tryLoadResultFromArray(occupiedCellIndex, resetToNullOnNoResult = true)
                if (resultFromArray != null) {
                    @Suppress("UNCHECKED_CAST")
                    result = resultFromArray.value as R
                } else {
                    result = operation()
                }

                helpOthers()
                releaseLock()
                return result
            }

            // if lock acquisition failed, try to delegate the work to another thread
            if (occupiedCellIndex == null) {
                val maybeFreeCellIndex = randomCellIndex()
                if (tasksForCombiner.compareAndSet(maybeFreeCellIndex, null, task)) {
                    occupiedCellIndex = maybeFreeCellIndex
                }
            }
        }
    }

    private fun tryLoadResultFromArray(cellToCheck: Int?, resetToNullOnNoResult: Boolean): Result<*>? {
        if (cellToCheck == null) return null
        return when (val cellValue = tasksForCombiner.get(cellToCheck)) {
            is Result<*> -> {
                tasksForCombiner.set(cellToCheck, null)
                return cellValue
            }

            else -> {
                checkNotNull(cellValue) { "Unexpected null value at $cellToCheck" }
                if (resetToNullOnNoResult) {
                    tasksForCombiner.set(cellToCheck, null)
                }
                null
            }
        }
    }


    private fun helpOthers() {
        repeat(TASKS_FOR_COMBINER_SIZE) { index ->
            when (val task = tasksForCombiner.get(index)) {
                null, is Result<*> -> {
                    return@repeat
                }

                Dequeue -> {
                    val result = Result(queue.removeFirstOrNull())
                    tasksForCombiner.set(index, result)
                    return@repeat
                }

                else -> {
                    tasksForCombiner.set(index, Result(Unit))
                    @Suppress("UNCHECKED_CAST") queue.add(task as E)
                }
            }
        }
    }

    private fun tryAcquireLock(): Boolean = combinerLock.compareAndSet(false, true)

    open fun releaseLock() {
        combinerLock.set(false)
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.length())
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

private object Dequeue

private class Result<V>(
    val value: V
)
