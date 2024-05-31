package pixel.auxframework.web.server

import arrow.core.Option
import io.ktor.server.routing.*
import kotlin.reflect.KParameter

interface RequestMapper<T> {
    fun mapRequest(
        parameter: KParameter,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ): Option<T>
}