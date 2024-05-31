package pixel.auxframework.web.server

import io.ktor.server.routing.*

interface ResponseMapper {
    /**
     * @return 是否继续
     */
    suspend fun mapResponse(
        rawResponse: Any?,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ): Pair<Any?, Boolean>
}