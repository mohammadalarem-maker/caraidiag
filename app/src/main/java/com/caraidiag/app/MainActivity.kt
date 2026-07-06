package com.caraidiag.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var inputMessage: EditText
    private lateinit var btnSend: Button
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ربط العناصر البرمجية بالواجهة الرسومية XML
        chatContainer = findViewById(R.id.chatContainer)
        inputMessage = findViewById(R.id.inputMessage)
        btnSend = findViewById(R.id.btnSend)

        // رسالة ترحيبية منهجية تظهر فور فتح التطبيق لتوجيه الفني أو المستخدم
        addMessageToChat("مرحباً بك في نظام التشخيص النيتف المحترف. يرجى كتابة نوع السيارة، والأعراض الحاصلة (مثل: تفتفة، تأخر تشغيل، لمبة محرك) لبدء التحليل واستبعاد الاحتمالات خطوة بخطوة.", false)

        // حدث الضغط على زر الإرسال
        btnSend.setOnClickListener {
            val message = inputMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                // 1. إضافة رسالة المستخدم للواجهة
                addMessageToChat(message, true)
                inputMessage.text.clear()
                
                // 2. إرسال العَرَض للذكاء الاصطناعي ومعالجته
                processDiagnostics(message)
            }
        }
    }

    // دالة بناء وتوليد فقاعات المحادثة برمجياً وديناميكياً لضمان الخفة والسرعة
    private fun addMessageToChat(message: String, isUser: Boolean) {
        runOnUiThread {
            val textView = TextView(this).apply {
                text = message
                textSize = 15f
                setPadding(24, 16, 24, 16)
                setTextColor(if (isUser) Color.WHITE else Color.parseColor("#0F172A"))
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                    bottomMargin = 12
                    gravity = if (isUser) Gravity.END else Gravity.START
                }
                layoutParams = params
                
                // تصميم خلفية الفقاعة (أزرق للمستخدم ورمادي للمساعد)
                setBackgroundColor(if (isUser) Color.parseColor("#2563EB") else Color.parseColor("#E2E8F0"))
            }
            chatContainer.addView(textView)
        }
    }

    // الدالة المسؤولة عن معالجة التشخيص وأمر الربط بـ الـ API لاحقاً
    private fun processDiagnostics(userMessage: String) {
        // رسالة مؤقتة لمحاكاة الفحص السريع حتى نقوم بربط الـ API في الخطوة التالية
        addMessageToChat("جاري تحليل العَرَض برمجياً... يرجى التحقق من ضغط الوقود والفيوزات المرتبطة كخطوة أولية.", false)
    }
}
