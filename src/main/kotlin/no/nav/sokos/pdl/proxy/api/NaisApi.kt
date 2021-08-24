package no.nav.sokos.ereg.proxy.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing

fun Application.naisApi(alive: () -> Boolean, ready: () -> Boolean) {
    routing {
        route("internal") {
            get("is_alive") {
                when (alive()) {
                    true -> call.respondText { "Application is alive" }
                    else -> call.respondText(
                        text = "Application is not alive",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            get("is_ready") {
                when (ready()) {
                    true -> call.respondText { "Application is ready" }
                    else -> call.respondText(
                        text = "Application is not ready",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
}

