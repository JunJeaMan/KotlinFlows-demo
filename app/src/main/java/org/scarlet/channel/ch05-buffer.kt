package org.scarlet.channel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.scarlet.util.log
import org.scarlet.util.onClose
import org.scarlet.util.onCompletion

object Buffering {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        // create buffered channel
        val channel = Channel<Int>(4)

        val sender = launch { // launch sender coroutine
            repeat(10) {
                log("Sending $it")
                channel.send(it) // will suspend when buffer is full
                log("$it sent")
            }
        }.onCompletion("Sender")

        // don't receive anything... just wait....
        delay(1000)
        sender.cancel() // cancel sender coroutine
    }
}

@ExperimentalCoroutinesApi
object Closing_SendChannel_Guarantees_All_Sent_Data_Received {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        // create buffered channel
        val channel = Channel<Int>(4).onClose()

        launch { // launch sender coroutine
            repeat(10) {
                log("Sending $it")
                channel.send(it) // will suspend when buffer is full
                log("$it sent")
            }
            log("Closing channel ...")
            channel.close()
        }

        for (value in channel) {
            log("Receiver: $value received")
            delay(500)
        }

    }
}