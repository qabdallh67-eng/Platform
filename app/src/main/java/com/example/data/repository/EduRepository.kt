package com.example.data.repository

import com.example.data.api.SupabaseClient
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class EduRepository(
    val supabaseClient: SupabaseClient = SupabaseClient()
) {
    // Current Student Profile
    private val _currentStudent = MutableStateFlow<UserProfile?>(null)
    val currentStudent: StateFlow<UserProfile?> = _currentStudent.asStateFlow()

    // App Settings
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // Cash Wallet Settings
    private val _cashSettings = MutableStateFlow(
        CashWalletSettings(
            walletNumber = "01098765432",
            instructions = "قم بالتحويل على رقم فودافون كاش أعلاه، ثم خذ لقطة شاشة للإيصال وارفعها في نموذج الشحن ليتم تأكيد العملية وإيداع الرصيد."
        )
    )
    val cashSettings: StateFlow<CashWalletSettings> = _cashSettings.asStateFlow()

    // Student Wallet
    private val _walletAccount = MutableStateFlow(
        WalletAccount(
            userId = "student-1",
            balance = 350.0,
            currency = "EGP"
        )
    )
    val walletAccount: StateFlow<WalletAccount> = _walletAccount.asStateFlow()

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(
        listOf(
            WalletTransaction(
                id = "tx-1",
                userId = "student-1",
                amount = 350.0,
                transactionType = "topup",
                title = "شحن محفظة كاش",
                createdAt = "2026-11-08 14:30",
                notes = "تمت الموافقة من الإدارة"
            )
        )
    )
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    private val _topupRequests = MutableStateFlow<List<TopupRequest>>(emptyList())
    val topupRequests: StateFlow<List<TopupRequest>> = _topupRequests.asStateFlow()

    // Courses List
    private val _courses = MutableStateFlow<List<Course>>(createInitialCourses())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    // Course Modules & Lessons Map: courseId -> List<CourseModule>
    private val _courseModules = MutableStateFlow<Map<String, List<CourseModule>>>(createInitialModules())
    val courseModules: StateFlow<Map<String, List<CourseModule>>> = _courseModules.asStateFlow()

    // Exams Map: examId -> Exam
    private val _exams = MutableStateFlow<Map<String, Exam>>(createInitialExams())
    val exams: StateFlow<Map<String, Exam>> = _exams.asStateFlow()

    // Exam Attempts List
    private val _examAttempts = MutableStateFlow<List<ExamAttempt>>(emptyList())
    val examAttempts: StateFlow<List<ExamAttempt>> = _examAttempts.asStateFlow()

    // Active Attempt (if student is currently in an exam)
    private val _activeAttempt = MutableStateFlow<ExamAttempt?>(null)
    val activeAttempt: StateFlow<ExamAttempt?> = _activeAttempt.asStateFlow()

    // Course Forums: courseId -> List<ForumPost>
    private val _forumPosts = MutableStateFlow<Map<String, List<ForumPost>>>(createInitialForumPosts())
    val forumPosts: StateFlow<Map<String, List<ForumPost>>> = _forumPosts.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(
        listOf(
            NotificationItem(
                id = "notif-1",
                title = "مرحبًا بك في منصة أبو قاعود!",
                message = "نتمنى لك رحلة تعليمية موفقة وممتعة مع نخبة الدروس والامتحانات التفاعلية.",
                createdAt = "منذ قليل",
                type = "welcome"
            ),
            NotificationItem(
                id = "notif-2",
                title = "تم إضافة امتحان جديد",
                message = "تمت إضافة اختبار تقييمي لوحدة التفاضل والتكامل في كورس الرياضيات.",
                createdAt = "اليوم",
                type = "exam"
            )
        )
    )
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Lesson Progress: lessonId -> Boolean (isCompleted)
    private val _completedLessons = MutableStateFlow<Set<String>>(emptySet())
    val completedLessons: StateFlow<Set<String>> = _completedLessons.asStateFlow()

    // --- Authentication ---
    suspend fun login(email: String, pass: String): Result<UserProfile> {
        val result = supabaseClient.signIn(email, pass)
        return result.fold(
            onSuccess = { (profile, _) ->
                _currentStudent.value = profile
                Result.success(profile)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun register(fullName: String, email: String, pass: String, phone: String): Result<UserProfile> {
        val result = supabaseClient.signUp(email, pass, fullName)
        return result.fold(
            onSuccess = { profile ->
                val registeredStudent = profile.copy(phone = phone)
                _currentStudent.value = registeredStudent
                Result.success(registeredStudent)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    fun logout() {
        _currentStudent.value = null
        supabaseClient.setAuthToken(null)
    }

    // --- Course Enrollment & Purchase ---
    suspend fun purchaseCourse(courseId: String): Result<Boolean> {
        val course = _courses.value.find { it.id == courseId }
            ?: return Result.failure(Exception("الكورس غير موجود"))

        if (course.isEnrolled) {
            return Result.success(true)
        }

        val currentBal = _walletAccount.value.balance
        if (currentBal < course.price) {
            return Result.failure(Exception("رصيد المحفظة غير كافٍ. المطلوب: ${course.price} ج.م، رصيدك الحالي: $currentBal ج.م"))
        }

        // Try RPC on Supabase
        val rpcResult = supabaseClient.purchaseCourseRpc(courseId)

        // Deduct from wallet and mark course enrolled
        val newBal = currentBal - course.price
        _walletAccount.value = _walletAccount.value.copy(balance = newBal)

        val newTx = WalletTransaction(
            id = "tx-${System.currentTimeMillis()}",
            userId = _currentStudent.value?.id ?: "student-1",
            amount = -course.price,
            transactionType = "purchase",
            title = "شراء ${course.title}",
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            notes = "تم الخصم من الرصيد والاشتراك بالكورس"
        )
        _transactions.value = listOf(newTx) + _transactions.value

        _courses.value = _courses.value.map {
            if (it.id == courseId) it.copy(isEnrolled = true) else it
        }

        return Result.success(true)
    }

    // --- Wallet Top-up ---
    suspend fun submitTopup(amount: Double, proofUri: String): Result<Boolean> {
        if (amount <= 0) return Result.failure(Exception("يرجى إدخال مبلغ صحيح"))
        val userId = _currentStudent.value?.id ?: "student-1"
        val req = TopupRequest(
            id = "topup-${System.currentTimeMillis()}",
            userId = userId,
            amount = amount,
            proofUrl = proofUri,
            status = "pending",
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            notes = "طلب شحن قيد مراجعة الإدارة"
        )
        _topupRequests.value = listOf(req) + _topupRequests.value
        supabaseClient.submitTopupRequest(userId, amount, proofUri)
        return Result.success(true)
    }

    // --- Lesson Progression ---
    fun markLessonComplete(lessonId: String) {
        val updated = _completedLessons.value.toMutableSet()
        updated.add(lessonId)
        _completedLessons.value = updated
    }

    fun isLessonUnlocked(lesson: Lesson, allLessonsInModule: List<Lesson>): Boolean {
        if (lesson.isFree) return true
        val course = _courses.value.find { it.id == lesson.courseId }
        if (course?.isEnrolled != true) return false

        val index = allLessonsInModule.indexOfFirst { it.id == lesson.id }
        if (index <= 0) return true // First lesson is available once enrolled

        val previousLesson = allLessonsInModule[index - 1]
        val isPrevCompleted = _completedLessons.value.contains(previousLesson.id)
        if (!isPrevCompleted) return false

        // If previous lesson had a mandatory exam, verify exam was passed
        if (previousLesson.isExamMandatory && previousLesson.relatedExamId != null) {
            val passedExam = _examAttempts.value.any {
                it.examId == previousLesson.relatedExamId && it.passed
            }
            return passedExam
        }

        return true
    }

    // --- Exam Engine with Strict One-Attempt Policy ---
    fun startExam(examId: String): Result<ExamAttempt> {
        val exam = _exams.value[examId] ?: return Result.failure(Exception("الامتحان غير موجود"))

        // Count previous attempts
        val pastAttempts = _examAttempts.value.filter { it.examId == examId }
        if (pastAttempts.size >= exam.maxAttempts) {
            return Result.failure(Exception("لقد استنفدت جميع المحاولات المتاحة لهذا الامتحان (${exam.maxAttempts}/${exam.maxAttempts}). لا يمكنك بدء محاولة جديدة إلا بعد مراجعة الإدارة."))
        }

        // Consume attempt immediately!
        val newAttemptNumber = pastAttempts.size + 1
        val attempt = ExamAttempt(
            id = "attempt-${UUID.randomUUID()}",
            examId = examId,
            studentId = _currentStudent.value?.id ?: "student-1",
            attemptNumber = newAttemptNumber,
            status = "started",
            startedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            examTitle = exam.title,
            totalMarks = exam.totalMarks
        )

        _activeAttempt.value = attempt
        _examAttempts.value = _examAttempts.value + attempt
        return Result.success(attempt)
    }

    fun submitExam(
        attemptId: String,
        answers: Map<String, String>, // questionId -> selectedOption or essay text
        timeSpentSeconds: Int
    ): Result<ExamAttempt> {
        val current = _activeAttempt.value
        val exam = _exams.value[current?.examId ?: ""] ?: return Result.failure(Exception("الامتحان غير موجود"))

        var correctCount = 0
        var incorrectCount = 0
        var unansweredCount = 0
        var score = 0.0
        var hasEssays = false

        for (q in exam.questions) {
            val ans = answers[q.id]
            if (ans.isNullOrBlank()) {
                unansweredCount++
            } else {
                when (q.questionType) {
                    QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE -> {
                        val correctText = q.options.getOrNull(q.correctOptionIndex)
                        if (ans == correctText) {
                            correctCount++
                            score += q.marks
                        } else {
                            incorrectCount++
                        }
                    }
                    QuestionType.ESSAY -> {
                        hasEssays = true
                        // Basic essay presence grants provisional points; final grade reviewed
                        score += (q.marks * 0.7)
                    }
                }
            }
        }

        val percentage = (score / exam.totalMarks.coerceAtLeast(1)) * 100.0
        val isPassed = percentage >= exam.passingPercentage

        val completedAttempt = current?.copy(
            status = "completed",
            score = score,
            percentage = percentage,
            passed = isPassed,
            timeSpentSeconds = timeSpentSeconds,
            submittedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            correctAnswersCount = correctCount,
            incorrectAnswersCount = incorrectCount,
            unansweredCount = unansweredCount,
            hasPendingEssays = hasEssays
        ) ?: ExamAttempt(id = attemptId, examId = exam.id, passed = isPassed, percentage = percentage)

        _activeAttempt.value = null
        _examAttempts.value = _examAttempts.value.map {
            if (it.id == completedAttempt.id) completedAttempt else it
        }

        // If exam is passed and linked to a lesson, unlock next progression
        if (isPassed && exam.lessonId.isNotEmpty()) {
            markLessonComplete(exam.lessonId)
        }

        return Result.success(completedAttempt)
    }

    fun cancelOrAbandonActiveExam(reason: String) {
        val current = _activeAttempt.value ?: return
        val abandoned = current.copy(
            status = "abandoned",
            submittedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        _activeAttempt.value = null
        _examAttempts.value = _examAttempts.value.map {
            if (it.id == abandoned.id) abandoned else it
        }
    }

    // --- Forum Posts ---
    fun addForumPost(courseId: String, title: String, content: String): ForumPost {
        val student = _currentStudent.value
        val newPost = ForumPost(
            id = "post-${System.currentTimeMillis()}",
            courseId = courseId,
            userId = student?.id ?: "student-1",
            studentName = student?.fullName ?: "طالب المنصة",
            title = title,
            content = content,
            createdAt = "الآن",
            likesCount = 0,
            commentsCount = 0
        )
        val currentMap = _forumPosts.value.toMutableMap()
        val list = currentMap[courseId] ?: emptyList()
        currentMap[courseId] = listOf(newPost) + list
        _forumPosts.value = currentMap
        return newPost
    }

    fun addForumComment(courseId: String, postId: String, commentText: String) {
        val student = _currentStudent.value
        val comment = ForumComment(
            id = "c-${System.currentTimeMillis()}",
            postId = postId,
            userId = student?.id ?: "student-1",
            studentName = student?.fullName ?: "طالب المنصة",
            content = commentText,
            createdAt = "الآن"
        )
        val currentMap = _forumPosts.value.toMutableMap()
        val list = (currentMap[courseId] ?: emptyList()).map { post ->
            if (post.id == postId) {
                post.copy(
                    commentsCount = post.commentsCount + 1,
                    comments = post.comments + comment
                )
            } else post
        }
        currentMap[courseId] = list
        _forumPosts.value = currentMap
    }

    fun toggleLikeForumPost(courseId: String, postId: String) {
        val currentMap = _forumPosts.value.toMutableMap()
        val list = (currentMap[courseId] ?: emptyList()).map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLikedByMe
                val newCount = if (newLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                post.copy(isLikedByMe = newLiked, likesCount = newCount)
            } else post
        }
        currentMap[courseId] = list
        _forumPosts.value = currentMap
    }

    // --- Pre-seeded Educational Data for Abou Qaoud Platform ---
    private fun createInitialCourses(): List<Course> {
        return listOf(
            Course(
                id = "course-abdullah",
                title = "عبد الله",
                description = "كورس عبد الله التعليمي.",
                price = 0.0,
                thumbnailUrl = "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=900&auto=format&fit=crop&q=85",
                isPublished = true,
                orderIndex = 1,
                instructor = "أ. أبو قاعود",
                level = "جميع المراحل",
                totalLessonsCount = 0,
                isEnrolled = true,
                progressPercent = 0
            )
        )
    }

    private fun createInitialModules(): Map<String, List<CourseModule>> {
        return emptyMap()
    }

    private fun createInitialExams(): Map<String, Exam> {
        return emptyMap()
    }

    private fun createInitialForumPosts(): Map<String, List<ForumPost>> {
        return emptyMap()
    }
}
