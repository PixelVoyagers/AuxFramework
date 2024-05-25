package pixel.auxframework.web.util

import arrow.core.Some
import io.ktor.server.routing.*
import pixel.auxframework.component.annotation.Component
import pixel.auxframework.web.server.RequestMapper
import pixel.auxframework.web.server.RestRequestMappingEntry
import kotlin.reflect.KParameter


class AuxWebRequest(private val entry: RestRequestMappingEntry,
                    private val controllerRouter: Route,
                    private val mappingRoute: Route,
                    private val routingContext: RoutingContext
) {
    fun queryParameter(name: String) = routingContext.call.queryParameters[name]
    fun queryParameter(name: String, default: () -> String) = queryParameter(name) ?: default()
    fun pathParameter(name: String) = routingContext.call.pathParameters[name]
    fun pathParameter(name: String, default: () -> String) = pathParameter(name) ?: default()
    fun parameter(name: String) = routingContext.call.parameters[name]
    fun parameter(name: String, default: () -> String) = parameter(name) ?: default()
}

@Component
class AuxWebRequestRequestMapper : RequestMapper<AuxWebRequest> {

    override fun mapRequest(
        parameter: KParameter,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ) = Some(AuxWebRequest(entry, controllerRouter, mappingRoute, routingContext))

}
