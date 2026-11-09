package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String = "",
    val email: String = "",
    @Json(name = "full_name") val fullName: String = "",
    val phone: String = "",
    @Json(name = "avatar_url") val avatarUrl: String = "",
    val role: String = "student"
)

@JsonClass(generateAdapter = true)
data class Course(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "order_index") val orderIndex: Int = 0,
    val instructor: String = "أ. أبو قاعود",
    val level: String = "المرحلة الثانوية",
    val totalLessonsCount: Int = 0,
    val isEnrolled: Boolean = false,
    val progressPercent: Int = 0
)

@JsonClass(generateAdapter = true)
data class CourseModule(
    val id: String = "",
    @Json(name = "course_id") val courseId: String = "",
    val title: String = "",
    val description: String = "",
    @Json(name = "order_index") val orderIndex: Int = 0,
    val lessons: List<Lesson> = emptyList()
)

enum class VideoType {
    YOUTUBE,
    DIRECT_URL
}

enum class LessonStatus {
    AVAILABLE,
    IN_PROGRESS,
    COMPLETED,
    LOCKED,
    EXAM_REQUIRED,
    EXAM_PASSED,
    EXAM_FAILED
}

@JsonClass(generateAdapter = true)
data class Lesson(
    val id: String = "",
    @Json(name = "module_id") val moduleId: String = "",
    @Json(name = "course_id") val courseId: String = "",
    val title: String = "",
    val description: String = "",
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "video_type") val videoType: String = "YOUTUBE",
    val duration: String = "30 دقيقة",
    val content: String = "",
    @Json(name = "is_free") val isFree: Boolean = false,
    @Json(name = "order_index") val orderIndex: Int = 0,
    val status: LessonStatus = LessonStatus.AVAILABLE,
    val relatedExamId: String? = null,
    val isExamMandatory: Boolean = false,
    val attachments: List<LessonAttachment> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LessonAttachment(
    val id: String = "",
    val title: String = "",
    @Json(name = "file_url") val fileUrl: String = "",
    @Json(name = "file_type") val fileType: String = "PDF",
    val fileSize: String = "2.4 MB"
)

enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    ESSAY
}

@JsonClass(generateAdapter = true)
data class Exam(
    val id: String = "",
    @Json(name = "course_id") val courseId: String = "",
    @Json(name = "module_id") val moduleId: String = "",
    @Json(name = "lesson_id") val lessonId: String = "",
    val title: String = "",
    val description: String = "",
    @Json(name = "time_limit") val timeLimitMinutes: Int = 30,
    @Json(name = "max_attempts") val maxAttempts: Int = 1,
    @Json(name = "passing_percentage") val passingPercentage: Int = 60,
    @Json(name = "is_mandatory") val isMandatory: Boolean = true,
    val totalMarks: Int = 100,
    val questions: List<ExamQuestion> = emptyList(),
    val remainingAttempts: Int = 1
)

@JsonClass(generateAdapter = true)
data class ExamQuestion(
    val id: String = "",
    @Json(name = "exam_id") val examId: String = "",
    @Json(name = "question_text") val questionText: String = "",
    @Json(name = "question_type") val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val options: List<String> = emptyList(),
    @Json(name = "image_url") val imageUrl: String? = null,
    val marks: Int = 10,
    @Json(name = "order_index") val orderIndex: Int = 0,
    val explanation: String? = null,
    val correctOptionIndex: Int = 0,
    val modelEssayAnswer: String? = null
)

@JsonClass(generateAdapter = true)
data class ExamAttempt(
    val id: String = "",
    @Json(name = "exam_id") val examId: String = "",
    @Json(name = "student_id") val studentId: String = "",
    @Json(name = "attempt_number") val attemptNumber: Int = 1,
    val status: String = "completed", // started, completed, abandoned
    val score: Double = 0.0,
    val totalMarks: Int = 100,
    val percentage: Double = 0.0,
    val passed: Boolean = false,
    @Json(name = "time_spent_seconds") val timeSpentSeconds: Int = 0,
    @Json(name = "started_at") val startedAt: String = "",
    @Json(name = "submitted_at") val submittedAt: String = "",
    val correctAnswersCount: Int = 0,
    val incorrectAnswersCount: Int = 0,
    val unansweredCount: Int = 0,
    val hasPendingEssays: Boolean = false,
    val examTitle: String = ""
)

@JsonClass(generateAdapter = true)
data class WalletAccount(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    val balance: Double = 0.0,
    val currency: String = "EGP"
)

@JsonClass(generateAdapter = true)
data class WalletTransaction(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    val amount: Double = 0.0,
    @Json(name = "transaction_type") val transactionType: String = "purchase", // topup, purchase
    val title: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class CashWalletSettings(
    val id: String = "default",
    @Json(name = "wallet_number") val walletNumber: String = "01000000000",
    @Json(name = "instructions") val instructions: String = "يرجى التحويل عبر فودافون كاش أو إنستاباي ثم إرفاق صورة إيصال التحويل.",
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TopupRequest(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    val amount: Double = 0.0,
    @Json(name = "proof_url") val proofUrl: String = "",
    val status: String = "pending", // pending, approved, rejected
    @Json(name = "created_at") val createdAt: String = "",
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class ForumPost(
    val id: String = "",
    @Json(name = "course_id") val courseId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "student_name") val studentName: String = "",
    @Json(name = "avatar_url") val avatarUrl: String = "",
    val title: String = "",
    val content: String = "",
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "comments_count") val commentsCount: Int = 0,
    @Json(name = "created_at") val createdAt: String = "",
    val isLikedByMe: Boolean = false,
    val comments: List<ForumComment> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ForumComment(
    val id: String = "",
    @Json(name = "post_id") val postId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "student_name") val studentName: String = "",
    val content: String = "",
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "created_at") val createdAt: String = "",
    val type: String = "general"
)

@JsonClass(generateAdapter = true)
data class AppSettings(
    val appName: String = "أبو قاعود",
    val welcomeHeadline: String = "مرحبًا بك في منصة أبو قاعود التعليمية",
    val welcomeSubheadline: String = "ابدأ رحلتك التعليمية وحقق أهدافك معنا",
    val supportPhone: String = "01000000000",
    val supportWhatsApp: String = "https://wa.me/201000000000",
    val youtubeUrl: String = "https://youtube.com/@abouqaoud",
    val facebookUrl: String = "https://facebook.com",
    val abdullahFacebookUrl: String = "https://www.facebook.com/share/1CJk9seQwC/",
    val promotionalBannerText: String = "خصم 20% لفترة محدودة على جميع باقات المراجعة النهائية!",
    val aiConfigured: Boolean = true
)
