package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseClient(
    context: Context? = null,
    private var authToken: String? = null
) {
    private val sessionPreferences = context?.applicationContext?.getSharedPreferences("abou_qaoud_auth", Context.MODE_PRIVATE)
    init { if (authToken == null) authToken = sessionPreferences?.getString("access_token", null) }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun setAuthToken(token: String?) {
        this.authToken = token
        if (token.isNullOrBlank()) sessionPreferences?.edit()?.remove("access_token")?.apply()
        else sessionPreferences?.edit()?.putString("access_token", token)?.apply()
    }

    fun hasSavedSession(): Boolean = !authToken.isNullOrBlank()

    private fun buildRequest(url: String, method: String = "GET", bodyJson: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .addHeader(SupabaseConfig.HEADER_API_KEY, SupabaseConfig.PUBLISHABLE_KEY)
            .addHeader(
                SupabaseConfig.HEADER_AUTHORIZATION,
                "Bearer ${authToken ?: SupabaseConfig.PUBLISHABLE_KEY}"
            )
            .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, "application/json")

        if (bodyJson != null && (method == "POST" || method == "PATCH" || method == "PUT")) {
            val body = bodyJson.toRequestBody(jsonMediaType)
            builder.method(method, body)
        } else if (method == "GET") {
            builder.get()
        }

        return builder.build()
    }

    // --- Phone + password authentication (email is optional profile information) ---
    private fun normalizePhone(raw: String): String {
        val compact = raw.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        val normalized = when {
            compact.startsWith("0020") -> "+${compact.drop(2)}"
            compact.startsWith("+") -> compact
            compact.startsWith("01") && compact.length == 11 -> "+20${compact.drop(1)}"
            compact.startsWith("1") && compact.length == 10 -> "+20$compact"
            else -> compact
        }
        require(Regex("^\\+[1-9][0-9]{7,14}$").matches(normalized)) {
            "اكتب رقم موبايل صحيح بصيغة مصرية مثل 01012345678."
        }
        return normalized
    }

    suspend fun signUp(phone: String, email: String, password: String, fullName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val normalizedPhone = normalizePhone(phone)
            val url = "${SupabaseConfig.PROJECT_URL}/auth/v1/signup"
            val json = JSONObject().apply {
                put("phone", normalizedPhone)
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", fullName)
                    put("phone", normalizedPhone)
                    if (email.isNotBlank()) put("email", email.trim())
                })
            }
            client.newCall(buildRequest(url, "POST", json.toString())).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@withContext Result.failure(Exception(parseError(body, "تعذر إنشاء الحساب. راجع رقم الهاتف وكلمة المرور.")))
                val auth = JSONObject(body)
                val user = auth.optJSONObject("user") ?: auth
                val id = user.optString("id")
                val token = auth.optString("access_token")
                if (id.isBlank()) return@withContext Result.failure(Exception("الخادم لم يرجع بيانات الحساب. حاول مرة أخرى."))
                if (token.isBlank()) return@withContext Result.failure(Exception("تم إرسال طلب إنشاء الحساب، لكن تأكيد الهاتف مطلوب. فعّل مزود الرسائل النصية في Supabase وأكمل تأكيد الرقم، أو عطّل تأكيد الهاتف إذا كنت تريد الدخول مباشرة."))
                setAuthToken(token)
                val profile = loadStudentProfile(id, user.optString("phone", normalizedPhone), email, fullName)
                    ?: run { setAuthToken(null); return@withContext Result.failure(Exception("تم إنشاء الحساب لكن تعذر تحميل ملف الطالب من قاعدة البيانات.")) }
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(phone: String, password: String): Result<Pair<UserProfile, String>> = withContext(Dispatchers.IO) {
        try {
            val normalizedPhone = normalizePhone(phone)
            val url = "${SupabaseConfig.PROJECT_URL}/auth/v1/token?grant_type=password"
            val json = JSONObject().apply {
                put("phone", normalizedPhone)
                put("password", password)
            }
            client.newCall(buildRequest(url, "POST", json.toString())).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@withContext Result.failure(Exception(parseError(body, "رقم الهاتف أو كلمة المرور غير صحيحة.")))
                val auth = JSONObject(body)
                val user = auth.optJSONObject("user") ?: JSONObject()
                val id = user.optString("id")
                val token = auth.optString("access_token")
                if (id.isBlank() || token.isBlank()) return@withContext Result.failure(Exception("لم يرجع خادم تسجيل الدخول جلسة صالحة."))
                setAuthToken(token)
                val meta = user.optJSONObject("user_metadata")
                val fallbackName = meta?.optString("full_name")?.takeIf { it.isNotBlank() } ?: normalizedPhone
                val profile = loadStudentProfile(id, user.optString("phone", normalizedPhone), meta?.optString("email").orEmpty(), fallbackName)
                if (profile == null) {
                    setAuthToken(null)
                    return@withContext Result.failure(Exception("تم التحقق من الدخول لكن ملف الطالب غير موجود في قاعدة البيانات. تواصل مع الإدارة."))
                }
                if (profile.role != "student") {
                    setAuthToken(null)
                    return@withContext Result.failure(Exception("هذا التطبيق مخصص لحسابات الطلاب فقط."))
                }
                Result.success(Pair(profile, token))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadStudentProfile(id: String, phone: String, fallbackEmail: String, fallbackName: String): UserProfile? {
        val url = "${SupabaseConfig.PROJECT_URL}/rest/v1/profiles?id=eq.$id&select=id,full_name,phone,email,role&limit=1"
        client.newCall(buildRequest(url)).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException(parseError(body, "تعذر تحميل بيانات حسابك من قاعدة البيانات."))
            val rows = JSONArray(body)
            if (rows.length() == 0) return null
            val row = rows.getJSONObject(0)
            return UserProfile(
                id = id,
                email = row.optString("email").ifBlank { fallbackEmail },
                fullName = row.optString("full_name").ifBlank { fallbackName },
                phone = row.optString("phone").ifBlank { phone },
                role = row.optString("role", "student")
            )
        }
    }

    // --- Session restoration ---
    suspend fun restoreSession(): Result<UserProfile?> = withContext(Dispatchers.IO) {
        val token = authToken
        if (token.isNullOrBlank()) return@withContext Result.success(null)
        try {
            val response = client.newCall(buildRequest("${SupabaseConfig.PROJECT_URL}/auth/v1/user")).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                setAuthToken(null)
                return@withContext Result.success(null)
            }
            val user = JSONObject(body)
            val id = user.optString("id")
            val meta = user.optJSONObject("user_metadata")
            val phone = user.optString("phone").ifBlank { meta?.optString("phone").orEmpty() }
            if (id.isBlank() || phone.isBlank()) {
                setAuthToken(null)
                return@withContext Result.success(null)
            }
            val fallbackName = meta?.optString("full_name")?.takeIf { it.isNotBlank() } ?: phone
            val profile = loadStudentProfile(id, phone, meta?.optString("email").orEmpty(), fallbackName)
            if (profile == null || profile.role != "student") {
                setAuthToken(null)
                return@withContext Result.success(null)
            }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Courses Query ---
    suspend fun getCourses(): Result<List<Course>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/rest/v1/courses?select=*&order=order_index.asc"
            val request = buildRequest(url)
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "[]"
                if (response.isSuccessful) {
                    val listType = Types.newParameterizedType(List::class.java, Course::class.java)
                    val adapter = moshi.adapter<List<Course>>(listType)
                    val list = adapter.fromJson(body) ?: emptyList()
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // --- Student wallet data ---
    suspend fun getWalletAccount(studentId: String): Result<WalletAccount?> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/rest/v1/wallet_accounts?student_id=eq.$studentId&select=id,student_id,balance,currency&limit=1"
            client.newCall(buildRequest(url)).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) Result.failure(Exception(parseError(body, "تعذر تحميل رصيد المحفظة")))
                else {
                    val rows = JSONArray(body)
                    if (rows.length() == 0) Result.success(null)
                    else {
                        val row = rows.getJSONObject(0)
                        Result.success(WalletAccount(row.optString("id"), row.optString("student_id"), row.optDouble("balance", 0.0), row.optString("currency", "EGP")))
                    }
                }
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getWalletTransactions(studentId: String): Result<List<WalletTransaction>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/rest/v1/wallet_transactions?student_id=eq.$studentId&select=id,student_id,amount,transaction_type,created_at,description&order=created_at.desc"
            client.newCall(buildRequest(url)).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) Result.failure(Exception(parseError(body, "تعذر تحميل سجل المحفظة")))
                else {
                    val rows = JSONArray(body)
                    val items = (0 until rows.length()).map { i ->
                        val row = rows.getJSONObject(i)
                        WalletTransaction(id = row.optString("id"), userId = row.optString("student_id"), amount = row.optDouble("amount", 0.0), transactionType = row.optString("transaction_type"), title = row.optString("description"), createdAt = row.optString("created_at"))
                    }
                    Result.success(items)
                }
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getTopupRequests(studentId: String): Result<List<TopupRequest>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/rest/v1/topup_requests?student_id=eq.$studentId&select=id,student_id,amount,proof_path,status,created_at,admin_note&order=created_at.desc"
            client.newCall(buildRequest(url)).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) Result.failure(Exception(parseError(body, "تعذر تحميل طلبات الشحن")))
                else {
                    val rows = JSONArray(body)
                    val items = (0 until rows.length()).map { i ->
                        val row = rows.getJSONObject(i)
                        TopupRequest(id = row.optString("id"), userId = row.optString("student_id"), amount = row.optDouble("amount", 0.0), proofUrl = row.optString("proof_path"), status = row.optString("status"), createdAt = row.optString("created_at"), notes = row.optString("admin_note"))
                    }
                    Result.success(items)
                }
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- Cash Wallet Settings ---
    suspend fun getCashWalletSettings(): Result<CashWalletSettings> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/rest/v1/cash_wallet_settings?select=*&limit=1"
            val request = buildRequest(url)
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "[]"
                if (response.isSuccessful) {
                    val listType = Types.newParameterizedType(List::class.java, CashWalletSettings::class.java)
                    val adapter = moshi.adapter<List<CashWalletSettings>>(listType)
                    val list = adapter.fromJson(body) ?: emptyList()
                    if (list.isNotEmpty()) {
                        Result.success(list.first())
                    } else {
                        Result.success(CashWalletSettings())
                    }
                } else {
                    Result.success(CashWalletSettings())
                }
            }
        } catch (e: Exception) {
            Result.success(CashWalletSettings())
        }
    }

    // --- Topup Request ---
    suspend fun submitTopupRequest(userId: String, amount: Double, proofUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/rest/v1/topup_requests"
            val json = JSONObject().apply {
                put("student_id", userId)
                put("amount", amount)
                if (proofUrl.isNotBlank()) put("proof_path", proofUrl)
            }
            val request = buildRequest(url, "POST", json.toString())
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) Result.success(true)
                else Result.failure(Exception(parseError(body, "تعذر إرسال طلب الشحن إلى قاعدة البيانات")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Purchase RPC ---
    suspend fun purchaseCourseRpc(courseId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/rest/v1/rpc/purchase_course_with_wallet"
            val json = JSONObject().apply {
                put("p_course_id", courseId)
            }
            val request = buildRequest(url, "POST", json.toString())
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val body = response.body?.string() ?: ""
                    Result.failure(Exception(parseError(body, "رصيد المحفظة غير كافٍ أو حدث خطأ أثناء الشراء")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Exam Attempt Recording ---
    suspend fun submitExamAttempt(attempt: ExamAttempt): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/rest/v1/exam_attempts"
            val json = JSONObject().apply {
                put("exam_id", attempt.examId)
                put("student_id", attempt.studentId)
                put("attempt_number", attempt.attemptNumber)
                put("score", attempt.score)
                put("percentage", attempt.percentage)
                put("passed", attempt.passed)
                put("status", attempt.status)
                put("time_spent_seconds", attempt.timeSpentSeconds)
            }
            val request = buildRequest(url, "POST", json.toString())
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseError(body: String, defaultMsg: String): String {
        return try {
            val obj = JSONObject(body)
            obj.optString("msg", obj.optString("message", obj.optString("error_description", defaultMsg)))
        } catch (_: Exception) {
            defaultMsg
        }
    }
}
