@file:OptIn(ExperimentalCoroutinesApi::class)

package day1

import TestBase
import kotlinx.coroutines.*
import org.jetbrains.lincheck.datastructures.*

class RendezvousChannelTest : TestBase(
    sequentialSpecification = SequentialChannelInt::class,
    checkObstructionFreedom = true
) {
    val channel = RendezvousChannel<Int>()

    @Operation(cancellableOnSuspension = false)
    suspend fun send(element: Int) = channel.send(element)

    @Operation(cancellableOnSuspension = false)
    suspend fun receive() = channel.receive()
}

class SequentialChannelInt() {
    private val senders = ArrayList<Pair<CancellableContinuation<Unit>, Int>>()
    private val receivers = ArrayList<CancellableContinuation<Int>>()

    suspend fun send(element: Int) {
        if (tryResumeFirstReceiver(element)) return
        suspendCancellableCoroutine { cont ->
            senders.add(cont to element)
        }
    }

    private fun tryResumeFirstReceiver(element: Int): Boolean {
        if (receivers.isNotEmpty()) {
            val r = receivers.removeFirst()
            r.resume(value = element, onCancellation = null)
            return true
        } else {
            return false
        }
    }

    suspend fun receive(): Int {
        tryResumeFirstSender()?.let { return it }
        return suspendCancellableCoroutine { cont ->
            receivers.add(cont)
        }
    }

    private fun tryResumeFirstSender(): Int? {
        if (senders.isNotEmpty()) {
            val (sender, element) = senders.removeAt(0)
            sender.resume(value = Unit, onCancellation = null)
            return element
        } else {
            return null
        }
    }
}