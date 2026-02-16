package cn.lalaki.demo

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import cn.lalaki.demo19999.R
import cn.lalaki.save.vars.SaveVar
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.system.exitProcess

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
        setContentView(R.layout.main)
        val spinnerView = findViewById<Spinner>(R.id.variable_list)
        val editText = findViewById<EditText>(R.id.edit_key_text)
        val editValueText = findViewById<EditText>(R.id.edit_value_text)
        val configPath = Paths.get(filesDir.canonicalPath, "config.ini")
        try {
            SaveVar.init(configPath)
        } catch (_: Throwable) {
        }
        findViewById<Button>(R.id.btn_kill).setOnClickListener {
            finish()
            exitProcess(0)
        }
        val list = mutableListOf<String>()
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
        spinnerView.adapter = arrayAdapter
        for (key in SaveVar.INSTANCE.keys().iterator()) {
            arrayAdapter.add("${key}=${SaveVar.INSTANCE.get(key.toString())}")
        }
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
            SaveVar.INSTANCE.set("${editText.text}", "${editValueText.text}")
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
        findViewById<Button>(R.id.view_config).setOnClickListener {
            if (configPath.isRegularFile()) {
                var lines: List<String>? = null
                try {
                    lines = Files.readAllLines(configPath)
                } catch (_: Throwable) {
                }
                if (lines != null) {
                    val tv = TextView(this)
                    for (line in lines) {
                        tv.append("${line}\r\n")
                    }
                    AlertDialog.Builder(this).setView(ScrollView(this).apply { addView(tv) }).show()
                }
            }
        }
    }
}
