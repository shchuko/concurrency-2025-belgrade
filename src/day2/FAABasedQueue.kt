package day2

import day1.*
import java.util.concurrent.atomic.*

class FAABasedQueue<E> : Queue<E> {
    private val headSegment: AtomicReference<Segment>
    private val tailSegment: AtomicReference<Segment>

    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    init {
        val zeroSegment = Segment(0)
        headSegment = AtomicReference(zeroSegment)
        tailSegment = AtomicReference(zeroSegment)
    }

    override fun enqueue(element: E) {
        while (true) {
            val segmentSearchStart = tailSegment.get()
            val index = enqIdx.getAndIncrement()

            val segmentId = index / SEGMENT_SIZE
            val segment = findOrCreateSegment(segmentSearchStart, segmentId)
            moveTailForward(segment)

            val indexInSegment = (index % SEGMENT_SIZE).toInt()
            if (segment.compareAndSet(indexInSegment, null, element)) {
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

            val segmentSearchStart = headSegment.get()
            val index = deqIdx.getAndIncrement()

            val segmentId = index / SEGMENT_SIZE
            val segment = findOrCreateSegment(segmentSearchStart, segmentId)
            moveHeadForward(segment)

            val indexInSegment = (index % SEGMENT_SIZE).toInt()
            if (!segment.compareAndSet(indexInSegment, null, POISONED)) {
                val element = segment.get(indexInSegment) as E
                segment.set(indexInSegment, null)
                return element
            }
        }
    }

    private fun findOrCreateSegment(startFrom: Segment, id: Long): Segment {
        require(startFrom.id <= id) { "Cannot start search for segment=#$id as startFrom=#${startFrom.id}" }
        var it = startFrom
        while (true) {
            // exit if the required segment is found
            if (it.id == id) {
                return it
            }

            val nextSegment = it.next.get()
            if (nextSegment != null) {
                // go to the next segment if it exists
                it = nextSegment
            } else {
                // if not exists, try creating it and inserting into the list
                val newSegment = Segment(it.id + 1)
                if (it.next.compareAndSet(null, newSegment)) {
                    if (newSegment.id == id) {
                        return newSegment
                    }

                    it = newSegment
                }
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

    private fun moveHeadForward(segment: Segment) {
        advanceSegmentReference(headSegment, segment)
    }

    private fun moveTailForward(segment: Segment) {
        advanceSegmentReference(tailSegment, segment)
    }

    private fun advanceSegmentReference(pointer: AtomicReference<Segment>, proposedSegment: Segment) {
        val currentSegment = pointer.get()
        if (currentSegment.id < proposedSegment.id) {
            pointer.compareAndSet(currentSegment, proposedSegment)
        }
    }
}

private class Segment(val id: Long) : AtomicReferenceArray<Any?>(SEGMENT_SIZE) {
    val next = AtomicReference<Segment?>(null)
}

private val POISONED = Any()

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2
