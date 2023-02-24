package no.nav.sokos.pdl.proxy.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.naisApi(alive: () -> Boolean, ready: () -> Boolean) {
    route("internal") {
        get("started") {
            call.respondText("Started")
        }
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

