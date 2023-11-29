package org.scarlet.flows.hot

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.scarlet.util.*
import kotlin.random.Random

/**
 * ## stateIn Demo
 *
 * The `stateIn` operator is useful in situations when there is a cold flow that provides
 * updates to the value of some state and is expensive to create and/or to maintain, but
 * there are multiple subscribers that need to collect the most recent state value.
 */

@DelicateCoroutinesApi
object stateIn_Demo {

    // Cold flow
    private val countingFlow: Flow<Int> = flow {
        currentCoroutineContext().job.onCompletion("countingFlow")

        repeat(10) {
            log("Emitting: $it")
            emit(it)
            log("Emitting: $it done")
            delay(100)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        val stateFlow = countingFlow.stateIn(
            scope = this,
            started = SharingStarted.Lazily,
            initialValue = null
        )

        // Subscriber 1
        val subscriber1 = launch {
            stateFlow.collect { value ->
                log("${spaces(4)}Subscriber1: $value")
            }
        }.onCompletion("Subscriber1")

        // Another later subscriber 500ms later
        val subscriber2 = launch {
            delay(500)
            stateFlow.collect { value ->
                log("${spaces(8)}Subscriber2: $value")
            }
        }.onCompletion("Subscriber2")

        delay(2_000)
        log("Cancelling all subscribers ...")
        subscriber1.cancelAndJoin()
        subscriber2.cancelAndJoin()
    }
}

/**
 * ### Suspending version of `stateIn`:
 * ```
 * suspend fun <T> Flow<T>.stateIn(scope: CoroutineScope): StateFlow<T>
 * ```
 *
 * Always need a value, so wait until the first value (i.e., initial value) is available.
 * When execution happens in suspending context and you want to compute and wait for the
 * initial value of the state to arrive from the upstream flow, there is a suspending
 * variant of `stateIn` without initial value and with the hard-coded `sharingStarted = Eagerly`.
 */

object Suspendingfunction_StateIn {

    // Cold flow
    private val greetingFlow = flow {
        log("Cold flow started")
        val seed = Random.nextInt()
        emit("Yellow $seed")
        log("Yellow")
        emit("Mellow $seed")
        log("Mellow")
    }.onEach { delay(3_000) }

    private suspend fun coldFlowDemo() = coroutineScope {
        launch {
            log("${spaces(4)}Subscriber1 launched")
            greetingFlow.collect { greeting ->
                log("${spaces(4)}subscriber1 - 1: $greeting")
            }
        }
        launch {
            log("${spaces(8)}Subscriber2 launched")
            greetingFlow.collect { greeting ->
                log("${spaces(8)}subscriber2 - 2: $greeting")
            }
        }
    }

    private suspend fun hotFlowDemo() = coroutineScope {
        // Note that this `stateIn` is a suspending function
        // 첫번째 data 가 emit 되면 coroutine resume 된다.
        // 그래서 initial value 가 필요 없다.
        val greetingState = greetingFlow.stateIn(this)

        val subscriber1 = launch {
            log("${spaces(4)}Subscriber1 launched")
            greetingState.collect { greeting ->
                log("${spaces(4)}subscriber1 - 1: $greeting")
            }
        }
        val subscriber2 = launch {
            log("${spaces(8)}Subscriber2 launched")
            greetingState.collect { greeting ->
                log("${spaces(8)}subscriber2 - 2: $greeting")
            }
        }

        delay(5_000)
        subscriber1.cancelAndJoin()
        subscriber2.cancelAndJoin()
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        coldFlowDemo()

        delim()

        hotFlowDemo()
    }
}

object stateIn_ColdToHot_Eagerly_vs_Lazily {

    private val coldFlow: Flow<Resource<Int>> = flow {
        for (i in 0..4) {
            log("Emitting: $i")
            emit(Resource.Success(i))
            log("Emitting: $i done")
            delay(1_000)
        }
    }

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {

        val stateFlow: StateFlow<Resource<Int>> = coldFlow.stateIn(
            this,
            SharingStarted.Eagerly, // Lazily
            Resource.Empty
        )

        // Subscriber 1
        val subscriber1 = launch {
            delay(2_000) // start after stateflow initialize
            log("${spaces(4)}Subscriber1 subscribes ...")
            stateFlow.collect {
                log("${spaces(4)}Subscriber1: $it")
                delay(1_500)
            }
        }.onCompletion("Subscriber1")

        // Subscriber 2
        val subscriber2 = launch {
            delay(3_500)
            log("${spaces(8)}Subscriber2 subscribes ...")
            stateFlow.collect {
                log("${spaces(8)}Subscriber2: $it")
            }
        }.onCompletion("Subscriber2")

        delay(7_000)
        subscriber1.cancelAndJoin()
        subscriber2.cancelAndJoin()
    }
}

// Note that program doesn't terminate!!
@DelicateCoroutinesApi
object WhileSubscribed_Demo {

    private val coldFlow: Flow<Int> = flow {
        currentCoroutineContext().job.onCompletion("coldFlow")

        log("Flow started ...")
        repeat(10) {
            log("Emitting: $it")
            emit(it)
            log("Emit: $it done")
            delay(500)
        }
        log("Flow completed ...")
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        log("Program started ...")

        val stateFlow: StateFlow<Int?> = coldFlow.stateIn(
            scope = this,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 3_000),
            initialValue = null
        )

        // First subscriber
        val subscriber1 = launch {
            delay(3_000)
            log("${spaces(4)}Subscriber1 subscribes ...")
            stateFlow.collect { value ->
                log("${spaces(4)}Subscriber1: $value")
            }
        }.onCompletion("Subscriber1")

        delay(5_000)
        log("Cancel subscriber1 ...")
        subscriber1.cancelAndJoin()

        delim()

        // Second subscriber joined later
        delay(1_500) // Change 500, 1500 to see the effect of `replayExpirationMillis`
        val subscriber2 = launch {
            log("${spaces(8)}Subscriber2 subscribes ...")
            stateFlow.collect { value ->
                log("${spaces(8)}Subscriber2: $value")
            }
        }.onCompletion("Subscriber2")

        delay(6_000)
        log("Cancel subscriber2 ...")
        subscriber2.cancelAndJoin()

        log(coroutineContext.job.children.toList())  // Check to see who is the culprit.
        coroutineContext.job.cancelChildren()
    }
}

