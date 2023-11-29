package org.scarlet.flows.advanced.a3context

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.scarlet.util.delim
import org.scarlet.util.log
import kotlin.coroutines.coroutineContext

suspend fun accept(block: suspend () -> Unit) {
    log("coroutineContext2 = $coroutineContext")
    log("currentCoroutineContext2 = ${currentCoroutineContext()}")

    delim()

    val scope = CoroutineScope(Job())
    scope.launch {
        log("coroutineContext3 = $coroutineContext")
        log("currentCoroutineContext3 = ${currentCoroutineContext()}")

        delim()

        block()
    }.join()
}

object WhichContext {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        delim()
        log("coroutineContext0 = $coroutineContext")
        log("currentCoroutineContext0 = ${currentCoroutineContext()}")

        launch {
            delim()
            log("coroutineContext1 = $coroutineContext")
            log("currentCoroutineContext1 = ${currentCoroutineContext()}")
            delim()

            //  여기는 왜 다를까요...
            accept {
                log("coroutineContext4 = $coroutineContext") // 정적으로 결정된다. 렉시컬하게
                log("currentCoroutineContext4 = ${currentCoroutineContext()}") // 실제 동작하는 context
                delim()
            }
            delay(1000)
        }.join()

        log("Done.")
    }
}
