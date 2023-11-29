package org.scarlet.flows.advanced.a1composition

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.scarlet.util.delim
import org.scarlet.util.log

/**
 * Zip:
 *
 * Just like the `Sequence.zip` extension function in the Kotlin standard library,
 * flows have a `zip` operator that combines the corresponding values of two flows.
 * Shorter and slower flow determines when to zip, and terminate.
 */

val numbers = (1..3).asFlow()
val strings = flowOf("one", "two", "three")

object Zip_Operator1 {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        numbers.zip(strings) { a, b -> "$a -> $b" }
            .collect { log(it) }
    }
}

val numsDelay = numbers.onEach { delay(1_000) }
val strsDelay = strings.onEach { delay(800) }

object Zip_Operator2 {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        numsDelay.zip(strsDelay) { a, b -> "$a -> $b" }
            .collect { log(it) }
    }
}

/**
 * Combine:
 *
 * When flow represents the most recent value of a variable or operation,
 * it might be needed to perform a computation that depends on the most
 * recent values of the corresponding flows and to recompute it whenever
 * any of the upstream flows emit a value.
 * The corresponding family of operators is called `combine`.
 */
object Combine_Operator {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        numbers.combine(strings) { a, b -> "$a -> $b" }
            .collect { log(it) }

        delim()

        numsDelay.combine(strsDelay) { a, b -> "$a -> $b" }
            .collect { log(it) }
    }
}