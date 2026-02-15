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
        @Synchronized
        fun init(config: Path?, vararg secretKeyArray: ByteArray) {
            if (config != null) {
                sCONFIG = config
            }
            val mConfig = sCONFIG
            if (mConfig != null && mConfig.isRegularFile()) {
                val ivData = ByteArray(12)
                SecureRandom().nextBytes(ivData)
                val iv = GCMParameterSpec(96, ivData)
                mIV = iv
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                mCipher = cipher
                var key: SecretKey? = null
                if (secretKeyArray.isNotEmpty()) {
                    key = SecretKeySpec(secretKeyArray[0], cipher.algorithm)
                }
                val localKey = Path.of(mConfig.parent.absolutePathString(), ".secret.key")
                if (key == null && localKey.isRegularFile()) {
                    var keyData: ByteArray? = null
                    try {
                        keyData = Files.readAllBytes(localKey)
                    } catch (_: Throwable) {

                    }
                    if (keyData != null) {
                        key = SecretKeySpec(keyData, cipher.algorithm)
                    }
                }
                if (key == null) {
                    val keyGen = KeyGenerator.getInstance(cipher.algorithm)
                    keyGen.init(256)
                    key = keyGen.generateKey()
                    try {
                        Files.write(localKey, key.encoded)
                    } catch (_: Throwable) {
                    }
                }
                mSecretKey = key
                mConfig.inputStream().use {
                    INSTANCE.load(it)
                }
            }
        }

        var sCONFIG: Path? = null
        var mCipher: Cipher? = null
        var mIV: GCMParameterSpec? = null
        var mSecretKey: SecretKey? = null
        val INSTANCE by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SaveVar() }
    }

    @Synchronized
    fun get(name: String?): String {
        var value = ""
        if (!name.isNullOrEmpty()) {
            value = getProperty(name, "")
            if (value.isNotEmpty()) {
                var decodeData = Base64.decode(value, Base64.NO_PADDING or Base64.NO_WRAP)
                val cipher = mCipher
                val key = mSecretKey
                val iv = mIV
                if (cipher != null && key != null && iv != null) {
                    val ivSize = iv.iv.size
                    val myIv = decodeData.copyOfRange(decodeData.size - ivSize, decodeData.size)
                    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(iv.tLen, myIv))
                    decodeData = cipher.doFinal(decodeData.dropLast(ivSize).toByteArray())
                }
                value = decodeData.decodeToString()
            }
        }
        return value
    }

    @Synchronized
    fun set(
        name: String,
        value: String?,
    ) {
        if (name.isNotEmpty()) {
            if (!value.isNullOrEmpty()) {
                val cipher = mCipher
                val key = mSecretKey
                val iv = mIV
                var data = value.encodeToByteArray()
                if (cipher != null && key != null && iv != null) {
                    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
                    data = cipher.doFinal(data)
                    data += iv.iv
                }
                setProperty(
                    name, Base64.encodeToString(data, Base64.NO_PADDING or Base64.NO_WRAP)
                )
            } else {
                unset(name)
            }
            sCONFIG?.outputStream()?.use {
                this.store(it, null)
            }
        }
    }

    @Synchronized
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

    @Synchronized
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
        this.setProperty(name, "")
    }
}
