package cn.lalaki.demo

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import cn.lalaki.demo19999.R
import cn.lalaki.save.vars.SaveVar
import java.nio.file.Paths
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
        try {
            SaveVar.init(Paths.get(filesDir.canonicalPath, "config.ini"))
            editText.append(SaveVar.INSTANCE.get("save"))
        } catch (ignored: Throwable) {
        }
        editText.addTextChangedListener(this)
        findViewById<Button>(R.id.btn_kill).setOnClickListener {
            finish()
            exitProcess(0)
        }
        SaveVar.INSTANCE.set("key", "value")
        SaveVar.INSTANCE.set("key", listOf("args0", "args1", "args2"), "&")
        SaveVar.INSTANCE.get("key")
        SaveVar.INSTANCE.get("key", "&")
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
        SaveVar.INSTANCE.set("save", s.toString())
    }
}
