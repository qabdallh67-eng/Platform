package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.EduRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Welcome : ScreenDestination()
    object Login : ScreenDestination()
    object Register : ScreenDestination()
    object Home : ScreenDestination()
    object Courses : ScreenDestination()
    data class CourseDetail(val courseId: String) : ScreenDestination()
    data class LessonPlayer(val courseId: String, val lessonId: String) : ScreenDestination()
    data class ExamStart(val examId: String) : ScreenDestination()
    data class ExamActive(val examId: String, val attemptId: String) : ScreenDestination()
    data class ExamResult(val attempt: ExamAttempt) : ScreenDestination()
    object Wallet : ScreenDestination()
    object Topup : ScreenDestination()
    data class CourseForum(val courseId: String) : ScreenDestination()
    object AiAssistant : ScreenDestination()
    object Profile : ScreenDestination()
    object Notifications : ScreenDestination()
    object ExamHistory : ScreenDestination()
}

data class AiMessage(
    val id: String = UUID(),
    val sender: String = "ai", // "user" or "ai"
    val text: String = "",
    val timestamp: String = "الآن"
) {
    companion object {
        fun UUID(): String = java.util.UUID.randomUUID().toString()
    }
}

class EduViewModel @JvmOverloads constructor(
    application: Application,
    val repository: EduRepository = EduRepository()
) : AndroidViewModel(application) {

    // Current Navigation Screen
    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Welcome)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Navigation Stack for Android BackHandler
    private val backStack = mutableListOf<ScreenDestination>()

    // Search query on courses/home
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active expanded modules in course details: set of moduleId
    private val _expandedModuleIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedModuleIds: StateFlow<Set<String>> = _expandedModuleIds.asStateFlow()

    // Exam In-Progress State
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _examAnswers = MutableStateFlow<Map<String, String>>(emptyMap())
    val examAnswers: StateFlow<Map<String, String>> = _examAnswers.asStateFlow()

    private val _examRemainingSeconds = MutableStateFlow(0)
    val examRemainingSeconds: StateFlow<Int> = _examRemainingSeconds.asStateFlow()

    private var examTimerJob: Job? = null

    // AI Assistant Chat Messages
    private val _aiMessages = MutableStateFlow<List<AiMessage>>(
        listOf(
            AiMessage(
                sender = "ai",
                text = "مرحبًا بك! أنا المساعد التعليمي الذكي لمنصة أبو قاعود. كيف يمكنني مساعدتك اليوم في استيعاب دروسك ومراجعة المواد؟"
            )
        )
    )
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // Top-level Repositories State
    val student = repository.currentStudent
    val appSettings = repository.appSettings
    val cashSettings = repository.cashSettings
    val wallet = repository.walletAccount
    val transactions = repository.transactions
    val topupRequests = repository.topupRequests
    val courses = repository.courses
    val courseModules = repository.courseModules
    val exams = repository.exams
    val examAttempts = repository.examAttempts
    val activeAttempt = repository.activeAttempt
    val forumPosts = repository.forumPosts
    val notifications = repository.notifications
    val completedLessons = repository.completedLessons

    fun navigateTo(dest: ScreenDestination) {
        val isPublicScreen = dest is ScreenDestination.Welcome ||
            dest is ScreenDestination.Login ||
            dest is ScreenDestination.Register
        if (!isPublicScreen && repository.currentStudent.value == null) {
            backStack.clear()
            _currentScreen.value = ScreenDestination.Welcome
            return
        }
        if (_currentScreen.value != dest) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = dest
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeAt(backStack.size - 1)
            _currentScreen.value = prev
            return true
        }
        return false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Toggle Module Expand/Collapse
    fun toggleModuleExpansion(moduleId: String) {
        val current = _expandedModuleIds.value.toMutableSet()
        if (current.contains(moduleId)) {
            current.remove(moduleId)
        } else {
            current.add(moduleId)
        }
        _expandedModuleIds.value = current
    }

    fun openCourse(courseId: String) {
        // Expand the first module by default
        val modules = courseModules.value[courseId]
        if (!modules.isNullOrEmpty()) {
            _expandedModuleIds.value = setOf(modules.first().id)
        }
        navigateTo(ScreenDestination.CourseDetail(courseId))
    }

    // --- Authentication Actions ---
    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.login(email, pass)
            if (res.isSuccess) {
                navigateTo(ScreenDestination.Home)
                onSuccess()
            } else {
                onError(res.exceptionOrNull()?.message ?: "فشل تسجيل الدخول")
            }
        }
    }

    fun register(name: String, email: String, pass: String, phone: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.register(name, email, pass, phone)
            if (res.isSuccess) {
                navigateTo(ScreenDestination.Home)
                onSuccess()
            } else {
                onError(res.exceptionOrNull()?.message ?: "فشل إنشاء الحساب")
            }
        }
    }

    fun logout() {
        repository.logout()
        navigateTo(ScreenDestination.Welcome)
    }

    // --- Course Purchase ---
    fun purchaseCourse(courseId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.purchaseCourse(courseId)
            if (res.isSuccess) {
                onResult(true, "تم الاشتراك في الكورس بنجاح!")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "فشلت عملية الشراء")
            }
        }
    }

    // --- Wallet Topup ---
    fun submitTopup(amount: Double, proofUri: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.submitTopup(amount, proofUri)
            if (res.isSuccess) {
                onResult(true, "تم إرسال طلب الشحن بنجاح! سيتم فحص الإيصال وإيداع الرصيد.")
                navigateTo(ScreenDestination.Wallet)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "فشل إرسال طلب الشحن")
            }
        }
    }

    // --- Exam Workflow ---
    fun startExam(examId: String) {
        val res = repository.startExam(examId)
        if (res.isSuccess) {
            val attempt = res.getOrThrow()
            val exam = exams.value[examId]
            _currentQuestionIndex.value = 0
            _examAnswers.value = emptyMap()
            _examRemainingSeconds.value = (exam?.timeLimitMinutes ?: 30) * 60

            startTimer(exam?.timeLimitMinutes ?: 30)
            navigateTo(ScreenDestination.ExamActive(examId, attempt.id))
        } else {
            Toast.makeText(getApplication(), res.exceptionOrNull()?.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun startTimer(minutes: Int) {
        examTimerJob?.cancel()
        examTimerJob = viewModelScope.launch {
            var totalSec = minutes * 60
            while (totalSec > 0) {
                delay(1000)
                totalSec--
                _examRemainingSeconds.value = totalSec
            }
            // Auto submit when time runs out!
            finishExamSubmission()
        }
    }

    fun setQuestionAnswer(questionId: String, answer: String) {
        val current = _examAnswers.value.toMutableMap()
        current[questionId] = answer
        _examAnswers.value = current
    }

    fun nextQuestion(totalQuestions: Int) {
        if (_currentQuestionIndex.value < totalQuestions - 1) {
            _currentQuestionIndex.value += 1
        }
    }

    fun prevQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
        }
    }

    fun jumpToQuestion(index: Int) {
        _currentQuestionIndex.value = index
    }

    fun finishExamSubmission() {
        examTimerJob?.cancel()
        val current = activeAttempt.value ?: return
        val exam = exams.value[current.examId] ?: return
        val totalSec = (exam.timeLimitMinutes * 60) - _examRemainingSeconds.value

        val res = repository.submitExam(
            attemptId = current.id,
            answers = _examAnswers.value,
            timeSpentSeconds = totalSec.coerceAtLeast(1)
        )
        if (res.isSuccess) {
            val completed = res.getOrThrow()
            navigateTo(ScreenDestination.ExamResult(completed))
        }
    }

    fun abandonActiveExam() {
        examTimerJob?.cancel()
        repository.cancelOrAbandonActiveExam("تم الخروج من الاختبار قبل التسليم")
        navigateTo(ScreenDestination.Home)
    }

    // --- AI Educational Assistant ---
    fun sendAiPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = AiMessage(sender = "user", text = prompt)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            delay(1200) // Simulated intelligent educational reasoning
            val replyText = generateEducationalAiResponse(prompt)
            val aiMsg = AiMessage(sender = "ai", text = replyText)
            _aiMessages.value = _aiMessages.value + aiMsg
            _isAiThinking.value = false
        }
    }

    private fun generateEducationalAiResponse(query: String): String {
        return when {
            query.contains("اشتقاق") || query.contains("تفاضل") ->
                "قواعد الاشتقاق الأساسية في منهاج الثانوية العامة تشمل:\n١. مشتقة الثابت = صفر\n٢. مشتقة س^ن = ن * س^(ن-١)\n٣. قاعدة الضرب: مشتقة الأول × الثاني + الأول × مشتقة الثاني\n٤. قاعدة القسمة: (مشتقة البسط × المقام - البسط × مشتقة المقام) / (المقام)²\n٥. قاعدة السلسلة: د ص / د س = (د ص / د ع) × (د ع / د س)."

            query.contains("فيزياء") || query.contains("كيرشوف") || query.contains("أوم") ->
                "قانونا كيرشوف هما ركيزة حل الدوائر الكهربية المعقدة:\n- القانون الأول (قانون حفظ الشحنة): مجموع التيارات الداخلة لنقطة تفرع = مجموع التيارات الخارجة منها (Σ I = 0).\n- القانون الثاني (قانون حفظ الطاقة): في أي مسار مغلق، مجموع القوى الدافعة الكهربية = مجموع فروق الجهد (Σ V_B = Σ I*R)."

            query.contains("امتحان") || query.contains("نصيحة") || query.contains("مذاكرة") ->
                "نصائح أبو قاعود للتفوق والدرجات النهائية:\n١. ابدأ بفهم النظريات والتعريفات قبل حفظ القوانين.\n٢. حل كل مسألة بيدك خطوة بخطوة ولا تكتفِ بالقراءة بالعين.\n٣. راجع أخطاء الاختبارات السابقة؛ فالخطأ الذي تتعلمه يثبت في الذاكرة.\n٤. نظّم جدولك الزمني وخذ قسطاً كافياً من الراحة قبل مواعيد الامتحانات."

            else ->
                "سؤال ممتاز في منهجك التعليمي! لتحقيق أقصى استفادة، ركز على الخطوات التفصيلية لكل مسألة، ويمكنك العودة لدرس الفيديو المخصص في المنصة أو مناقشة الفكرة في منتدى الكورس مع زملائك والأستاذ أبو قاعود."
        }
    }

    // --- External Social Links ---
    fun openExternalUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, number: String) {
        try {
            val formatted = number.replace("+", "").replace(" ", "").trim()
            val uri = Uri.parse("https://wa.me/$formatted")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تطبيق واتساب غير مثبت", Toast.LENGTH_SHORT).show()
        }
    }
}
