package com.example.ui.screens.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.home.CourseCatalogCard
import com.example.ui.theme.BrandPrimary
import com.example.ui.viewmodel.EduViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesCatalogScreen(
    viewModel: EduViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCourse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val courses = viewModel.courses.value
    var filterText by remember { mutableStateOf("") }

    val filtered = if (filterText.isBlank()) {
        courses
    } else {
        courses.filter {
            it.title.contains(filterText, ignoreCase = true) ||
                    it.description.contains(filterText, ignoreCase = true) ||
                    it.level.contains(filterText, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دليل الكورسات والمناهج", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("catalog_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الرجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("courses_catalog_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("بحث في الكورسات والمراحل التعليمية...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = BrandPrimary)
                    },
                    trailingIcon = {
                        if (filterText.isNotEmpty()) {
                            IconButton(onClick = { filterText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_search_input"),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            items(filtered) { course ->
                CourseCatalogCard(
                    course = course,
                    onClick = { onNavigateToCourse(course.id) }
                )
            }
        }
    }
}
