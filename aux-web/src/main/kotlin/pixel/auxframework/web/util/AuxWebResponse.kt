package pixel.auxframework.web.util

import arrow.core.Some
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pixel.auxframework.component.annotation.Component
import pixel.auxframework.web.server.RequestMapper
import pixel.auxframework.web.server.RestRequestMappingEntry
import java.time.temporal.Temporal
import kotlin.reflect.KParameter

class AuxWebResponse(
    private val entry: RestRequestMappingEntry,
    private val controllerRouter: Route,
    private val mappingRoute: Route,
    private val routingContext: RoutingContext
) {

    var statusCode: HttpStatusCode?
        set(value) {
            routingContext.call.response.status(value ?: return)
        }
        get() = routingContext.call.response.status()

    fun header(name: String, value: String) = routingContext.call.response.header(name, value)
    fun header(name: String, value: Int) = routingContext.call.response.header(name, value)
    fun header(name: String, value: Long) = routingContext.call.response.header(name, value)
    fun header(name: String, value: Temporal) = routingContext.call.response.header(name, value)
    fun header(name: String) = routingContext.call.response.headers[name]
    fun header(name: String, default: () -> String) = routingContext.call.response.headers[name] ?: default()
    fun header() = routingContext.call.response.headers
    fun cookie() = routingContext.call.response.cookies

    suspend fun respondJson(code: HttpStatusCode = HttpStatusCode.OK, block: () -> Any?) =
        routingContext.call.respondBytes(
            status = code,
            contentType = ContentType.Application.Json,
            bytes = jacksonObjectMapper().writeValueAsBytes(block())
        )

}

@Component
class AuxWebResponseRequestMapper : RequestMapper<AuxWebResponse> {

    override fun mapRequest(
        parameter: KParameter,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ) = Some(AuxWebResponse(entry, controllerRouter, mappingRoute, routingContext))

}
