package pixel.auxframework.web.server

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import io.ktor.server.routing.*
import pixel.auxframework.component.annotation.Component
import pixel.auxframework.web.annotation.PathVariable
import pixel.auxframework.web.annotation.QueryVariable
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

@Component
class DefaultRequestMapper1 : RequestMapper<RestRequestMappingEntry> {

    override fun mapRequest(
        parameter: KParameter,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ) = Some(entry)

}

@Component
class DefaultRequestMapper2 : RequestMapper<Route> {

    override fun mapRequest(
        parameter: KParameter,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ) = Some(mappingRoute)

}

@Component
class DefaultRequestMapper3 : RequestMapper<RoutingContext> {

    override fun mapRequest(
        parameter: KParameter,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ) = Some(routingContext)

}

@Component
class PathVariableRequestMapping : RequestMapper<String?> {

    override fun mapRequest(
        parameter: KParameter,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ): Option<String?> {
        val annotation = parameter.findAnnotation<PathVariable>() ?: return None
        val name = annotation.name.let { if (it == "<default>") parameter.name!! else it }
        return Some(routingContext.call.pathParameters[name])
    }

}

@Component
class QueryVariableRequestMapping : RequestMapper<String?> {

    override fun mapRequest(
        parameter: KParameter,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ): Option<String?> {
        val annotation = parameter.findAnnotation<QueryVariable>() ?: return None
        val name = annotation.name.let { if (it == "<default>") parameter.name!! else it }
        return Some(routingContext.call.queryParameters[name])
    }

}
