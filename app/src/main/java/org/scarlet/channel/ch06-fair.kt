package org.scarlet.channel

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.scarlet.util.log

object Channels_Fair_PingPong {

    data class Ball(var hits: Int)

    private suspend fun player(name: String, table: Channel<Ball>) {
        for (ball in table) { // receive the ball in a loop
            ball.hits++
            log("$name $ball")
            delay(300) // wait a bit
            table.send(ball) // send the ball back
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        val table = Channel<Ball>() // a shared table

        launch { player("Harry(\uD83D\uDC66):", table) }
        launch { player("Sally(\uD83D\uDC67):", table) }

        table.send(Ball(0)) // serve the ball

        delay(2000)
        coroutineContext.cancelChildren() // game over, cancel them
    }

}
