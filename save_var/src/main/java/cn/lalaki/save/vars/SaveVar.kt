package cn.lalaki.save.vars

import android.util.Base64
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import kotlin.io.path.outputStream

@Suppress("MemberVisibilityCanBePrivate")
open class SaveVar : Properties() {
    companion object {
        fun init(config: Path?) {
            if (config != null) {
                sCONFIG = config
            }
            val mConfig = sCONFIG
            if (mConfig != null && mConfig.isRegularFile()) {
                mConfig.inputStream().use {
                    INSTANCE.load(it)
                }
            }
        }

        var sCONFIG: Path? = null
        val INSTANCE by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SaveVar() }
    }

    fun get(name: String?): String {
        var value = ""
        if (!name.isNullOrEmpty()) {
            value = getProperty(name, "")
            if (!value.isNullOrEmpty()) {
                value = Base64.decode(value, Base64.DEFAULT).decodeToString()
            }
        }
        return value
    }

    fun set(
        name: String,
        value: String?,
    ) {
        if (name.isNotEmpty()) {
            if (!value.isNullOrEmpty()) {
                setProperty(name, Base64.encodeToString(value.encodeToByteArray(), Base64.DEFAULT))
            } else {
                unset(name)
            }
            sCONFIG?.outputStream()?.use {
                this.store(it, null)
            }
        }
    }

    private fun unset(name: String) {
        this.setProperty(name, "")
    }

    fun set(
        name: String,
        value: List<String>?,
        separator: String,
    ) {
        if (value.isNullOrEmpty()) {
            unset(name)
        } else {
            set(name, value.joinToString(separator))
        }
    }

    fun get(
        name: String?,
        separator: String,
    ): List<String>? {
        val value = get(name)
        if (value.isNotEmpty()) {
            return value.split(separator)
        }
        return null
    }
}
