package com.example.ui.screens.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Exam
import com.example.data.model.ExamQuestion
import com.example.data.model.QuestionType
import com.example.ui.components.ScreenProtection
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamActiveScreen(
    examId: String,
    attemptId: String,
    viewModel: EduViewModel,
    modifier: Modifier = Modifier
) {
    // Mandated FLAG_SECURE on exam screens (Requirement 22)
    ScreenProtection(isProtected = true)

    val exam = viewModel.exams.value[examId]
    val currentIndex = viewModel.currentQuestionIndex.value
    val answers = viewModel.examAnswers.value
    val remainingSeconds = viewModel.examRemainingSeconds.value

    var showExitDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    // Intercept back button to prevent accidental exam abandonment
    BackHandler {
        showExitDialog = true
    }

    if (exam == null || exam.questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد أسئلة في هذا الامتحان")
        }
        return
    }

    val currentQuestion = exam.questions.getOrNull(currentIndex) ?: exam.questions.first()
    val totalQuestions = exam.questions.size
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val isTimerUrgent = remainingSeconds < 180 // less than 3 minutes

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = exam.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "سؤال ${currentIndex + 1} من $totalQuestions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Countdown Timer Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isTimerUrgent) ErrorRed.copy(alpha = 0.2f) else BrandPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isTimerUrgent) ErrorRed else BrandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimerUrgent) ErrorRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Exit Button (with warning)
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.testTag("exam_exit_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "خروج", tint = Color.Gray)
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
                    // Previous Question Button
                    OutlinedButton(
                        onClick = { viewModel.prevQuestion() },
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("exam_prev_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("السابق")
                    }

                    // Next or Finish Button
                    if (currentIndex < totalQuestions - 1) {
                        Button(
                            onClick = { viewModel.nextQuestion(totalQuestions) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            modifier = Modifier.testTag("exam_next_button")
                        ) {
                            Text("التالي", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { showSubmitDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.testTag("exam_finish_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إنهاء الامتحان", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("exam_active_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Interactive Question Navigation Grid / Strip (Requirement 13)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(exam.questions) { index, q ->
                    val isAnswered = !answers[q.id].isNullOrBlank()
                    val isCurrent = index == currentIndex

                    Surface(
                        shape = CircleShape,
                        color = when {
                            isCurrent -> BrandPrimary
                            isAnswered -> SuccessGreen.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = if (isCurrent) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.jumpToQuestion(index) }
                            .testTag("question_grid_item_$index")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isCurrent -> Color.White
                                    isAnswered -> SuccessGreen
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // Question Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Question Header Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = BrandPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = when (currentQuestion.questionType) {
                                        QuestionType.MULTIPLE_CHOICE -> "اختيار من متعدد"
                                        QuestionType.TRUE_FALSE -> "صواب أو خطأ"
                                        QuestionType.ESSAY -> "سؤال مقالي"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "${currentQuestion.marks} درجات",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandSecondary
                            )
                        }

                        Text(
                            text = currentQuestion.questionText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp
                        )

                        // Optional Image in question
                        currentQuestion.imageUrl?.let { imgUrl ->
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "صورة السؤال",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Answer Options Area
                when (currentQuestion.questionType) {
                    QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE -> {
                        val selectedAns = answers[currentQuestion.id]

                        currentQuestion.options.forEachIndexed { optIndex, optionText ->
                            val isSelected = selectedAns == optionText

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) BrandPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) BrandPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.setQuestionAnswer(currentQuestion.id, optionText)
                                    }
                                    .testTag("option_${currentQuestion.id}_$optIndex")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.setQuestionAnswer(currentQuestion.id, optionText) },
                                        colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                                    )
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) BrandPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    QuestionType.ESSAY -> {
                        val currentEssayAnswer = answers[currentQuestion.id] ?: ""

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "اكتب إجابتك النموذجية باللغة العربية أدناه:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = currentEssayAnswer,
                                onValueChange = { viewModel.setQuestionAnswer(currentQuestion.id, it) },
                                placeholder = { Text("اكتب خطوات الحل بالتفصيل...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp)
                                    .testTag("essay_input_${currentQuestion.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Submission Confirmation Dialog (Requirement 13)
    if (showSubmitDialog) {
        val answeredCount = answers.size
        val unanswered = totalQuestions - answeredCount

        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            icon = { Icon(Icons.Default.Send, contentDescription = null, tint = BrandPrimary) },
            title = {
                Text("تسليم إجابات الامتحان", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هل أنت متأكد أنك تريد إنهاء الامتحان وتسليم إجاباتك؟")
                    Text("تم الإجابة عن: $answeredCount من $totalQuestions أسئلة")
                    if (unanswered > 0) {
                        Text(
                            text = "تنبيه: لديك $unanswered أسئلة غير مجابة!",
                            color = WarningYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        viewModel.finishExamSubmission()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.testTag("dialog_confirm_submit_button")
                ) {
                    Text("تسليم الامتحان", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("العودة للامتحان")
                }
            }
        )
    }

    // Exit Warning Dialog (Strict Attempt Policy - Requirement 11)
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed) },
            title = {
                Text("تحذير: سيتم احتساب المحاولة كمستهلكة", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("لقد بدأت هذا الامتحان بالفعل وتم احتساب المحاولة. إذا خرجت الآن فلن تتمكن من إعادة الامتحان تلقائيًا وستسجل المحاولة كملغاة. هل تريد الخروج بالفعل؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.abandonActiveExam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("نعم، خروج من الامتحان", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("متابعة الحل", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
