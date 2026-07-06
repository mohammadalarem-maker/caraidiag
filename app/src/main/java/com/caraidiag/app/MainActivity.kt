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
    
    private val apiKey = "_SECURE_GEMINI_KEY_" 
    private val apiUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=$apiKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatContainer = findViewById(R.id.chatContainer)
        inputMessage = findViewById(R.id.inputMessage)
        btnSend = findViewById(R.id.btnSend)

        addMessageToChat("مرحباً بك. نظام كشف الأخطاء الموسّع مفعّل الآن لمعرفة سبب عدم استجابة السيرفر بدقة.", false)

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
        addMessageToChat("جاري الاتصال واستخلاص كود الاستجابة الفعلي...", false)

        val systemPrompt = "أنت مهندس ميكانيك. حلل: $userMessage"

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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                addMessageToChat("فشل اتصال بالشبكة تماماً: ${e.message}", false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseData = response.body?.string() ?: "استجابة فارغة"
                    
                    // إذا فشل الطلب، سنعرض الكود والرد القادم من جوجل بالكامل
                    if (!response.isSuccessful) {
                        addMessageToChat("رد السيرفر الرسمي (كود ${response.code}):\n$responseData", false)
                        return
                    }
                    
                    try {
                        val jsonObject = JSONObject(responseData)
                        val candidates = jsonObject.getJSONArray("candidates")
                        val firstCandidate = candidates.getJSONObject(0)
                        val contentObj = firstCandidate.getJSONObject("content")
                        val parts = contentObj.getJSONArray("parts")
                        val aiResponse = parts.getJSONObject(0).getString("text")
                        
                        addMessageToChat(aiResponse, false)
                    } catch (e: Exception) {
                        addMessageToChat("خطأ معالجة داخلي: ${e.message}\nالرد الأصلي:\n$responseData", false)
                    }
                }
            }
        })
    }
}
