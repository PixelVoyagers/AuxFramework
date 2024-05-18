package pixel.auxframework.web.server

import pixel.auxframework.component.annotation.Autowired
import pixel.auxframework.component.annotation.Component

@Component
data class ServerConfig(@Autowired(false) var port: Int = 8080)