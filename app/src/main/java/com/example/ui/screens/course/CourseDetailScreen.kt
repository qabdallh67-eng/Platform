package com.example.ui.screens.course

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.AppHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    viewModel: EduViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (String, String) -> Unit,
    onNavigateToExam: (String) -> Unit,
    onNavigateToForum: (String) -> Unit,
    onNavigateToWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val courses = viewModel.courses.value
    val course = courses.find { it.id == courseId } ?: courses.firstOrNull()
    val modules = viewModel.courseModules.value[courseId] ?: emptyList()
    val expandedModuleIds = viewModel.expandedModuleIds.value
    val completedLessons = viewModel.completedLessons.value
    val wallet = viewModel.wallet.value

    var showPurchaseDialog by remember { mutableStateOf(false) }
    var purchaseMessage by remember { mutableStateOf<String?>(null) }

    if (course == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("الكورس غير متوفر")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("course_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                actions = {
                    if (course.isEnrolled) {
                        IconButton(
                            onClick = { onNavigateToForum(course.id) },
                            modifier = Modifier.testTag("course_forum_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "منتدى الكورس",
                                tint = BrandPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!course.isEnrolled) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "سعر الكورس الكامل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${course.price.toInt()} ج.م",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandSecondary
                            )
                        }

                        Button(
                            onClick = { showPurchaseDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("course_purchase_button")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الاشتراك بالمحفظة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("course_detail_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Course Banner & Info
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = course.thumbnailUrl,
                            contentDescription = course.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (course.isEnrolled) SuccessGreen.copy(alpha = 0.15f) else BrandPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (course.isEnrolled) "أنت مشترك في هذا الكورس" else "${course.price.toInt()} ج.م",
                                        color = if (course.isEnrolled) SuccessGreen else BrandPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = course.level,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Text(
                                text = course.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "المدرس: ${course.instructor}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (course.isEnrolled) {
                                Button(
                                    onClick = { onNavigateToForum(course.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Forum, contentDescription = null, tint = BrandPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("منتدى نقاش الكورس للطلاب", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Modules & Lessons Header
            item {
                Text(
                    text = "خطة الدروس والوحدات التعليمية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Accordion Modules List (Requirement 8)
            items(modules) { module ->
                val isExpanded = expandedModuleIds.contains(module.id)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("module_card_${module.id}")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Module Accordion Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.toggleModuleExpansion(module.id) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${module.orderIndex}",
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPrimary
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = module.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${module.lessons.size} دروس تعليمية",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Chevron expand/collapse arrow
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "طي" else "توسيع",
                                tint = BrandPrimary
                            )
                        }

                        // Expanded Lessons List
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 1.dp
                                )

                                module.lessons.forEachIndexed { index, lesson ->
                                    val isUnlocked = viewModel.repository.isLessonUnlocked(lesson, module.lessons)
                                    val isCompleted = completedLessons.contains(lesson.id)

                                    LessonItemRow(
                                        lesson = lesson,
                                        lessonNumber = index + 1,
                                        isUnlocked = isUnlocked,
                                        isCompleted = isCompleted,
                                        onClick = {
                                            if (isUnlocked) {
                                                onNavigateToLesson(course.id, lesson.id)
                                            }
                                        },
                                        onExamClick = {
                                            lesson.relatedExamId?.let { onNavigateToExam(it) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Purchase Course Confirmation Dialog
    if (showPurchaseDialog) {
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = {
                Text("تأكيد شراء الكورس بالمحفظة", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سعر الكورس: ${course.price.toInt()} ج.م")
                    Text("رصيدك الحالي بالمحفظة: ${wallet.balance.toInt()} ج.م")
                    if (wallet.balance < course.price) {
                        Text(
                            text = "تنبيه: رصيدك غير كافٍ. يرجى شحن المحفظة أولاً.",
                            color = ErrorRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "سيتم خصم ${course.price.toInt()} ج.م وتفعيل جميع دروس وامتحانات الكورس فوراً.",
                            color = SuccessGreen
                        )
                    }
                }
            },
            confirmButton = {
                if (wallet.balance >= course.price) {
                    Button(
                        onClick = {
                            showPurchaseDialog = false
                            viewModel.purchaseCourse(course.id) { success, msg ->
                                purchaseMessage = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text("تأكيد الخصم والاشتراك", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            showPurchaseDialog = false
                            onNavigateToWallet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandSecondary)
                    ) {
                        Text("شحن المحفظة الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Purchase Result Alert
    purchaseMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { purchaseMessage = null },
            title = { Text("معلومات الشراء") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { purchaseMessage = null }) {
                    Text("حسنًا")
                }
            }
        )
    }
}

@Composable
fun LessonItemRow(
    lesson: Lesson,
    lessonNumber: Int,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onExamClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isUnlocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isUnlocked) { onClick() }
            .testTag("lesson_row_${lesson.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Status Icon
                val statusIcon = when {
                    isCompleted -> Icons.Default.CheckCircle
                    !isUnlocked -> Icons.Default.Lock
                    else -> Icons.Default.PlayCircleOutline
                }
                val iconTint = when {
                    isCompleted -> SuccessGreen
                    !isUnlocked -> Color.Gray
                    else -> BrandPrimary
                }

                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )

                Column {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lesson.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (lesson.isFree) {
                            Text(
                                text = "مجاني للتجربة",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Related Exam Button if exists
            if (lesson.relatedExamId != null && isUnlocked) {
                OutlinedButton(
                    onClick = onExamClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("lesson_exam_button_${lesson.id}")
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lesson.isExamMandatory) "امتحان إلزامي" else "امتحان اختياري",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (lesson.isExamMandatory) WarningYellow else BrandPrimary
                    )
                }
            }
        }
    }
}
