package no.nav.sokos.pdl.proxy.util

import kotlinx.coroutines.delay

suspend fun <T> retry (
    numOfRetries: Int = 5,
    initialDelayMs: Long = 250,
    block: suspend () -> T,
): T {

    var throwable: Exception? = null
    for (n in 1..numOfRetries) {
        try {
            return block()
        } catch (ex: Exception) {
            throwable = ex
            delay(initialDelayMs)
        }
    }
    throw throwable!!
}