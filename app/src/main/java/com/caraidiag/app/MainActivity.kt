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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var inputMessage: EditText
    private lateinit var btnSend: Button
    private val client = OkHttpClient()
    
    // سيقوم خادم جيت هاب بحقن المفتاح هنا تلقائياً أثناء البناء
    private val apiKey = "_SECURE_GEMINI_KEY_" 
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatContainer = findViewById(R.id.chatContainer)
        inputMessage = findViewById(R.id.inputMessage)
        btnSend = findViewById(R.id.btnSend)

        addMessageToChat("مرحباً بك في نظام التشخيص النيتف المحترف. يرجى كتابة نوع السيارة، والأعراض الحاصلة (مثل: تفتفة، تأخر تشغيل، لمبة محرك) لبدء التحليل واستبعاد الاحتمالات خطوة بخطوة.", false)

        btnSend.setOnClickListener {
            val message = inputMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessageToChat(message, true)
                inputMessage.text.clear()
                processDiagnostics(message)
            }
        }
    }

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
                setBackgroundColor(if (isUser) Color.parseColor("#2563EB") else Color.parseColor("#E2E8F0"))
            }
            chatContainer.addView(textView)
        }
    }

    private fun processDiagnostics(userMessage: String) {
        addMessageToChat("جاري الاتصال بالذكاء الاصطناعي وتحليل العَرَض الفني...", false)

        val systemPrompt = "أنت مهندس وخبير محترف في كهرباء وميكانيك السيارات. قم بتحليل العَرَض التالي وأعطِ الفني خطوات فحص منهجية مرتبة واستبعد الاحتمالات من الأسهل للأعقد. العَرَض: $userMessage"

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val jsonBody = """
            {
                "contents": [{
                    "parts": [{"text": "$systemPrompt"}]
                }]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(apiUrl)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        // تمرير الـ Callback بالشكل الصحيح هندسياً وإغلاق الأقواس بدقة
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                addMessageToChat("فشل الاتصال بالشبكة: ${e.message}.. تحقق من الإنترنت.", false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        addMessageToChat("خطأ من خادم الذكاء الاصطناعي. تأكد من صحة مفتاح الـ API.", false)
                        return
                    }
                    
                    val responseData = response.body?.string()
                    if (responseData != null) {
                        try {
                            val jsonObject = JSONObject(responseData)
                            val candidates = jsonObject.getJSONArray("candidates")
                            val firstCandidate = candidates.getJSONObject(0)
                            val contentObj = firstCandidate.getJSONObject("content")
                            val parts = contentObj.getJSONArray("parts")
                            val aiResponse = parts.getJSONObject(0).getString("text")
                            
                            addMessageToChat(aiResponse, false)
                        } catch (e: Exception) {
                            addMessageToChat("حدث خطأ أثناء معالجة البيانات التشخيصية.", false)
                        }
                    }
                }
            }
        })
    }
}
