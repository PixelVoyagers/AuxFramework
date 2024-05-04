package pixel.auxframework.component.factory

import pixel.auxframework.core.runtime.status.BaseStatus
import pixel.auxframework.core.runtime.status.StatusSupplier
import java.util.*

/**
 * 模块装填
 * @see AuxModuleStatusSupplier
 */
enum class ModuleStatus : BaseStatus {
    IDLE, STARTING, RUNNING, STOPPING, STOPPED;

    override fun getStatusName() = this.name.toLowerCase(Locale.ROOT)
}

/**
 * 模块装填供应器
 * @see AuxModule
 */
interface AuxModuleStatusSupplier : StatusSupplier<BaseStatus>

/**
 * 模块
 */
interface AuxModule
