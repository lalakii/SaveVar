// SPDX-License-Identifier: MIT
package cn.lalaki.save.vars

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Properties
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/***
 * @since SaveVarEnhanced 2026-09-21
 * @author lalaki i@lalaki.cn
 * @see <a href="https://github.com/lalakii/SaveVar">SaveVar</a>
 */
sealed class SaveVarManager {
    companion object {
        private const val DELIMITER_CHAR: Char = '\u001F'
        private val SAVE_VAR by lazy { SaveVar() }
        private val THREAD_POOL: ThreadPoolExecutor by lazy {
            ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(1.coerceAtLeast(Runtime.getRuntime().availableProcessors())),
                { runnable -> Thread(runnable, "SaveVar-IO-Executor") },
                ThreadPoolExecutor.DiscardOldestPolicy()
            )
        }

        fun init(configPath: Path, aesKey: ByteArray): Boolean {
            clear()
            if (SAVE_VAR.init(configPath, aesKey)) {
                return true
            }
            SAVE_VAR.wipe()
            return false
        }

        fun get(key: Any?, fallbackValue: String = ""): String {
            if (key is String) {
                return get(key, fallbackValue)
            }
            return fallbackValue
        }

        fun get(key: String, fallbackValue: String = ""): String {
            return SAVE_VAR.getProperty(key, fallbackValue)
        }

        fun set(key: String, value: String?) {
            SAVE_VAR.also {
                if (value == null) {
                    it.remove(key)
                } else {
                    it.setProperty(key, value)
                }
            }
        }

        @Suppress("unused")
        fun getStringList(key: String) = get(key, "").split(DELIMITER_CHAR)

        fun getOrderedStringSet(key: String): LinkedHashSet<String> =
            get(key, "").split(DELIMITER_CHAR).filter { it.isNotBlank() }
                .toCollection(LinkedHashSet())

        @Suppress("unused")
        fun addToOrderedStringSet(key: String, value: String?) {
            if (value != null) {
                getOrderedStringSet(key).also {
                    it.add(value)
                    set(key, it.joinToString(separator = DELIMITER_CHAR.toString()))
                }
            }
        }

        @Suppress("unused")
        fun addToOrderedStringSet(key: String, value: List<String>?, append: Boolean = true) {
            if (value != null) {
                if (!append) {
                    set(key, null)
                }
                getOrderedStringSet(key).also {
                    it.addAll(value)
                    set(key, it.joinToString(separator = DELIMITER_CHAR.toString()))
                }
            }
        }

        fun keys(): Set<Any> = SAVE_VAR.keys

        @Suppress("unused")
        fun clear() = SAVE_VAR.clear()

        @Suppress("unused")
        fun clearConfigAndCache() {
            SAVE_VAR.clearConfigAndCache()
        }

        @Suppress("unused")
        fun generateRandomAESKey() = SAVE_VAR.generateRandomAESKey()
        private fun isWritableRegularFile(path: Path) =
            Files.isRegularFile(path) && Files.isReadable(path) && Files.isWritable(path)

        private fun getTmpConfigPath(path: Path): Path {
            val fileName = path.fileName?.toString() ?: ".cn_lalaki_def_config.ini"
            return if (!fileName.startsWith(".")) {
                path.resolveSibling(".${fileName}")
            } else {
                path.resolveSibling("_${fileName}")
            }
        }
    }

    private class SaveVar : Properties() {
        private companion object {
            private const val KEY_STORE_TIMESTAMP = "__cn.lalaki.save_var.timestamp"
            private const val KEY_IV_LEN = 16
            private val SECURE_RANDOM by lazy { SecureRandom() }
        }

        private var mAesKey: ByteArray? = null
        private var mConfigPath: Path? = null
        fun init(configPath: Path, aesKey: ByteArray): Boolean {
            this.mConfigPath = configPath
            this.mAesKey = aesKey
            val vars = requireVars() ?: return false
            if (!Files.exists(vars.first)) {
                return true
            }
            try {
                Files.setAttribute(
                    vars.first, "posix:permissions", PosixFilePermissions.fromString("rw-------")
                )
            } catch (_: Throwable) {
            }
            try {
                return loadDecrypted(vars.first, aesKey)
            } catch (_: Throwable) {
            }
            return false
        }

        fun generateRandomAESKey() = ByteArray(32).also {
            SECURE_RANDOM.nextBytes(it)
        }

        fun clearConfigAndCache() {
            requireVars()?.also {
                THREAD_POOL.execute {
                    safelyDeleteFile(it.first)
                    safelyDeleteFile(getTmpConfigPath(it.first))
                }
                clear()
                wipe()
            }
        }

        fun wipe() {
            requireVars()?.also {
                mConfigPath = null
                it.second.fill(0)
                mAesKey = null
            }
        }

        private fun loadDecrypted(configPath: Path, aesKey: ByteArray): Boolean {
            if (isWritableRegularFile(configPath)) {
                BufferedInputStream(Files.newInputStream(configPath)).use { fis ->
                    val iv = ByteArray(KEY_IV_LEN)
                    if (fis.read(iv) == KEY_IV_LEN) {
                        load(
                            CipherInputStream(
                                fis, getCipher(aesKey, Cipher.DECRYPT_MODE, AtomicReference(iv))
                            )
                        )
                        return true
                    }
                }
            }
            return false
        }

        private fun storeEncrypted() = requireVars()?.also { vars ->
            val cipher = getCipher(vars.second, Cipher.ENCRYPT_MODE, AtomicReference(null))
            val tmpPath = getTmpConfigPath(vars.first)
            BufferedOutputStream(Files.newOutputStream(tmpPath)).also { bos ->
                bos.write(cipher.iv)
                CipherOutputStream(bos, cipher).use { cos ->
                    store(cos, System.currentTimeMillis().toString())
                }
            }
            Files.move(
                tmpPath,
                vars.first,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        private fun getCipher(
            aesKey: ByteArray, mode: Int, iv: AtomicReference<ByteArray?>
        ) = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(
                mode,
                SecretKeySpec(aesKey, "AES"),
                IvParameterSpec(iv.updateAndGet { p0 -> p0 ?: genIV(); })
            )
        }

        private fun genIV() = ByteArray(KEY_IV_LEN).also {
            SECURE_RANDOM.nextBytes(it)
        }

        private fun requireVars(): Pair<Path, ByteArray>? {
            val configPath = mConfigPath ?: return null
            val aesKey = mAesKey ?: return null
            return configPath to aesKey
        }

        private fun safelyDeleteFile(path: Path) {
            if (isWritableRegularFile(path)) {
                Files.delete(path)
            }
        }

        override fun setProperty(key: String?, value: String?): Any? {
            val oldValue = super.getProperty(key)
            if (oldValue != value) {
                super.setProperty(KEY_STORE_TIMESTAMP, System.currentTimeMillis().toString())
                THREAD_POOL.execute {
                    try {
                        storeEncrypted()
                    } catch (_: Throwable) {
                    }
                }
            }
            return super.setProperty(key, value)
        }
    }
}