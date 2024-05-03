package pixel.auxframework.component.factory

import pixel.auxframework.core.runtime.status.BaseStatus
import pixel.auxframework.core.runtime.status.StatusSupplier
import java.util.*

enum class ModuleStatus : BaseStatus {
    IDLE, STARTING, RUNNING, STOPPING, STOPPED;

    override fun getStatusName() = this.name.toLowerCase(Locale.ROOT)
}

interface AuxModuleStatusSupplier : StatusSupplier<BaseStatus>

interface AuxModule
