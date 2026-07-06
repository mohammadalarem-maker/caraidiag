package com.caraidiag.app

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
    
    // حفظ تاريخ المحادثة التفاعلية لإرسالها بالكامل مع كل طلب ليعرف الذكاء الاصطناعي الإجابات السابقة
    private val conversationalHistory = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // بناء الواجهة برمجياً 100% لضمان التحديث الجمالي الفوري وحل مشكلة التمرير بدون تعديل ملفات XML
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC")) // خلفية مريحة وعصرية
        }

        // 1. شريط العنوان العلوي (Header) بتصميم أنيق وهادئ
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

        // 2. منطقة عرض المحادثة مع تفعيل التمرير التلقائي لأسفل (ScrollView)
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

        // 3. شريط الإدخال السفلي بتصميم دائري متطابق مع واجهات جمني الحديثة
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
            
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#1A73E8")) // لون جمني الأزرق القياسي
                cornerRadius = 45f
            }
        }

        bottomBar.addView(inputMessage)
        bottomBar.addView(btnSend)
        rootLayout.addView(bottomBar)

        setContentView(rootLayout)

        // الرسالة الترحيبية المنهجية الأولى للسيستم الفني
        addMessageToChat("مرحباً بك يا هندسة في نظام الفحص المنهجي. يرجى كتابة المشكلة الحاصلة في السيارة للبدء في تتبعها خطوة بخطوة.", false)

        btnSend.setOnClickListener {
            val message = inputMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessageToChat(message, true)
                inputMessage.text.clear()
                
                // تفعيل حقل الإدخال فوراً وإبقائه نشطاً للكتابة المستمرة دون الحاجة للضغط عليه مجدداً
                inputMessage.requestFocus()
                
                // حفظ رسالة المستخدم في السجل لضمان ترابط الأفكار
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
                setTextIsSelectable(true) // جعل النص قابلاً للنسخ والتحديد الكامل بكل سهولة
                
                // تصميم فقاعات انسيابية مثل جمني تماماً
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
                    // تحديد أقصى عرض للفقاعة لتبقى متناسقة
                    maxWidth = (resources.displayMetrics.widthPixels * 0.78).toInt()
                }
                layoutParams = params
            }
            
            chatContainer.addView(textView)
            
            // الحل الجذري لمشكلة التمرير: قفز الشاشة لأسفل فوراً عند ظهور أي رسالة
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun processDiagnostics() {
        addMessageToChat("جاري تحليل الخطوة الحالية بفحص هندسي...", false)

        val jsonPayloadArray = JSONArray()
        
        // هندسة الأوامر الصارمة لتحويل الشات إلى شجرة تشخيص فنية تمنع العشوائية والتخمين
        val systemPromptObj = JSONObject().apply {
            put("role", "system")
            put("content", "أنت مهندس تشخيص أعطال وخبير محترف في كهرباء وميكانيك السيارات (Diagnostic Master). وضيفتك هي قيادة الفني عبر شجرة تشخيص هندسية صارمة ومنع التخمين العشوائي تماماً. التزم بالقواعد التالية بالملي: 1. ممنوع تخمين العطل أو تقديم حلول نهائية أو سرد احتمالات متعددة من البداية. 2. إذا كانت هذه أول شكوى من المستخدم ولم يذكر تفاصيل السيارة، يجب أن تسأله فوراً وسؤالاً وحيداً: (ما هو نوع وموديل السيارة؟ وهل هناك أكواد عطل DTC أو لمبة تشيك إنجن ظاهرة بجهاز الفحص؟). 3. تتبع العطل بناءً على المنطق المنهجي: ابدأ دائماً بالتحقق من دوائر التغذية الأساسية (البطارية، فيوزات المنظومة، خطوط الأرضي والريلايهات) ثم انتقل تدريجياً للاشارات والحساسات ثم الأجزاء الميكانيكية. 4. اطرح دائماً سؤالاً واحداً محدد ومختصر جداً في كل رسالة، وانتظر جواب الفني لتبني عليه الخطوة التالية (مثال: إذا قال السيارة لا تدور نهائياً، اسأله: هل أنوار الطبلون تضعف عند محاولة التشغيل أم أن السلف لا يستجيب أبداً؟). 5. اجعل لغتك مقتضبة جداً، عملية، ومباشرة كمهندس ورشة محترف دون مقدمات إنشائية.")
        }
        jsonPayloadArray.put(systemPromptObj)

        // دمج تاريخ المحادثة بالكامل ليتذكر الموديل الردود السابقة بدقة
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
                        
                        // حفظ رد الذكاء الاصطناعي في التاريخ ليعتمد عليه في الخطوة القادمة
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
