package day2

import java.util.concurrent.atomic.*

class CountersSnapshot {
    val counter1 = AtomicLong(0)
    val counter2 = AtomicLong(0)
    val counter3 = AtomicLong(0)

    fun incrementCounter1() = counter1.getAndIncrement()
    fun incrementCounter2() = counter2.getAndIncrement()
    fun incrementCounter3() = counter3.getAndIncrement()

    fun countersSnapshot(): Triple<Long, Long, Long> {
        while (true) {
            val snapshot1 = counter1.get()
            val snapshot2 = counter2.get()
            val snapshot3 = counter3.get()

            val snapshot1After = counter1.get()
            val snapshot2After = counter2.get()
            if (snapshot1 == snapshot1After && snapshot2 == snapshot2After) {
                return Triple(snapshot1, snapshot2, snapshot3)
            }
        }
    }
}