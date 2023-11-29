package org.scarlet.flows.hot

import kotlinx.coroutines.*
import org.scarlet.util.Resource
import org.scarlet.util.spaces
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.scarlet.util.delim
import org.scarlet.util.log

/**
 * ## Make SharedFlow as StateFLow
 */
// StateFlow는 LiveData와 완전 비슷 하고 할 수 있는 일이 더 많다.
// 그래서 LiveData는 점 차 deprecated되고 있는 상황이다.
// 더 많은 위치(View, ViewModel 이외에도 사용가능하다.)

// 이녀석은 SharedFlow를 StateFlow처럼 사용하는 부분이다.
object SharedFlow_As_StateFlow {

    private val _stateFlow = MutableStateFlow<Resource<Int>>(Resource.Empty)
//    val stateFlow: StateFlow<Resource<Int>> = _stateFlow
    val stateFlow = _stateFlow.asStateFlow()

    // MutableStateFlow(initialValue) is a shared flow with the following parameters:
    // 이게 뭐여?
    private val _sharedFlow = MutableSharedFlow<Resource<Int>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply {
        tryEmit(Resource.Empty)
        distinctUntilChanged()
    }

    private val sharedFlow: SharedFlow<Resource<Int>> = _sharedFlow

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        stateFlow_case()

        delim()

        sharedFlow_case()
    }

    private suspend fun stateFlow_case() = coroutineScope {
        val subscriber1 = launch {
            log("Subscriber1 subscribes")
            stateFlow.collect {
                log("Subscriber1: $it")
                delay(200)
            }
        }

        val subscriber2 = launch {
            delay(1_000)
            log("${spaces(8)}Subscriber2 subscribes after 1000ms")
            stateFlow.collect {
                log("${spaces(8)}Subscriber2: $it")
                delay(200)
            }
        }

        // Publisher
        launch {
            for (i in 0..5) {
                log("Emitting $i")
                _stateFlow.value = Resource.Success(i)
                delay(200)
            }
        }

        delay(3_000)
        subscriber1.cancelAndJoin()
        subscriber2.cancelAndJoin()
    }

    private suspend fun sharedFlow_case() = coroutineScope {
        val subscriber1 = launch {
            log("Subscriber1 subscribes")
            sharedFlow.collect {
                log("Subscriber1: $it")
                delay(200)
            }
        }

        val subscriber2 = launch {
            delay(1_000)
            log("${spaces(8)}Subscriber2 subscribes after 1000ms")
            sharedFlow.collect {
                log("${spaces(8)}Subscriber2: $it")
                delay(200)
            }
        }

        // Publisher
        launch {
            for (i in 0..5) {
                log("Emitting $i")
                _sharedFlow.emit(Resource.Success(i))
                delay(200)
            }
        }

        delay(3_000)
        subscriber1.cancelAndJoin()
        subscriber2.cancelAndJoin()
    }
}