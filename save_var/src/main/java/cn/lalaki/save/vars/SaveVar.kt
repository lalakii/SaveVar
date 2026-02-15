package cn.lalaki.save.vars

import android.util.Base64
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import kotlin.io.path.outputStream

@Suppress("MemberVisibilityCanBePrivate")
open class SaveVar : Properties() {
    companion object {
        fun init(config: Path?, vararg secretKeyArray: ByteArray) {
            sCONFIG = config
            val mConfig = sCONFIG
            if (mConfig != null) {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                mCipher = cipher
                val alg = cipher.algorithm.take(3)
                var key: SecretKey? = null
                if (secretKeyArray.isNotEmpty()) {
                    key = SecretKeySpec(secretKeyArray[0], alg)
                }
                val localKey = Path.of(mConfig.parent.absolutePathString(), ".secret.key")
                if (key == null && localKey.isRegularFile()) {
                    var keyData: ByteArray? = null
                    try {
                        keyData = Files.readAllBytes(localKey)
                    } catch (_: Throwable) {
                    }
                    if (keyData != null) {
                        key = SecretKeySpec(keyData, alg)
                    }
                }
                if (key == null) {
                    val keyGen = KeyGenerator.getInstance(alg)
                    keyGen.init(256)
                    key = keyGen.generateKey()
                    try {
                        Files.write(localKey, key.encoded)
                    } catch (_: Throwable) {
                    }
                }
                mSecretKey = key
                if (mConfig.isRegularFile()) {
                    mConfig.inputStream().use {
                        INSTANCE.load(it)
                    }
                }
            }
        }

        val INSTANCE by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SaveVar() }
        private var sCONFIG: Path? = null
        private var mCipher: Cipher? = null
        private var mSecretKey: SecretKey? = null
        private val mSecureRandom = SecureRandom()
        private const val IV_SIZE = 12
        private const val T_LEN = 128
    }

    fun get(name: String?): String {
        var value = ""
        if (!name.isNullOrEmpty() && this.containsKey(name)) {
            value = getProperty(name, "")
            if (value.isNotEmpty()) {
                var decodeData = Base64.decode(value, Base64.NO_WRAP or Base64.NO_PADDING)
                val cipher = mCipher
                val key = mSecretKey
                if (cipher != null && key != null) {
                    val myIv = decodeData.copyOfRange(decodeData.size - IV_SIZE, decodeData.size)
                    try {
                        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(T_LEN, myIv))
                        decodeData = cipher.doFinal(decodeData.dropLast(IV_SIZE).toByteArray())
                    } catch (_: Throwable) {
                    }
                }
                value = decodeData.decodeToString()
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
                val cipher = mCipher
                val key = mSecretKey
                val ivData = ByteArray(IV_SIZE)
                mSecureRandom.nextBytes(ivData)
                val iv = GCMParameterSpec(T_LEN, ivData)
                var data = value.encodeToByteArray()
                if (cipher != null && key != null) {
                    try {
                        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
                        data = cipher.doFinal(data) + iv.iv
                    } catch (_: Throwable) {
                    }
                }
                setProperty(
                    name, Base64.encodeToString(data, Base64.NO_WRAP or Base64.NO_PADDING)
                )
            } else {
                unset(name)
            }
            sCONFIG?.outputStream()?.use {
                this.store(it, null)
            }
        }
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

    private fun unset(name: String) {
        this.remove(name)
    }
}
