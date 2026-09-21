package cn.lalaki.demo

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.lalaki.demo.a10086.R
import cn.lalaki.save.vars.SaveVarManager
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.isRegularFile
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

/**
 * Created on 2024-06-02
 *
 * @author lalaki (i@lalaki.cn)
 * @since 测试类
 * 保存变量测试
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val path = cacheDir.resolve("app-01.log")
            val log =
                "TIME: ${System.currentTimeMillis()}\b${thread.name}\n${thread}\n${throwable.message}\n${throwable.localizedMessage}\n${throwable.cause}\n${throwable}\n\n"
            Files.write(
                path.toPath(),
                log.encodeToByteArray(),
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE
            )
            finish()
            exitProcess(0)
        }
        setContentView(R.layout.main)
        val spinnerView = findViewById<Spinner>(R.id.variable_list)
        val editText = findViewById<EditText>(R.id.edit_key_text)
        val editValueText = findViewById<EditText>(R.id.edit_value_text)
        val configPath = Paths.get(cacheDir.canonicalPath, "config.ini")
        val aesKey = cacheDir.resolve("aes.key").toPath()
        var aesKeyData: ByteArray
        if (Files.exists(aesKey)) {
            ByteArrayOutputStream().use {
                Files.copy(aesKey, it)
                aesKeyData = it.toByteArray()
            }
        } else {
            aesKeyData = SaveVarManager.generateRandomAESKey()
            Files.write(aesKey, aesKeyData)
        }
        if (SaveVarManager.init(configPath, aesKeyData)) {
            Toast.makeText(this, R.string.props_load, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.props_decrypt_failed, Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btn_kill).setOnClickListener {
            finish()
            exitProcess(0)
        }
        val list = mutableListOf<String>()
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
        for (key in SaveVarManager.keys()) {
            arrayAdapter.add("${key}=${SaveVarManager.get(key)}")
        }
        spinnerView.adapter = arrayAdapter
        findViewById<Button>(R.id.btn_rm_config).setOnClickListener {
            if (configPath.isRegularFile()) {
                Files.delete(configPath)
                Toast.makeText(this@MainActivity, R.string.delete, Toast.LENGTH_SHORT).show()
                arrayAdapter.clear()
                arrayAdapter.notifyDataSetChanged()
            }
        }
        arrayAdapter.notifyDataSetChanged()
        findViewById<Button>(R.id.btn_add).setOnClickListener {
            SaveVarManager.set("${editText.text}", "${editValueText.text}")
            var removed = false
            var item1: String? = null
            for (i in 0 until arrayAdapter.count) {
                val item = arrayAdapter.getItem(i)
                if (item?.startsWith("${editText.text}=") == true) {
                    removed = true
                    item1 = item
                    break
                }
            }
            if (removed) {
                arrayAdapter.remove(item1)
            }
            arrayAdapter.add("${editText.text}=${editValueText.text}")
            arrayAdapter.notifyDataSetChanged()
            try {
                val size = arrayAdapter.count
                spinnerView.setSelection(size - 1)
            } catch (_: Throwable) {
            }
        }
        findViewById<Button>(R.id.regen_aes_key).setOnClickListener {
            if (Files.isRegularFile(aesKey)) {
                Files.delete(aesKey)
            }
            Toast.makeText(this,R.string.regen_aes_key_done, Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.random_test).setOnClickListener {
            val threadPool = Executors.newFixedThreadPool(24)
            val totalWrites = 10000
            val threadsCount = 24
            val writesPerThread = totalWrites / threadsCount
            thread {
                measureTimeMillis {
                    for (t in 0 until threadsCount) {
                        @Suppress("unused")
                        threadPool.execute {
                            for (i in 0 until writesPerThread) {
                                // 动态生成不同的 Key-Value，并疯狂调用 set 执行“排队加解密与落盘”
                                val key = "thread_${t}_key_${UUID.randomUUID()}"
                                val value = "value_data_${System.currentTimeMillis()}"
                                SaveVarManager.set(key, value)
                                try {
                                    Log.v("SaveVarTest", "${key}:${value}")
                                } catch (e: Throwable) {
                                    Log.e("SaveVarTest", "❌ 线程 $t 写入中途崩溃！", e)
                                }
                            }
                        }
                    }
                    threadPool.shutdown()
                    try {
                        threadPool.awaitTermination(5, TimeUnit.MINUTES)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        findViewById<Button>(R.id.view_config).setOnClickListener {
            if (configPath.isRegularFile()) {
                var bytes: ByteArray? = null
                try {
                    bytes = Files.readAllBytes(configPath)
                } catch (_: Throwable) {
                }
                if (bytes != null) {
                    val baseText = Base64.encodeToString(bytes, Base64.DEFAULT)
                    val recyclerView = RecyclerView(this)
                    recyclerView.layoutManager = LinearLayoutManager(this)
                    recyclerView.adapter =
                        TextItemAdapter(baseText.split("\n"), LayoutInflater.from(this))
                    AlertDialog.Builder(this).setView(recyclerView).show()
                }
            }
        }
    }


    class TextItemAdapter(val list: List<String>, val inflater: LayoutInflater) :
        RecyclerView.Adapter<TextItemAdapter.TextItemHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): TextItemHolder {
            return TextItemHolder(
                inflater.inflate(
                    android.R.layout.simple_list_item_1,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(
            holder: TextItemHolder,
            position: Int
        ) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int {
            return list.size
        }

        class TextItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(android.R.id.text1)
            fun bind(text: String) {
                textView.text = text
            }
        }
    }
}
