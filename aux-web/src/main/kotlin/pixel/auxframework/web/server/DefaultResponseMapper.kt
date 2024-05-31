package pixel.auxframework.web.server

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pixel.auxframework.component.annotation.Component
import java.io.File
import java.io.InputStream
import kotlin.reflect.full.createType

data class RawResponse(
    val rawResponse: Any,
    val typeInfo: TypeInfo = TypeInfo(
        rawResponse::class,
        rawResponse::class.java,
        rawResponse::class.createType()
    ),
    val statusCode: HttpStatusCode? = null
)

@Component
class DefaultResponseMapper : ResponseMapper {

    override suspend fun mapResponse(
        rawResponse: Any?,
        entry: RestRequestMappingEntry,
        controllerRouter: Route,
        mappingRoute: Route,
        routingContext: RoutingContext
    ): Pair<Any?, Boolean> = when (rawResponse) {
        is String -> routingContext.call.respondText(rawResponse) to false
        is ByteArray -> routingContext.call.respondBytes(rawResponse) to false
        is InputStream -> routingContext.call.respondBytes(
            withContext(Dispatchers.IO) {
                rawResponse.readAllBytes()
            }
        ) to false

        is File -> routingContext.call.respondFile(rawResponse) to false
        is RawResponse -> routingContext.call.respond(
            rawResponse.statusCode ?: HttpStatusCode.OK,
            rawResponse.rawResponse,
            rawResponse.typeInfo
        ) to false

        else -> rawResponse to true
    }

}