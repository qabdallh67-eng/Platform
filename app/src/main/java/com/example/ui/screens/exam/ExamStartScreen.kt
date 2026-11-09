package com.example.ui.screens.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Exam
import com.example.ui.components.ScreenProtection
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamStartScreen(
    examId: String,
    viewModel: EduViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Screen protection
    ScreenProtection(isProtected = true)

    val exam = viewModel.exams.value[examId]
    val attempts = viewModel.examAttempts.value.filter { it.examId == examId }
    var showStartConfirmDialog by remember { mutableStateOf(false) }

    if (exam == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("الامتحان غير متوفر")
        }
        return
    }

    val usedAttempts = attempts.size
    val remainingAttempts = (exam.maxAttempts - usedAttempts).coerceAtLeast(0)
    val canAttempt = remainingAttempts > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل الاختبار", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("exam_start_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الرجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("exam_start_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Quiz, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(32.dp))
                        }
                    }

                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = exam.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Exam Info Grid
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ExamInfoRow(label = "مدة الامتحان", value = "${exam.timeLimitMinutes} دقيقة", icon = Icons.Default.Timer)
                    ExamInfoRow(label = "عدد الأسئلة", value = "${exam.questions.size} أسئلة", icon = Icons.Default.FormatListNumbered)
                    ExamInfoRow(label = "الدرجة الكلية", value = "${exam.totalMarks} درجة", icon = Icons.Default.Grade)
                    ExamInfoRow(label = "نسبة النجاح المطلوبة", value = "${exam.passingPercentage}%", icon = Icons.Default.Percent)
                    ExamInfoRow(
                        label = "المحاولات المتاحة",
                        value = "المحاولات المتاحة: $remainingAttempts من ${exam.maxAttempts}",
                        icon = Icons.Default.Replay,
                        isHighlight = true
                    )
                }
            }

            // Strict Policy Notice
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = WarningYellow.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تنبيه هام للطلاب:\nفور الضغط على 'ابدأ الامتحان' سيتم احتساب المحاولة رسميًا. الخروج من شاشة الامتحان أو إغلاق التطبيق سيؤدي لاعتبار المحاولة منتهية ولا يمكن إعادتها تلقائياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start Exam Button
            Button(
                onClick = {
                    if (canAttempt) {
                        showStartConfirmDialog = true
                    }
                },
                enabled = canAttempt,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_exam_button")
            ) {
                Text(
                    text = if (canAttempt) "ابدأ الامتحان" else "استنفدت جميع المحاولات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    // Confirmation Dialog before starting (strict policy)
    if (showStartConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStartConfirmDialog = false },
            icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = BrandPrimary) },
            title = { Text("تأكيد بدء الاختبار الآن", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت مستعد لبدء الامتحان؟ سيتم تشغيل المؤقت الزمني (${exam.timeLimitMinutes} دقيقة) واحتساب المحاولة فوراً.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartConfirmDialog = false
                        viewModel.startExam(exam.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("نعم، ابدأ الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartConfirmDialog = false }) {
                    Text("العودة")
                }
            }
        )
    }
}

@Composable
fun ExamInfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isHighlight) BrandSecondary else BrandPrimary, modifier = Modifier.size(18.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) BrandSecondary else MaterialTheme.colorScheme.onSurface
        )
    }
}
