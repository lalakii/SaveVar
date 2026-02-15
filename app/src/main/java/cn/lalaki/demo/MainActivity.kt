package cn.lalaki.demo

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
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
class MainActivity : AppCompatActivity(), TextWatcher {
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        textView = findViewById(R.id.textView)
        val editText = findViewById<AppCompatEditText>(R.id.editText)
        val configPath = Paths.get(filesDir.canonicalPath, "config.ini")
        try {
            SaveVar.init(configPath)
            editText.append(SaveVar.INSTANCE.get("save"))
        } catch (_: Throwable) {
        }
        editText.addTextChangedListener(this)
        findViewById<Button>(R.id.btn_kill).setOnClickListener {
            finish()
            exitProcess(0)
        }
        findViewById<Button>(R.id.btn_rm_config).setOnClickListener {
            if (configPath.isRegularFile()) {
                Files.delete(configPath)
                Toast.makeText(this@MainActivity, R.string.delete, Toast.LENGTH_SHORT).show()
                finish()
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
        //  SaveVar.INSTANCE.set("key", "value")
        // SaveVar.INSTANCE.set("key", listOf("args0", "args1", "args2"), "&")
        // SaveVar.INSTANCE.get("key")
        // SaveVar.INSTANCE.get("key", "&")
    }

    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int,
    ) {
    }

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int,
    ) {
    }

    override fun afterTextChanged(s: Editable?) {
        SaveVar.INSTANCE.set("save", "${s?.toString()}")
    }
}
