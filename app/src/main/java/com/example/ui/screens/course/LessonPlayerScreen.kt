package com.example.ui.screens.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Lesson
import com.example.data.model.LessonAttachment
import com.example.ui.components.ScreenProtection
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    courseId: String,
    lessonId: String,
    viewModel: EduViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToExam: (String) -> Unit,
    onNavigateToLesson: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Enforce FLAG_SECURE on protected lesson content (Requirement 22)
    ScreenProtection(isProtected = true)

    val modules = viewModel.courseModules.value[courseId] ?: emptyList()
    val allLessons = modules.flatMap { it.lessons }
    val lesson = allLessons.find { it.id == lessonId } ?: allLessons.firstOrNull()
    val completedLessons = viewModel.completedLessons.value
    val isCompleted = completedLessons.contains(lessonId)

    if (lesson == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("الدرس غير متوفر")
        }
        return
    }

    val currentIndex = allLessons.indexOfFirst { it.id == lesson.id }
    val prevLesson = if (currentIndex > 0) allLessons[currentIndex - 1] else null
    val nextLesson = if (currentIndex >= 0 && currentIndex < allLessons.size - 1) allLessons[currentIndex + 1] else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("lesson_player_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الرجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Lesson Button
                    OutlinedButton(
                        onClick = {
                            prevLesson?.let { onNavigateToLesson(courseId, it.id) }
                        },
                        enabled = prevLesson != null,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الدرس السابق")
                    }

                    // Next Lesson Button
                    Button(
                        onClick = {
                            if (lesson.isExamMandatory && lesson.relatedExamId != null && !isCompleted) {
                                onNavigateToExam(lesson.relatedExamId)
                            } else {
                                nextLesson?.let { onNavigateToLesson(courseId, it.id) }
                            }
                        },
                        enabled = nextLesson != null || (lesson.relatedExamId != null && !isCompleted),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        modifier = Modifier.testTag("lesson_next_button")
                    ) {
                        Text(
                            text = if (lesson.isExamMandatory && lesson.relatedExamId != null && !isCompleted)
                                "بدء الامتحان الإلزامي"
                            else
                                "الدرس التالي",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        modifier = modifier.testTag("lesson_player_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dedicated Video Player for this lesson!
            item {
                VideoPlayerView(
                    lesson = lesson,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Lesson Title & Completion Action
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lesson.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            if (isCompleted) {
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                        Text("مكتمل", color = SuccessGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        Text(
                            text = lesson.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Mark Lesson Completed Button
                        if (!isCompleted) {
                            Button(
                                onClick = {
                                    viewModel.repository.markLessonComplete(lesson.id)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("mark_lesson_completed_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تأكيد إتمام مشاهدة الدرس", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Related Exam Card (if attached to lesson)
            if (lesson.relatedExamId != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lesson.isExamMandatory) WarningYellow.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = if (lesson.isExamMandatory) WarningYellow else BrandPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = if (lesson.isExamMandatory) "امتحان إلزامي لاجتياز الدرس" else "اختبار تقييمي للدرس",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (lesson.isExamMandatory) "يجب النجاح لفتح الدرس القادم" else "تدريب إضافي لتقييم مستواك",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { onNavigateToExam(lesson.relatedExamId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (lesson.isExamMandatory) WarningYellow else BrandPrimary
                                ),
                                modifier = Modifier.testTag("open_lesson_exam_button")
                            ) {
                                Text(
                                    text = "ابدأ الامتحان",
                                    fontWeight = FontWeight.Bold,
                                    color = if (lesson.isExamMandatory) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Lesson Attachments (PDFs / Summaries)
            if (lesson.attachments.isNotEmpty()) {
                item {
                    Text(
                        text = "ملحقات وملازم الدرس",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(lesson.attachments) { att ->
                    AttachmentRow(attachment = att)
                }
            }
        }
    }
}

@Composable
fun AttachmentRow(attachment: LessonAttachment) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ErrorRed.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                }

                Column {
                    Text(
                        text = attachment.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${attachment.fileType} • ${attachment.fileSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = {}) {
                Icon(Icons.Default.Download, contentDescription = "تحميل", tint = BrandPrimary)
            }
        }
    }
}
