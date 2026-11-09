package com.example.data.api

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
    private var authToken: String? = null
) {
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
    }

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

    // --- Authentication ---
    suspend fun signUp(email: String, password: String, fullName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/auth/v1/signup"
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", fullName)
                    put("role", "student")
                })
            }
            val request = buildRequest(url, "POST", json.toString())
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonObj = JSONObject(body)
                    val userObj = jsonObj.optJSONObject("user") ?: jsonObj
                    val id = userObj.optString("id", "")
                    val token = jsonObj.optString("access_token", "")
                    if (token.isNotEmpty()) {
                        setAuthToken(token)
                    }
                    Result.success(
                        UserProfile(
                            id = id,
                            email = email,
                            fullName = fullName,
                            role = "student"
                        )
                    )
                } else {
                    val errMsg = parseError(body, "فشل إنشاء الحساب")
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Pair<UserProfile, String>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.PROJECT_URL}/auth/v1/token?grant_type=password"
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = buildRequest(url, "POST", json.toString())
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonObj = JSONObject(body)
                    val token = jsonObj.optString("access_token", "")
                    val userObj = jsonObj.getJSONObject("user")
                    val id = userObj.optString("id", "")
                    val meta = userObj.optJSONObject("user_metadata")
                    val fullName = meta?.optString("full_name") ?: email.substringBefore("@")
                    setAuthToken(token)
                    val profile = UserProfile(
                        id = id,
                        email = email,
                        fullName = fullName,
                        role = "student"
                    )
                    Result.success(Pair(profile, token))
                } else {
                    val errMsg = parseError(body, "البريد الإلكتروني أو كلمة المرور غير صحيحة")
                    Result.failure(Exception(errMsg))
                }
            }
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
                put("user_id", userId)
                put("amount", amount)
                put("proof_url", proofUrl)
                put("status", "pending")
            }
            val request = buildRequest(url, "POST", json.toString())
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
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
