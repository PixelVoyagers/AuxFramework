package pixel.auxframework.core

import pixel.auxframework.util.FunctionUtils.memorize

class AuxVersion(val version: String) {

    companion object {
        fun current() = memorize {
            AuxVersion(AuxVersion::class.java.`package`.implementationVersion ?: "<null>")
        }
    }

    override fun hashCode() = version.hashCode()
    override fun equals(other: Any?) =
        other === this || (other != null && other is AuxVersion && other.version == this.version)

    override fun toString() = version

}