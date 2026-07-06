package com.caraidiag.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var inputMessage: EditText
    private lateinit var btnSend: Button
    private val client = OkHttpClient()
    
    // سيقوم جيت هاب بحقن مفتاحك الآمن هنا تلقائياً أثناء البناء
    private val apiKey = "_SECURE_GEMINI_KEY_" 
    private val apiUrl = "https://api.groq.com/openai/v1/chat/completions"
    
    // حفظ تاريخ المحادثة التفاعلية لترابط التشخيص
    private val conversationalHistory = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        // 1. شريط العنوان العلوي (Header)
        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(32, 40, 32, 40)
            elevation = 8f
            gravity = Gravity.CENTER
        }
        val headerTitle = TextView(this).apply {
            text = "مساعد فحص وتشخيص السيارات الذكي"
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        headerBar.addView(headerTitle)
        rootLayout.addView(headerBar)

        // 2. منطقة عرض المحادثة (ScrollView)
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
        }
        chatContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        scrollView.addView(chatContainer)
        rootLayout.addView(scrollView)

        // 3. شريط الإدخال السفلي بتصميم جمني
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(24, 20, 24, 20)
            gravity = Gravity.CENTER_VERTICAL
            elevation = 16f
        }

        inputMessage = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 16
            }
            hint = "اكتب العَرَض أو نتيجة الفحص..."
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.parseColor("#1E293B"))
            textSize = 15f
            setPadding(36, 28, 36, 28)
            
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#F1F5F9"))
                cornerRadius = 45f
            }
        }

        btnSend = Button(this).apply {
            text = "إرسال"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(44, 0, 44, 0)
            
            // حل مشكلة الانبت الجذري: منع الزر من أخذ التركيز نهائياً عند الضغط عليه
            isFocusable = false
            isFocusableInTouchMode = false
            
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#1A73E8"))
                cornerRadius = 45f
            }
        }

        bottomBar.addView(inputMessage)
        bottomBar.addView(btnSend)
        rootLayout.addView(bottomBar)

        setContentView(rootLayout)

        // الرسالة الترحيبية الأولى
        addMessageToChat("مرحباً بك يا هندسة. أنا مساعدك الفني لتتبع الأعطال خطوة بخطوة. اكتب المشكلة الحاصلة في السيارة لنبدأ في تحليلها فوراً.", false)

        btnSend.setOnClickListener {
            val message = inputMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessageToChat(message, true)
                inputMessage.text.clear()
                
                // إعادة التركيز فوراً إلى حقل الإدخال
                inputMessage.requestFocus()
                
                // إجبار الكيبورد على البقاء مفتوحاً ونشطاً للكتابة المستمرة
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(inputMessage, InputMethodManager.SHOW_IMPLICIT)
                
                val userLog = JSONObject().apply {
                    put("role", "user")
                    put("content", message)
                }
                conversationalHistory.put(userLog)
                
                processDiagnostics()
            }
        }
    }

    private fun addMessageToChat(message: String, isUser: Boolean) {
        runOnUiThread {
            val textView = TextView(this).apply {
                text = message
                textSize = 15f
                setPadding(38, 26, 38, 26)
                setTextColor(Color.parseColor(if (isUser) "#041E49" else "#1E293B"))
                setTextIsSelectable(true) // الرسائل قابلة للنسخ والتحديد
                
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor(if (isUser) "#E9F1FE" else "#F1F3F4"))
                    cornerRadius = 36f
                }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                    bottomMargin = 12
                    gravity = if (isUser) Gravity.END else Gravity.START
                    maxWidth = (resources.displayMetrics.widthPixels * 0.78).toInt()
                }
                layoutParams = params
            }
            
            chatContainer.addView(textView)
            
            // التمرير التلقائي الفوري لأسفل الشاشة
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
                // تأكيد إضافي لبقاء حقل الإدخال فعالاً بعد التمرير
                if (isUser) {
                    inputMessage.requestFocus()
                }
            }
        }
    }

    private fun processDiagnostics() {
        addMessageToChat("جاري تحليل المعطيات هندسياً...", false)

        val jsonPayloadArray = JSONArray()
        
        // تعديل هندسة الأوامر لفرض التحليل الهندسي وضبط اللهجة الفنية البيضاء
        val systemPromptObj = JSONObject().apply {
            put("role", "system")
            put("content", "أنت مهندس تشخيص أعطال محترف وخبير في كهرباء وميكانيك السيارات (Diagnostic Master). تتحدث بلهجة بيضاء فنية واضحة ومفهومة لجميع الفنيين في الورش (مثل استخدام المصطلحات: سلف، دينامو، طبلون، كويلات، بخاخات، ريلاي، فيوز)، ويُمنع تماماً استخدام اللهجة المغربية أو أي مصطلحات غير متداولة في ورش الجزيرة العربية. طريقة عملك الصارمة: 1. ممنوع التخمين العشوائي أو إعطاء حلول نهائية متسرعة. 2. عند استلام أي إجابة أو نتيجة فحص من الفني، يجب أولاً أن تقوم بتحليل النتيجة هندسياً في بداية رسالتك وتشرح له ماذا نستنتج من هذه النتيجة وماذا نستبعد تماماً (مثال: بما أن السلف يدق بقوة، إذن البطارية ونظام التشغيل الأولي سليمين، والخلل ينحصر الآن في منظومة الوقود أو الإشعال). 3. بناءً على هذا التحليل، اطرح خطوة الفحص التالية مباشرة بسؤال واحد محدد ومختصر جداً لتقليص الاحتمالات والوصول للخلل بالتدريج. 4. إذا كانت هذه أول شكوى ولم يذكر الفني تفاصيل السيارة، اطلب منه أولاً تحديد نوع السيارة وموديلها وهل لمبة الماكينة والعة أم لا مع ذكر كود الفحص إن وجد. اجعل ردودك عملية، مقتضبة، ومرتبة هندسياً.")
        }
        jsonPayloadArray.put(systemPromptObj)

        for (i in 0 until conversationalHistory.length()) {
            jsonPayloadArray.put(conversationalHistory.getJSONObject(i))
        }

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val jsonBody = JSONObject().apply {
            put("model", "llama-3.1-8b-instant")
            put("messages", jsonPayloadArray)
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                removeLastMessage()
                addMessageToChat("فشل اتصال بالشبكة: ${e.message}", false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    removeLastMessage()
                    val responseData = response.body?.string() ?: "استجابة فارغة"
                    
                    if (!response.isSuccessful) {
                        addMessageToChat("خطأ من السيرفر: كود ${response.code}", false)
                        return
                    }
                    
                    try {
                        val jsonObject = JSONObject(responseData)
                        val choices = jsonObject.getJSONArray("choices")
                        val firstChoice = choices.getJSONObject(0)
                        val messageObj = firstChoice.getJSONObject("message")
                        val aiResponse = messageObj.getString("content")
                        
                        val aiLog = JSONObject().apply {
                            put("role", "assistant")
                            put("content", aiResponse)
                        }
                        conversationalHistory.put(aiLog)
                        
                        addMessageToChat(aiResponse, false)
                    } catch (e: Exception) {
                        addMessageToChat("خطأ معالجة داخلي في عرض الرد.", false)
                    }
                }
            }
        })
    }

    private fun removeLastMessage() {
        runOnUiThread {
            val childCount = chatContainer.childCount
            if (childCount > 0) {
                chatContainer.removeViewAt(childCount - 1)
            }
        }
    }
}
