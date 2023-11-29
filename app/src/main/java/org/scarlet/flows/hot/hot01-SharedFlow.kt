package org.scarlet.flows.hot

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.scarlet.util.*

/**
 * ## SharedFlow Demo
 *
 * `SharedFlow` is like a **broadcast channel**: everyone can send (emit) messages which will be
 * received by every coroutine that is listening (collecting).
 *
 * **Shared flow waits for all subscribers to receive**. *Good fit for event handling*.
 */

object SharedFlow_behaves_like_a_broadcast_channel {

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // default configuration
        val mutableSharedFlow = MutableSharedFlow<String>(
            replay = 0,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.SUSPEND
        )

        // Two subscribers
        val subscriber1 = launch(CoroutineName("Subscriber 1")) {
            mutableSharedFlow.collect {
                log("subscriber1: received $it")
            }
        }.onCompletion("Subscriber1")
        val subscriber2 = launch(CoroutineName("Subscriber 2")) {
            mutableSharedFlow.collect {
                log("subscriber2: received $it")
            }
        }.onCompletion("Subscriber2")

        delay(100) // allow time for subscribers being ready

        // Publisher
        launch(CoroutineName("Publisher   ")) {
            log("Emit Data1")
            mutableSharedFlow.emit("Data1")
            delay(2_000)
            log("Emit Data2")
            mutableSharedFlow.emit("Data2")
        }.onCompletion("Publisher").join()

        // Clear subscribers and publisher
        coroutineContext.job.cancelChildren()
        joinAll(subscriber1, subscriber2)
    }
}

object SharedFlow_Single_Subscriber_Rendezvous {

    // default configuration
    private val sharedFlow = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        // Publisher
        launch {
            for (i in 0..4) {
                log("Emitting $i (#subscribers = ${sharedFlow.subscriptionCount.value})")
                sharedFlow.emit(i) // Values emitted before subscription are lost
                log("Emitting $i done")
                delay(500)
            }
        }.onCompletion("Publisher")

        // A subscriber subscribes after 1000ms later
        val subscriber = launch {
            delay(1_000)
            delim()
            log("${spaces(7)}Subscribe to emitter after 1000ms")
            sharedFlow.collect {
                log("${spaces(7)}Subscriber: $it")
                delay(3_000)
            }
        }.onCompletion("Subscriber")

        delay(10_000)
        subscriber.cancelAndJoin()
    }
}

object SharedFlow_Single_Subscriber_with_Buffers {

    // Change `extraBufferCapacity`
    private val sharedFlow = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 2,  // 0, 1, 2
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        // Publisher
        launch {
            for (i in 0..10) {
                log("Emitting $i (#subscribers = ${sharedFlow.subscriptionCount.value})")
                sharedFlow.emit(i)
                log("Emitting $i done")
                delay(50)
            }
        }

        // A slow subscriber
        val subscriber = launch {
            delay(150)
            delim()
            log("${spaces(7)}Subscribe to sharedFlow after 150ms")
            sharedFlow.collect {
                log("${spaces(7)}Subscriber: $it")
                delay(200)
            }
        }

        delay(3_000)
        subscriber.cancelAndJoin()
    }
}

/**
 * ### `MutableSharedFlow` is conceptually similar to RxJava `Subject`s.
 *  - When `replay = 0`, similar to a `PublishSubject`.
 *  - When `replay = 1`, similar to a `BehaviorSubject`.<br>
 *  - When `replay = Int.MAX_VALUE`, similar to `ReplaySubject`.
 */
@ExperimentalCoroutinesApi
object SharedFlow_replayCache {

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val mutableSharedFlow = MutableSharedFlow<Int>(
            replay = 1, // 0, 1, 2
            onBufferOverflow = BufferOverflow.SUSPEND // Change to DROP_OLDEST
        )

        // Subscriber
        val subscriber = launch {
            mutableSharedFlow.collect {
                log("subscriber: received $it")
                delay(3_000)
            }
        }

        // Publisher
        launch {
            repeat(4) {
                log("publisher: emitting $it")
                mutableSharedFlow.emit(it)
                log("publisher: emitting $it done")
                log("Replay cache = ${mutableSharedFlow.replayCache}")
                delay(100)
                delim()
            }
        }.join()

        delay(8_000)
        mutableSharedFlow.resetReplayCache()
        log("After reset: cache = ${mutableSharedFlow.replayCache}")

        subscriber.cancelAndJoin()
    }
}

object SharedFlow_Multiple_Subscribers {

    private val sharedFlow = MutableSharedFlow<Int>(
        replay = 2, // 0, 1, 2
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.SUSPEND
        // DROP_OLDEST does not guarantee receivers accept all equal # of values.
    )

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        // Publisher
        launch {
            for (i in 0..10) {
                log("Emitting $i (#subscribers = ${sharedFlow.subscriptionCount.value})")
                sharedFlow.emit(i)
                log("Emitting $i done")
                delay(50)
            }
        }

        /*
         * Subscribe two subscribers 300ms apart
         */
        val subscriber1 = launch {
            log("${spaces(4)}Subscriber1 subscribes")
            sharedFlow.collect {
                log("${spaces(4)}Subscriber1: $it")
                delay(100)
            }
        }

        val subscriber2 = launch {
            delay(300)
            log("${spaces(8)}Subscriber2 subscribes")
            sharedFlow.collect {
                log("${spaces(8)}Subscriber2: $it")
                delay(200)
            }
        }

        delay(3_000)
        coroutineContext.job.cancelChildren()
        joinAll(subscriber1, subscriber2)
    }
}

/**
 * SharedFlow emits duplicated values.
 */
object SharedFlow_Emits_Duplicated_Values {

    private val sharedFlow = MutableSharedFlow<Resource<Int>>()

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {

        // Subscriber
        val subscriber = launch {
            sharedFlow.collect {
                log("Subscriber: value = $it")
                delay(1_000)
            }
        }

        // Publisher
        launch {
            for (i in listOf(0, 1, 1, 2, 2, 3, 3)) {
                sharedFlow.emit(Resource.Success(i))
            }
        }.join()

        subscriber.cancelAndJoin()
    }
}

