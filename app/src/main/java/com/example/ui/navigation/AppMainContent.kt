package com.example.ui.navigation

import android.widget.Toast

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.screens.ai.AiAssistantScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.PhoneVerificationScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.auth.WelcomeScreen
import com.example.ui.screens.course.CourseDetailScreen
import com.example.ui.screens.course.CoursesCatalogScreen
import com.example.ui.screens.course.LessonPlayerScreen
import com.example.ui.screens.exam.ExamActiveScreen
import com.example.ui.screens.exam.ExamResultScreen
import com.example.ui.screens.exam.ExamStartScreen
import com.example.ui.screens.forum.CourseForumScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.profile.ExamHistoryScreen
import com.example.ui.screens.profile.NotificationsScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.wallet.TopupScreen
import com.example.ui.screens.wallet.WalletScreen
import com.example.ui.theme.BrandPrimary
import com.example.ui.viewmodel.EduViewModel
import com.example.ui.viewmodel.ScreenDestination

@Composable
fun AppMainContent(
    viewModel: EduViewModel,
    modifier: Modifier = Modifier
) {
    // Force Arabic RTL default throughout entire app
    val context = LocalContext.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val currentScreen by viewModel.currentScreen.collectAsState()

        // Handle Android Back Button
        val canGoBack = currentScreen !is ScreenDestination.Home && currentScreen !is ScreenDestination.Welcome
        BackHandler(enabled = canGoBack) {
            viewModel.navigateBack()
        }

        val showBottomBar = currentScreen is ScreenDestination.Home ||
                currentScreen is ScreenDestination.Courses ||
                currentScreen is ScreenDestination.Wallet ||
                currentScreen is ScreenDestination.Profile

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("app_bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        // 1. الرئيسية
                        NavigationBarItem(
                            selected = currentScreen is ScreenDestination.Home,
                            onClick = { viewModel.navigateTo(ScreenDestination.Home) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                            label = { Text("الرئيسية") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPrimary,
                                selectedTextColor = BrandPrimary,
                                indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_home")
                        )

                        // 2. الكورسات
                        NavigationBarItem(
                            selected = currentScreen is ScreenDestination.Courses,
                            onClick = { viewModel.navigateTo(ScreenDestination.Courses) },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "الكورسات") },
                            label = { Text("الكورسات") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPrimary,
                                selectedTextColor = BrandPrimary,
                                indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_courses")
                        )

                        // 3. المحفظة
                        NavigationBarItem(
                            selected = currentScreen is ScreenDestination.Wallet,
                            onClick = { viewModel.navigateTo(ScreenDestination.Wallet) },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "المحفظة") },
                            label = { Text("المحفظة") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPrimary,
                                selectedTextColor = BrandPrimary,
                                indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_wallet")
                        )

                        // 4. حسابي
                        NavigationBarItem(
                            selected = currentScreen is ScreenDestination.Profile,
                            onClick = { viewModel.navigateTo(ScreenDestination.Profile) },
                            icon = { Icon(Icons.Default.Person, contentDescription = "حسابي") },
                            label = { Text("حسابي") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPrimary,
                                selectedTextColor = BrandPrimary,
                                indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_profile")
                        )
                    }
                }
            },
            modifier = modifier
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val screen = currentScreen) {
                    is ScreenDestination.Welcome -> {
                        WelcomeScreen(
                            onNavigateToLogin = { viewModel.navigateTo(ScreenDestination.Login) },
                            onNavigateToRegister = { viewModel.navigateTo(ScreenDestination.Register) }
                        )
                    }

                    is ScreenDestination.Login -> {
                        LoginScreen(
                            onLoginClick = { email, pass ->
                                viewModel.login(email, pass, onSuccess = { Toast.makeText(context, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show() }, onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() })
                            },
                            onNavigateToRegister = { viewModel.navigateTo(ScreenDestination.Register) }
                        )
                    }

                    is ScreenDestination.Register -> {
                        RegisterScreen(
                            onRegisterClick = { name, email, pass, phone ->
                                viewModel.register(name, email, pass, phone, onSuccess = { Toast.makeText(context, "تم إنشاء طلب الحساب. إذا لم يكتمل الدخول، راجع إعداد تأكيد الهاتف في لوحة Supabase.", Toast.LENGTH_LONG).show() }, onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() })
                            },
                            onNavigateToLogin = { viewModel.navigateTo(ScreenDestination.Login) }
                        )
                    }

                    is ScreenDestination.PhoneVerification -> {
                        PhoneVerificationScreen(
                            phone = viewModel.pendingPhone.value,
                            onVerifyClick = { otp ->
                                viewModel.verifyPhone(
                                    otp,
                                    onSuccess = { Toast.makeText(context, "تم تأكيد رقم الهاتف وتسجيل الدخول", Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                )
                            },
                            onBackToRegister = { viewModel.navigateTo(ScreenDestination.Register) }
                        )
                    }

                    is ScreenDestination.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToCourse = { courseId -> viewModel.openCourse(courseId) },
                            onNavigateToLesson = { cId, lId -> viewModel.navigateTo(ScreenDestination.LessonPlayer(cId, lId)) },
                            onNavigateToExam = { examId -> viewModel.navigateTo(ScreenDestination.ExamStart(examId)) },
                            onNavigateToWallet = { viewModel.navigateTo(ScreenDestination.Wallet) },
                            onNavigateToAi = { viewModel.navigateTo(ScreenDestination.AiAssistant) },
                            onNavigateToNotifications = { viewModel.navigateTo(ScreenDestination.Notifications) },
                            onNavigateToProfile = { viewModel.navigateTo(ScreenDestination.Profile) },
                            onNavigateToCoursesCatalog = { viewModel.navigateTo(ScreenDestination.Courses) }
                        )
                    }

                    is ScreenDestination.Courses -> {
                        CoursesCatalogScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() },
                            onNavigateToCourse = { courseId -> viewModel.openCourse(courseId) }
                        )
                    }

                    is ScreenDestination.CourseDetail -> {
                        CourseDetailScreen(
                            courseId = screen.courseId,
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() },
                            onNavigateToLesson = { cId, lId -> viewModel.navigateTo(ScreenDestination.LessonPlayer(cId, lId)) },
                            onNavigateToExam = { examId -> viewModel.navigateTo(ScreenDestination.ExamStart(examId)) },
                            onNavigateToForum = { cId -> viewModel.navigateTo(ScreenDestination.CourseForum(cId)) },
                            onNavigateToWallet = { viewModel.navigateTo(ScreenDestination.Wallet) }
                        )
                    }

                    is ScreenDestination.LessonPlayer -> {
                        LessonPlayerScreen(
                            courseId = screen.courseId,
                            lessonId = screen.lessonId,
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() },
                            onNavigateToExam = { examId -> viewModel.navigateTo(ScreenDestination.ExamStart(examId)) },
                            onNavigateToLesson = { cId, lId -> viewModel.navigateTo(ScreenDestination.LessonPlayer(cId, lId)) }
                        )
                    }

                    is ScreenDestination.ExamStart -> {
                        ExamStartScreen(
                            examId = screen.examId,
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() }
                        )
                    }

                    is ScreenDestination.ExamActive -> {
                        ExamActiveScreen(
                            examId = screen.examId,
                            attemptId = screen.attemptId,
                            viewModel = viewModel
                        )
                    }

                    is ScreenDestination.ExamResult -> {
                        ExamResultScreen(
                            attempt = screen.attempt,
                            viewModel = viewModel,
                            onReturnToCourse = { viewModel.navigateTo(ScreenDestination.Home) }
                        )
                    }

                    is ScreenDestination.Wallet -> {
                        WalletScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() },
                            onNavigateToTopup = { viewModel.navigateTo(ScreenDestination.Topup) }
                        )
                    }

                    is ScreenDestination.Topup -> {
                        TopupScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() }
                        )
                    }

                    is ScreenDestination.CourseForum -> {
                        CourseForumScreen(
                            courseId = screen.courseId,
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() }
                        )
                    }

                    is ScreenDestination.AiAssistant -> {
                        AiAssistantScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() }
                        )
                    }

                    is ScreenDestination.Profile -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() },
                            onNavigateToWallet = { viewModel.navigateTo(ScreenDestination.Wallet) },
                            onNavigateToNotifications = { viewModel.navigateTo(ScreenDestination.Notifications) },
                            onNavigateToExamHistory = { viewModel.navigateTo(ScreenDestination.ExamHistory) },
                            onNavigateToCourses = { viewModel.navigateTo(ScreenDestination.Courses) }
                        )
                    }

                    is ScreenDestination.Notifications -> {
                        NotificationsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() }
                        )
                    }

                    is ScreenDestination.ExamHistory -> {
                        ExamHistoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateBack() }
                        )
                    }
                }
            }
        }
    }
}
