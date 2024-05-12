package pixel.auxframework.web

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import pixel.auxframework.component.annotation.Service
import pixel.auxframework.component.factory.ComponentFactory
import pixel.auxframework.component.factory.DisposableComponent
import pixel.auxframework.component.factory.getComponents
import pixel.auxframework.context.builtin.AfterContextRefreshed
import pixel.auxframework.web.server.InitializeWebServer
import pixel.auxframework.web.server.ServerConfig

@Service
class AuxWeb(private val componentFactory: ComponentFactory, private val config: ServerConfig) : AfterContextRefreshed, DisposableComponent {

    private var webApplication: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    fun getWebApplicationOrNull() = webApplication
    fun getWebApplication() = getWebApplicationOrNull()!!

    override fun afterContextRefreshed() {
        webApplication = embeddedServer(Netty, config.port) {
            componentFactory.getComponents<InitializeWebServer>().forEach(InitializeWebServer::initializeWebServer)
        }
        getWebApplication().start()
    }

    override fun dispose() = getWebApplication().stop()

}
