package org.scarlet.flows.hot

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import org.scarlet.flows.basics.DataSource.genToken
import org.scarlet.flows.basics.DataSource.tokens
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.scarlet.util.log
import org.scarlet.util.onCompletion

class StateFlow_TurbineTest {

    @Test
    fun `StateFlow test`() = runTest {
        val stateFlow = MutableStateFlow(0)

        // Publisher
        launch {
            repeat(3) {
                stateFlow.value = it + 1
            }
        }

        stateFlow.test {
            repeat(4) {
                log("received value = ${awaitItem()}")
            }
        }
    }

    @Test
    fun `stateFlow never completes but - turbine behavior`() = runBlocking {
        val stateFlow = MutableStateFlow(0)

        stateFlow
            .onCompletion { ex -> log("ON COMPLETE: ${ex?.javaClass?.name}") }
            .test {
                log(awaitItem())
            }
    }

    @Test
    fun `StateFlow - lost initial state`() = runTest {
        val hotFlow = MutableStateFlow(42)

        hotFlow.emit(1)

        hotFlow.test {
            val value = awaitItem()
            assertThat(value).isEqualTo(1)
        }
    }

    @Test
    fun `StateFlow - init value collected`() = runTest {
        val hotFlow = MutableStateFlow(42)

        val list = mutableListOf<Int>()
        hotFlow.test {
            hotFlow.emit(1)

            list.add(awaitItem())
            list.add(awaitItem())
        }

        assertThat(list).isEqualTo(listOf(42, 1))
    }

    @Test
    fun `StateFlow - realistic test`() = runTest {
        // Arrange (Given)
        val publisher = launch {
            genToken() // infinite flow
        }.onCompletion("Publisher")

        // Act (When)
        tokens.test {
            // Assert (Then)
            repeat(5) {
                log(awaitItem())
            }
        }

        publisher.cancelAndJoin() // Must use this line.
    }

    @Test
    fun `suspending function version - stateIn`() = runTest {
        val payload = 0
        val given: StateFlow<Int> = flow {
            emit(payload)
            delay(1_000) // what if to move this line before first emit?
            emit(payload + 1)
        }.stateIn(scope = this)

        launch {
            given.test {
                log(awaitItem())
                log(awaitItem())
            }
        }
    }

    @Test
    fun `stateIn demo`() = runTest {
        val payload = 0
        val given: StateFlow<Int?> = flow {
            log("started ...")
            emit(payload)
        }.stateIn(
            scope = this,
            // What if using Early or WhileSubscribed?
            started = SharingStarted.Eagerly,
            initialValue = null
        )

        given.test {
            log("Testing started ...")
            log(awaitItem())
            log(awaitItem())
        }
    }

}