package org.scarlet.flows.hot

import org.scarlet.util.Resource
import org.scarlet.util.spaces
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.scarlet.util.log
import org.scarlet.util.onCompletion

/**
 * ## StateFlow:
 *  - conflation
 *
 *  Not suitable for tracing locations (due to conflation) and event processing (due to uniqueness)
 *  Slow subscribers may miss intermediate values.
 */
// 반드시 초기값이 필요하다. ShareFlow는 필요 없다.
// 중복 데이터는 처리 안한다.
object StateFlow_and_Conflation {

    // State flow must have an initial value.
    private val stateFlow = MutableStateFlow<Resource<Int>>(Resource.Empty)

    /**
     * Check conflation behavior using slow vs. fast collectors.
     */
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {

        // Subscriber
        val subscriber = launch {
//            delay(450) // 1. Uncomment to check to see whether initial value collected or not
            log("${spaces(4)}Subscriber: subscribe to stateflow")
            stateFlow.collect {
                log("${spaces(4)}Subscriber: $it")
                delay(100) // 2. change to 100 (fast subscriber), 400 (slow subscriber)
            }
        }.onCompletion("Subscriber")

        // Publisher
        launch {
            log("Publisher: started")
            for (i in 0..4) {
                log("Emit $i")
                stateFlow.value = Resource.Success(i)
                // race condition이 걱정이 되면 아래처럼 update를 사용한다.
//                stateFlow.update {
//
//                }
                delay(200)
            }
        }.onCompletion("Publisher")

        delay(2_000)
        subscriber.cancelAndJoin()
    }
}

/**
 *  Check whether initial value delivered to late collector.
 */
// Hot flow 라서 subscriber 에 상관없이 emit 한다. 그래서 subscribe에서 전에 다 못받는 경우가 생긴다.
// 등록된 이후에 녀석은 다 받는다.
object StateFlow_Late_Collector {

    private val stateFlow = MutableStateFlow<Resource<Int>>(Resource.Empty)

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {

        // Publisher
        launch {
            for (i in 0..4) {
                stateFlow.value = Resource.Success(i)
                log("Emit $i (#subscribers = ${stateFlow.subscriptionCount.value})")
                delay(100)
            }
        }.onCompletion("Publisher")

        // A late subscriber
        val subscriber = launch {
            // To delay subscriber subscription
            delay(300)
            log("${spaces(4)}Subscriber: subscribe to stateflow")
            stateFlow.collect {
                log("${spaces(4)}subscriber: $it")
            }
        }.onCompletion("Subscriber")

        delay(1_000)
        subscriber.cancelAndJoin()
    }
}

object StateFlow_Multiple_Subscribers {
    // 이녀석은 buffer 가 1 인 녀석이다.
    // buffer 의 값을 항상 overwrite 하게 된다.
    // 그래서 livedata와 동작이 비슷하다.
    // Subscriber 의 상태에 따라 현재 가지고 있는 값이 다를 수 있다.
    private val stateFlow = MutableStateFlow<Resource<Int>>(Resource.Empty)

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {

        // Subscriber 1
        val subscriber1 = launch {
            log("${spaces(4)}Subscriber1 subscribes")
            stateFlow.collect {
                log("${spaces(4)}Subscriber1: $it")
                delay(100)
            }
        }.onCompletion("Subscriber1")

        // Subscriber 2
        val subscriber2 = launch {
            delay(250)
            log("${spaces(12)}Subscriber2 subscribes")
            stateFlow.collect {
                log("${spaces(12)}subscriber2: $it")
                delay(400) // Change 100, 400.
            }
        }.onCompletion("Subscriber2")

        // Publisher
        val publisher = launch {
            for (i in 0..4) {
                log("Emitter: $i")
                stateFlow.value = Resource.Success(i)
                delay(200)
            }
        }.onCompletion("Publisher")

        delay(2_000)
        log("Cancelling children")
        coroutineContext.job.cancelChildren()
        joinAll(subscriber1, subscriber2, publisher)
    }
}

object StateFlow_Squash_Duplication {
    // SharedFlow와는 다르다.
    private val stateFlow = MutableStateFlow<Resource<Int>>(Resource.Empty)

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        // Subscriber
        val subscriber = launch {
            log("${spaces(4)}Subscriber: subscribe to stateflow")
            stateFlow.collect {
                log("${spaces(4)}subscriber: $it")
            }
        }

        // Publisher
        launch {
            for (i in listOf(1, 1, 2, 2, 3, 3, 3)) {
                log("Emit $i")
                stateFlow.value = Resource.Success(i)
                delay(200)
            }
        }

        delay(2_000)
        subscriber.cancelAndJoin()
    }
}



