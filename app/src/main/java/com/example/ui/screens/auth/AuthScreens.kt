package com.example.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(24.dp)
            .testTag("welcome_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Official Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo_new),
                contentDescription = "شعار أبو قاعود",
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, BrandPrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "أبو قاعود",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "مرحبًا بك في منصة أبو قاعود التعليمية",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "ابدأ رحلتك التعليمية وحقق أهدافك معنا\nمنصة حصرية ومتطورة للطلاب بأعلى معايير الجودة",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Login Button
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_login_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text(
                    text = "تسجيل الدخول",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Register Button
            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_register_button"),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(BrandPrimary, BrandSecondary))
                )
            ) {
                Text(
                    text = "إنشاء حساب جديد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("login_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo_new),
            contentDescription = "شعار أبو قاعود",
            modifier = Modifier.size(90.dp).clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("تسجيل الدخول", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("ادخل برقم موبايلك، أو بريد الحساب القديم، وكلمة المرور", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; errorText = "" },
            label = { Text("رقم الموبايل أو البريد الإلكتروني") },
            placeholder = { Text("01012345678") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BrandPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("login_phone_input"),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorText = "" },
            label = { Text("كلمة المرور") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandPrimary) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "إظهار/إخفاء كلمة المرور")
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
            shape = RoundedCornerShape(12.dp)
        )
        if (errorText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (phone.filter { it.isDigit() }.length !in 10..15 && !(phone.contains("@") && phone.substringAfter("@").contains("."))) errorText = "اكتب رقم موبايل صحيح أو بريد الحساب القديم."
                else if (password.isBlank()) errorText = "اكتب كلمة المرور."
                else onLoginClick(phone.trim(), password)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("login_submit_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
        ) {
            Text("تسجيل الدخول", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.clickable { onNavigateToRegister() }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ليس لديك حساب؟", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("إنشاء حساب جديد", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BrandPrimary)
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("register_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo_new),
            contentDescription = "شعار أبو قاعود",
            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("إنشاء حساب طالب جديد", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("رقم الموبايل أساسي، والبريد الإلكتروني اختياري", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; errorText = "" },
            label = { Text("الاسم الكامل") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; errorText = "" },
            label = { Text("رقم الموبايل") },
            placeholder = { Text("01012345678") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BrandPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("register_phone_input"),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorText = "" },
            label = { Text("البريد الإلكتروني (اختياري)") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("register_email_input"),
            shape = RoundedCornerShape(12.dp),
            supportingText = { Text("تقدر تسيبه فاضي وتكمل عادي.") }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorText = "" },
            label = { Text("كلمة المرور") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandPrimary) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "إظهار/إخفاء كلمة المرور")
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("register_password_input"),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorText = "" },
            label = { Text("تأكيد كلمة المرور") },
            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = BrandPrimary) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_input"),
            shape = RoundedCornerShape(12.dp)
        )
        if (errorText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val phoneDigits = phone.filter { it.isDigit() }
                when {
                    fullName.isBlank() -> errorText = "اكتب اسمك بالكامل."
                    phoneDigits.length !in 10..15 -> errorText = "اكتب رقم موبايل صحيح."
                    email.isNotBlank() && (!email.contains("@") || !email.substringAfter("@").contains(".")) -> errorText = "البريد الإلكتروني غير صحيح، أو اتركه فارغًا."
                    password.length < 6 -> errorText = "كلمة المرور لازم تكون 6 أحرف على الأقل."
                    password != confirmPassword -> errorText = "كلمتا المرور غير متطابقتين."
                    else -> onRegisterClick(fullName.trim(), email.trim(), password, phone.trim())
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("register_submit_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
        ) {
            Text("إنشاء الحساب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.clickable { onNavigateToLogin() }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("لديك حساب بالفعل؟", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("تسجيل الدخول", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BrandPrimary)
        }
    }
}
