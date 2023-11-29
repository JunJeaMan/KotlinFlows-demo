package org.scarlet.flows.hot

import com.google.common.truth.Truth.assertThat
import org.scarlet.flows.basics.DataSource.genToken
import org.scarlet.flows.basics.DataSource.tokens
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.scarlet.util.log
import org.scarlet.util.onCompletion

class StateFlow_NativeTest {

    @Test
    fun `StateFlow conflates test`() = runTest {
        val stateFlow = MutableStateFlow(0)

        // Publisher
        launch {
            repeat(5) {
                delay(200) // change this 200, 1000
                stateFlow.value = it + 1
            }
        }.onCompletion("Publisher")

        // Subscriber
        val collector = launch {
            stateFlow.collect {
                log("received value = $it")
                delay(500)
            }
        }.onCompletion("Subscriber")

        delay(3_000)
        collector.cancelAndJoin()
    }

    @Test
    fun `stateFlow never completes`() = runTest {
        val stateFlow = MutableStateFlow(0)

        // Publisher
        launch {
            repeat(5) {
                delay(200)
                stateFlow.value = it + 1
            }
        }.onCompletion("Publisher")

        stateFlow
            .onCompletion { ex -> log("ON COMPLETE: ${ex?.javaClass?.name}") }
            .collect {
                log("received = $it")
            }
    }

    @Test
    fun `StateFlow - lost initial state`() = runTest {
        val hotFlow = MutableStateFlow(42)

        hotFlow.emit(1)

        val value = hotFlow.first()
        assertThat(value).isEqualTo(1)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `StateFlow - init value collected`() = runTest {
        val hotFlow = MutableStateFlow(42)

        val list = mutableListOf<Int>()
        val job = launch {
            hotFlow.collect {
                list.add(it)
            }
        }
//        runCurrent() // or use `UnconfinedDispatcher`

        hotFlow.emit(1)

        delay(100)

        job.cancelAndJoin()
        assertThat(list).isEqualTo(listOf(42, 1))
    }

    @Test
    fun `StateFlow - realistic test`() = runTest {
        val publisher = launch {
            genToken() // infinite flow
        }.onCompletion("Publisher")

        val subscriber = launch {
            tokens.collect {
                log("received value = $it")
            }
        }.onCompletion("Subscriber")

        delay(1_000)

        subscriber.cancelAndJoin()
        publisher.cancelAndJoin()
    }

    @Test
    fun `suspending function version of stateIn`() = runBlocking {
        val payload = 0
        val given: StateFlow<Int> = flow {
            log("started ...")
            emit(payload)
            delay(1_000) // what if to move this line one up?
            emit(payload + 1)
            log("finished ...")
        }.stateIn(scope = this)

        val subscriber = launch {
            log("collecting started ...")

            given.collect {
                log("received value = $it")
            }
        }.onCompletion("Subscriber")

        delay(3_000)
        subscriber.cancelAndJoin()
    }

    /**/

    @Test
    fun `stateIn - Early vs Lazily`() = runTest {
        val payload = 0
        val given: StateFlow<Int?> = flow {
            log("started ...")
            emit(payload)
        }.stateIn(
            scope = this, // started를 WhileSubscribed 로 할때 test
            // What if using Early or WhileSubscribed?
            started = SharingStarted.Eagerly,
            initialValue = null
        )

        val result = mutableListOf<Int?>()
        val job = launch {
            log("collecting started ...")
            given.collect {
                result.add(it)
                log("received value = $it")
            }
        }

        delay(1_000)
        job.cancelAndJoin()

        log("result = $result")
    }

}