package com.example.ui.screens.forum

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ForumComment
import com.example.data.model.ForumPost
import com.example.ui.theme.*
import com.example.ui.viewmodel.EduViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseForumScreen(
    courseId: String,
    viewModel: EduViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val posts = viewModel.forumPosts.value[courseId] ?: emptyList()
    val course = viewModel.courses.value.find { it.id == courseId }

    var showCreatePostDialog by remember { mutableStateOf(false) }
    var activePostForComments by remember { mutableStateOf<ForumPost?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("منتدى نقاش الطلاب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(course?.title ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("forum_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الرجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreatePostDialog = true },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_post_fab")
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "سؤال جديد")
            }
        },
        modifier = modifier.testTag("course_forum_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = BrandPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = null, tint = BrandPrimary)
                        Text(
                            text = "منتدى تفاعلي خاص بالطلاب المشتركين في هذا الكورس. شارك استفساراتك وتناقش مع زملائك والمدرس.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد استفسارات بعد، كن أول من يسأل!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(posts) { post ->
                    ForumPostCard(
                        post = post,
                        onLikeClick = { viewModel.repository.toggleLikeForumPost(courseId, post.id) },
                        onCommentClick = { activePostForComments = post }
                    )
                }
            }
        }
    }

    // Create New Post Dialog
    if (showCreatePostDialog) {
        var postTitle by remember { mutableStateOf("") }
        var postContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            title = { Text("طرح سؤال أو استفسار جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = postTitle,
                        onValueChange = { postTitle = it },
                        label = { Text("عنوان السؤال") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_post_title_input")
                    )

                    OutlinedTextField(
                        value = postContent,
                        onValueChange = { postContent = it },
                        label = { Text("تفاصيل السؤال والموضوع") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).testTag("new_post_content_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (postTitle.isNotBlank() && postContent.isNotBlank()) {
                            viewModel.repository.addForumPost(courseId, postTitle.trim(), postContent.trim())
                            showCreatePostDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier.testTag("submit_new_post_button")
                ) {
                    Text("نشر السؤال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Comments Sheet / Dialog
    activePostForComments?.let { post ->
        var newCommentText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { activePostForComments = null },
            title = { Text("الردود والمناقشة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = post.title, fontWeight = FontWeight.Bold, color = BrandPrimary)
                    Text(text = post.content, style = MaterialTheme.typography.bodySmall)

                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(post.comments) { comment ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = comment.studentName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Text(text = comment.content, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("اكتب تعليقك أو إجابتك...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            viewModel.repository.addForumComment(courseId, post.id, newCommentText.trim())
                            newCommentText = ""
                            activePostForComments = null
                        }
                    }
                ) {
                    Text("إرسال الرد")
                }
            },
            dismissButton = {
                TextButton(onClick = { activePostForComments = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun ForumPostCard(
    post: ForumPost,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BrandPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = post.studentName.take(1),
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = post.studentName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = post.createdAt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                TextButton(
                    onClick = onLikeClick,
                    modifier = Modifier.testTag("like_post_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                        contentDescription = "إعجاب",
                        tint = if (post.isLikedByMe) BrandPrimary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount}",
                        color = if (post.isLikedByMe) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Comments Button
                TextButton(
                    onClick = onCommentClick,
                    modifier = Modifier.testTag("comments_button_${post.id}")
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "الردود", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${post.commentsCount} ردود")
                }
            }
        }
    }
}
