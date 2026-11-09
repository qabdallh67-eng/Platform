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
    private val _completedLessons = MutableStateFlow<Set<String>>(setOf("lesson-1-1"))
    val completedLessons: StateFlow<Set<String>> = _completedLessons.asStateFlow()

    init {
        // Automatically start with a logged-in student state for immediate testing,
        // or support full Supabase login
        _currentStudent.value = UserProfile(
            id = "student-1",
            email = "student@abouqaoud.edu",
            fullName = "عبد الله أحمد",
            phone = "01012345678",
            avatarUrl = "",
            role = "student"
        )
    }

    // --- Authentication ---
    suspend fun login(email: String, pass: String): Result<UserProfile> {
        val result = supabaseClient.signIn(email, pass)
        return if (result.isSuccess) {
            val (profile, _) = result.getOrThrow()
            _currentStudent.value = profile
            Result.success(profile)
        } else {
            // Local fallback if offline or test mode
            val profile = UserProfile(
                id = "student-${System.currentTimeMillis() % 1000}",
                email = email,
                fullName = if (email.contains("@")) email.substringBefore("@") else "طالب منصة أبو قاعود",
                role = "student"
            )
            _currentStudent.value = profile
            Result.success(profile)
        }
    }

    suspend fun register(fullName: String, email: String, pass: String, phone: String): Result<UserProfile> {
        val result = supabaseClient.signUp(email, pass, fullName)
        return if (result.isSuccess) {
            val profile = result.getOrThrow().copy(phone = phone)
            _currentStudent.value = profile
            Result.success(profile)
        } else {
            val profile = UserProfile(
                id = "student-${System.currentTimeMillis() % 1000}",
                email = email,
                fullName = fullName,
                phone = phone,
                role = "student"
            )
            _currentStudent.value = profile
            Result.success(profile)
        }
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
                id = "course-math",
                title = "الرياضيات المتقدمة — التفاضل والتكامل",
                description = "دورة شاملة تغطي جميع أفكار النهايات، الاشتقاق وتطبيقاته، والتكامل المحدد وغير المحدد مع بنك أسئلة الوزارة ونماذج امتحانات تفاعلية.",
                price = 200.0,
                thumbnailUrl = "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=600&auto=format&fit=crop&q=80",
                instructor = "أ. أبو قاعود",
                level = "الصف الثالث الثانوي",
                totalLessonsCount = 8,
                isEnrolled = true,
                progressPercent = 35
            ),
            Course(
                id = "course-physics",
                title = "الفيزياء الحديثة والكهرومغناطيسية",
                description = "شرح وافي لقوانين كيرشوف، الحث الكهرومغناطيسي، الدوائر المترددة وازدواجية الموجة والجسيم مع تجارب محاكاة وتطبيقات عملية.",
                price = 220.0,
                thumbnailUrl = "https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?w=600&auto=format&fit=crop&q=80",
                instructor = "أ. أبو قاعود",
                level = "الصف الثالث الثانوي",
                totalLessonsCount = 10,
                isEnrolled = false,
                progressPercent = 0
            ),
            Course(
                id = "course-chem",
                title = "الكيمياء العضوية الشاملة",
                description = "إتقان تسمية وتفاعلات الهيدروكربونات، الكحولات، الفينولات، والأحماض الكربوكسيلية مع خرائط ذهنية مبتكرة لحفظ التفاعلات.",
                price = 180.0,
                thumbnailUrl = "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=600&auto=format&fit=crop&q=80",
                instructor = "أ. أبو قاعود",
                level = "الصف الثالث الثانوي",
                totalLessonsCount = 6,
                isEnrolled = false,
                progressPercent = 0
            )
        )
    }

    private fun createInitialModules(): Map<String, List<CourseModule>> {
        return mapOf(
            "course-math" to listOf(
                CourseModule(
                    id = "mod-1",
                    courseId = "course-math",
                    title = "الوحدة الأولى: نهايات الدوال وقواعد الاشتقاق",
                    description = "تأسيس قوي لمفهوم النهاية والاشتقاق الضمني والبارامتري.",
                    orderIndex = 1,
                    lessons = listOf(
                        Lesson(
                            id = "lesson-1-1",
                            moduleId = "mod-1",
                            courseId = "course-math",
                            title = "الدرس 1: مفهوم معدل التغير وقواعد الاشتقاق الأساسية",
                            description = "شرح مدخل التفاضل، المعنى الهندسي للمشتقة الأولى، وقواعد اشتقاق كثيرات الحدود والدوال الكسرية.",
                            videoUrl = "https://www.youtube.com/watch?v=17_1T09b6uY",
                            videoType = "YOUTUBE",
                            duration = "35 دقيقة",
                            isFree = true,
                            orderIndex = 1,
                            status = LessonStatus.COMPLETED
                        ),
                        Lesson(
                            id = "lesson-1-2",
                            moduleId = "mod-1",
                            courseId = "course-math",
                            title = "الدرس 2: اشتقاق الدوال المثلثية وتطبيقات السلسلة",
                            description = "قاعدة السلسلة المتسلسلة ومشتقات الجا، الجتا، والظا مع تدريبات مكثفة على مسائل المستويات العليا.",
                            videoUrl = "https://www.youtube.com/watch?v=WsQQvHm4lSw",
                            videoType = "YOUTUBE",
                            duration = "42 دقيقة",
                            isFree = false,
                            orderIndex = 2,
                            status = LessonStatus.AVAILABLE,
                            relatedExamId = "exam-math-1",
                            isExamMandatory = true,
                            attachments = listOf(
                                LessonAttachment(
                                    id = "att-1",
                                    title = "ملخص قوانين اشتقاق الدوال المثلثية.pdf",
                                    fileUrl = "https://example.com/math-summary.pdf",
                                    fileSize = "1.8 MB"
                                )
                            )
                        ),
                        Lesson(
                            id = "lesson-1-3",
                            moduleId = "mod-1",
                            courseId = "course-math",
                            title = "الدرس 3: الاشتقاق البارامتري والاشتقاق الضمني",
                            description = "التعامل مع المعادلات الوسيطية وحساب المشتقات العليا مع شروط التماس والعمودي.",
                            videoUrl = "https://www.youtube.com/watch?v=p33AXPCj264",
                            videoType = "YOUTUBE",
                            duration = "48 دقيقة",
                            isFree = false,
                            orderIndex = 3,
                            status = LessonStatus.LOCKED,
                            isExamMandatory = false
                        )
                    )
                ),
                CourseModule(
                    id = "mod-2",
                    courseId = "course-math",
                    title = "الوحدة الثانية: تطبيقات التفاضل وسلوك الدالة",
                    description = "تحديد فترات التزايد والتناقص، القيم العظمى والصغرى، ورسم المنحنيات.",
                    orderIndex = 2,
                    lessons = listOf(
                        Lesson(
                            id = "lesson-2-1",
                            moduleId = "mod-2",
                            courseId = "course-math",
                            title = "الدرس 4: فترات التزايد والتناقص والنقاط الحرجة",
                            description = "اختبار المشتقة الأولى ونقاط الانقلاب مع تطبيقات عملية على مسائل القيم القصوى.",
                            videoUrl = "https://www.youtube.com/watch?v=rfscVS0vtbw",
                            videoType = "YOUTUBE",
                            duration = "50 دقيقة",
                            isFree = false,
                            orderIndex = 4,
                            status = LessonStatus.LOCKED
                        )
                    )
                )
            ),
            "course-physics" to listOf(
                CourseModule(
                    id = "mod-phys-1",
                    courseId = "course-physics",
                    title = "الفصل الأول: التيار الكهربي وقانون أوم وقوانين كيرشوف",
                    description = "دراسة شدة التيار وفرق الجهد والمقاومة وتوصيل المقاومات.",
                    orderIndex = 1,
                    lessons = listOf(
                        Lesson(
                            id = "lesson-phys-1",
                            moduleId = "mod-phys-1",
                            courseId = "course-physics",
                            title = "الدرس 1: قانون أوم والمقاومة النوعية والتوصيلية الكهربية",
                            description = "مفهوم المقاومة وتأثير الطول والمساحة ودرجة الحرارة.",
                            videoUrl = "https://www.youtube.com/watch?v=17_1T09b6uY",
                            videoType = "YOUTUBE",
                            duration = "40 دقيقة",
                            isFree = true,
                            orderIndex = 1,
                            status = LessonStatus.AVAILABLE
                        )
                    )
                )
            )
        )
    }

    private fun createInitialExams(): Map<String, Exam> {
        return mapOf(
            "exam-math-1" to Exam(
                id = "exam-math-1",
                courseId = "course-math",
                moduleId = "mod-1",
                lessonId = "lesson-1-2",
                title = "اختبار تفاعلي: اشتقاق الدوال المثلثية وقاعدة السلسلة",
                description = "اختبار تقييمي لقياس فهم قواعد اشتقاق الدوال الدائرية. انتبه: يسمح بمحاولة واحدة فقط، وسيتم احتساب المحاولة فور بدء الاختبار.",
                timeLimitMinutes = 20,
                maxAttempts = 1, // STRICT ONE ATTEMPT POLICY
                passingPercentage = 60,
                isMandatory = true,
                totalMarks = 30,
                remainingAttempts = 1,
                questions = listOf(
                    ExamQuestion(
                        id = "q-1",
                        examId = "exam-math-1",
                        questionText = "إذا كانت ص = جا(٣س)، فإن د ص / د س تساوي:",
                        questionType = QuestionType.MULTIPLE_CHOICE,
                        options = listOf(
                            "٣ جتا(٣س)",
                            "-٣ جتا(٣س)",
                            "جتا(٣س)",
                            "٣ جا(٣س)"
                        ),
                        correctOptionIndex = 0,
                        marks = 10,
                        orderIndex = 1,
                        explanation = "مشتقة جا(٣س) = جتا(٣س) مضروبة في مشتقة ما بداخل القوس وهي ٣، الناتج ٣ جتا(٣س)."
                    ),
                    ExamQuestion(
                        id = "q-2",
                        examId = "exam-math-1",
                        questionText = "ميل المماس لمنحنى الدالة ص = ظا(س) عند س = ٠ يساوي ١.",
                        questionType = QuestionType.TRUE_FALSE,
                        options = listOf("صواب", "خطأ"),
                        correctOptionIndex = 0,
                        marks = 10,
                        orderIndex = 2,
                        explanation = "مشتقة ظا(س) هي قا²(س)، وقا(٠) = ١، إذن قا²(٠) = ١."
                    ),
                    ExamQuestion(
                        id = "q-3",
                        examId = "exam-math-1",
                        questionText = "اشرح باختصار الخطوات المتبعة لإيجاد المشتقة الأولى لدالة ضمنية س² + ص² = ٢٥ بالنسبة للمتغير س، ثم احسب ميل المماس عند النقطة (٣، ٤).",
                        questionType = QuestionType.ESSAY,
                        options = emptyList(),
                        marks = 10,
                        orderIndex = 3,
                        modelEssayAnswer = "نشتق الطرفين بالنسبة لـ س: ٢س + ٢ص (د ص / د س) = ٠. إذن د ص / د س = -س / ص. بالتعويض بالنقطة (٣، ٤) نحصل على ميل المماس = -٣/٤."
                    )
                )
            )
        )
    }

    private fun createInitialForumPosts(): Map<String, List<ForumPost>> {
        return mapOf(
            "course-math" to listOf(
                ForumPost(
                    id = "fp-1",
                    courseId = "course-math",
                    userId = "user-2",
                    studentName = "محمد إبراهيم",
                    title = "سؤال في مسألة اشتقاق خارج قسمة دالتين",
                    content = "هل يجب دائماً فك الأقواس في المقام بعد تطبيق قاعدة اشتقاق الكسر أم يمكن تركه كما هو؟",
                    createdAt = "منذ ساعتين",
                    likesCount = 5,
                    commentsCount = 2,
                    isLikedByMe = true,
                    comments = listOf(
                        ForumComment(
                            id = "c-1",
                            postId = "fp-1",
                            userId = "user-admin",
                            studentName = "أ. أبو قاعود (المشرف)",
                            content = "يفضل دائماً ترك المقام (المقام)² دون فك الأقواس إلا إذا كان هناك اختصار مباشر مع البسط.",
                            createdAt = "منذ ساعة"
                        )
                    )
                ),
                ForumPost(
                    id = "fp-2",
                    courseId = "course-math",
                    userId = "user-3",
                    studentName = "سارة محمود",
                    title = "نصيحة لمراجعة الوحدة الأولى",
                    content = "قمت بحل بنك الأسئلة المرفق في الدرس الثاني وكان مفيداً جداً خاصة مسائل المستويات العليا!",
                    createdAt = "أمس",
                    likesCount = 12,
                    commentsCount = 1
                )
            )
        )
    }
}
