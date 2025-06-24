package day1

import java.util.concurrent.atomic.*
import kotlin.coroutines.*

// Never stores `null`-s for simplicity.
class RendezvousChannel<E : Any> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node(null, null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    suspend fun send(element: E) {
        while (true) {
            val curTail = tail.get()
            val curHead = head.get()

            // Is this queue empty or contain other senders?
            if (isEmptyOrContainsSenders()) {
                val success = suspendCoroutine<Boolean> { continuation ->
                    val node = Node(element, continuation as Continuation<Any?>)
                    if (!tryAddNode(curTail, node)) {
                        // Fail and retry.
                        continuation.resume(false)
                    }
                }
                // Finish on success and retry on failure.
                if (success) return
            } else {
                // The queue contains receivers, try to extract the first one.
                val firstReceiver = tryExtractNode(curHead) ?: continue
                check(firstReceiver.element == RECEIVER) { "Expected RECEIVER, got SENDER" }
                firstReceiver.continuation!!.resume(element)
                return
            }
        }
    }

    suspend fun receive(): E {
        while (true) {
            val curTail = tail.get()
            val curHead = head.get()

            // Is this queue empty or contain other receivers?
            if (isEmptyOrContainsReceivers()) {
                val element = suspendCoroutine<E?> { continuation ->
                    val node = Node(RECEIVER, continuation as Continuation<Any?>)
                    if (!tryAddNode(curTail, node)) {
                        // Fail and retry.
                        continuation.resume(null)
                    }
                }
                // Should we retry?
                if (element == null) continue
                // Return the element
                return element
            } else {
                // The queue contains senders, try to extract the first one.
                val firstSender = tryExtractNode(curHead) ?: continue
                firstSender.continuation!!.resume(true)
                check(firstSender.element != RECEIVER) { "Expected SENDER, got RECEIVER" }
                return firstSender.element as E
            }
        }
    }

    private fun isEmptyOrContainsReceivers(): Boolean {
        // For receivers, Node.element === RECEIVER
        val firstNode = head.get().next
        val firstElement = firstNode.get()?.element
        return firstElement == null || firstElement === RECEIVER
    }

    private fun isEmptyOrContainsSenders(): Boolean {
        // For senders, Node.element !== RECEIVER
        val firstNode = head.get().next
        val firstElement = firstNode.get()?.element
        return firstElement !== RECEIVER
    }

    private fun tryAddNode(curTail: Node, newNode: Node): Boolean {
        if (curTail.next.compareAndSet(null, newNode)) {
            tail.compareAndSet(curTail, newNode)
            return true
        }
        tail.compareAndSet(curTail, curTail.next.get())
        return false
    }

    private fun tryExtractNode(curHead: Node): Node? {
        val nextNode = curHead.next.get() ?: return null
        if (head.compareAndSet(curHead, nextNode)) {
            return nextNode
        }
        return null
    }

    class Node(
        // Sending element in case of suspended `send()` or
        // RECEIVER in case of suspended `receive()`.
        val element: Any?,
        // Suspended `send` of `receive` request.
        val continuation: Continuation<Any?>?
    ) {
        val next = AtomicReference<Node?>(null)
    }
}

private val RECEIVER = "Receiver"