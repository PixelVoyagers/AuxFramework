package pixel.auxframework.web.server

import arrow.core.Some
import io.ktor.http.*
import io.ktor.server.routing.*
import pixel.auxframework.component.annotation.Service
import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.ComponentFactory
import pixel.auxframework.component.factory.ComponentPostProcessor
import pixel.auxframework.component.factory.getComponents
import pixel.auxframework.util.toClass
import pixel.auxframework.util.toParameterized
import pixel.auxframework.web.AuxWeb
import pixel.auxframework.web.annotation.RequestMapping
import pixel.auxframework.web.annotation.RestController
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

data class RestRequestMappingEntry(val componentDefinition: ComponentDefinition, val method: KFunction<*>, val restController: RestController, val requestMapping: RequestMapping)

@Service
class RestWebService(private val auxWeb: AuxWeb, private val componentFactory: ComponentFactory) : InitializeWebServer, ComponentPostProcessor {

    val entries = mutableListOf<RestRequestMappingEntry>()

    fun register(entry: RestRequestMappingEntry) {
        entries += entry
    }

    override fun processComponent(componentDefinition: ComponentDefinition) {
        if (!componentDefinition.isInitialized()) return
        val instance = componentDefinition.castOrNull<Any?>() ?: return
        val restController = instance::class.findAnnotation<RestController>() ?: return
        for (mappingFunction in instance::class.memberFunctions) {
            val annotations = mappingFunction.findAnnotations(RequestMapping::class)
            for (annotation in annotations) {
                register(RestRequestMappingEntry(componentDefinition, mappingFunction, restController, annotation))
            }
        }
    }

    override fun initializeWebServer()  {
        auxWeb.getWebApplication().application.routing {
            for (entry in entries) {
                for (root in entry.restController.roots) {
                    val route = if (root.isRegex) route(Regex(root.path)) {}
                    else route(root.path) {}
                    for (path in entry.requestMapping.paths) {
                        for (methodName in entry.requestMapping.methods) {
                            val method = HttpMethod(methodName)
                            val mappingRoute = if (path.isRegex) route.route(Regex(path.path), method) {}
                            else route.route(path.path, method) {}
                            mappingRoute.handle {
                                val arguments: Map<KParameter, Any?> = entry.method.parameters.associateWith {
                                    if (it.name == null) return@associateWith entry.componentDefinition.cast()
                                    val mappers = componentFactory.getComponents<RequestMapper<*>>()
                                        .filter { mapper ->
                                            mapper::class.java.genericInterfaces
                                                .first { type -> type.toParameterized().rawType.toClass().isSubclassOf(RequestMapper::class) }
                                                .toParameterized()
                                                .actualTypeArguments.first()
                                                .toClass()
                                                .isSuperclassOf(it.type.toClass())
                                        }
                                    for (mapper in mappers) {
                                        val result = mapper.mapRequest(it, entry, route, mappingRoute, this)
                                        if (result is Some) return@associateWith result.value
                                    }
                                    null
                                }
                                var response: Any? = entry.method.callSuspendBy(arguments)
                                val responseMappers = componentFactory.getComponents<ResponseMapper>()
                                for (mapper in responseMappers) {
                                    val map = mapper.mapResponse(response, entry, route, mappingRoute, this)
                                    response = map.first
                                    if (!map.second) break
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

