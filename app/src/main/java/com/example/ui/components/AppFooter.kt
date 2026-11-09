package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AppFooter(
    onOpenUrl: (Context, String) -> Unit,
    onOpenWhatsApp: (Context, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(20.dp)
            .testTag("app_footer"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Social Media Buttons Row
        Text(
            text = "تواصل مع منصة أبو قاعود التعليمية",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // WhatsApp Button
            SocialIconButton(
                title = "واتساب",
                iconColor = WhatsAppGreen,
                onClick = { onOpenWhatsApp(context, "01000000000") },
                testTag = "footer_whatsapp_button"
            )

            // YouTube Button
            SocialIconButton(
                title = "يوتيوب",
                iconColor = YouTubeRed,
                onClick = { onOpenUrl(context, "https://youtube.com/@abouqaoud") },
                testTag = "footer_youtube_button"
            )

            // Facebook Button
            SocialIconButton(
                title = "فيسبوك",
                iconColor = FacebookBlue,
                onClick = { onOpenUrl(context, "https://facebook.com") },
                testTag = "footer_facebook_button"
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // FIXED ABDULLAH FACEBOOK LINK (Mandatory Requirement)
        // URL: https://www.facebook.com/share/1CJk9seQwC/
        // Display: Abdullah
        // Below it: EDUCATION • TECHNOLOGY • DEVELOPMENT
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(BrandPrimary, BrandSecondary)),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable {
                    onOpenUrl(context, "https://www.facebook.com/share/1CJk9seQwC/")
                }
                .testTag("fixed_abdullah_link"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "فتح رابط فيسبوك",
                    tint = FacebookBlue,
                    modifier = Modifier.size(20.dp)
                )

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Abdullah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FacebookBlue
                    )
                    Text(
                        text = "EDUCATION • TECHNOLOGY • DEVELOPMENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Platform Copyright & Identity
        Text(
            text = "جميع الحقوق محفوظة © منصة أبو قاعود التعليمية للطلاب",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SocialIconButton(
    title: String,
    iconColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = iconColor.copy(alpha = 0.12f),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(iconColor, RoundedCornerShape(50))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
    }
}
