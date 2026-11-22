package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SampleData
import com.example.model.User
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgPink
import com.example.ui.viewmodel.AiCaptionUiState

data class PhotoFilter(
    val name: String,
    val overlayColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    aiCaptionState: AiCaptionUiState,
    onGenerateAiCaptions: (topicOrVibe: String, location: String?, filterName: String) -> Unit,
    onSharePost: (caption: String, imageUrl: String, location: String?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sampleGalleryImages = remember { SampleData.sampleExploreItems }

    var mediaUri by remember { mutableStateOf<String?>(sampleGalleryImages.first()) }
    var isVideoMedia by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("San Francisco, CA") }
    var aiTopicText by remember { mutableStateOf("") }

    val taggedPeople = remember { mutableStateListOf<User>() }
    var showTagPeopleSheet by remember { mutableStateOf(false) }

    val filters = remember {
        listOf(
            PhotoFilter("Normal", Color.Transparent),
            PhotoFilter("Clarendon", Color(0x330080FF)),
            PhotoFilter("Juno", Color(0x33FFB6C1)),
            PhotoFilter("Valencia", Color(0x33FFE4B5)),
            PhotoFilter("Lark", Color(0x2200FFFF)),
            PhotoFilter("Gingham", Color(0x22808080))
        )
    }
    var selectedFilter by remember { mutableStateOf(filters[0]) }

    val popularHashtags = listOf("#photography", "#vibe", "#aesthetic", "#art", "#travel", "#goldenhour", "#instasnap")

    // Zero-permission Android Photo Picker for Image & Video upload
    val visualMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            isVideoMedia = mimeType.startsWith("video/")
            mediaUri = uri.toString()
        }
    }

    val currentImage = mediaUri ?: sampleGalleryImages.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New post",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            TextButton(
                onClick = {
                    val finalCaption = if (taggedPeople.isNotEmpty()) {
                        val tags = taggedPeople.joinToString(" ") { "@${it.username}" }
                        "$captionText\n\nTagged: $tags".trim()
                    } else {
                        captionText
                    }
                    onSharePost(finalCaption, currentImage, locationText)
                },
                modifier = Modifier.testTag("create_post_share_button")
            ) {
                Text(
                    text = "Share",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = IgBlue
                    )
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Selected Media Preview with Filter overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black.copy(alpha = 0.05f))
            ) {
                AsyncImage(
                    model = currentImage,
                    contentDescription = "Selected media preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (!isVideoMedia && selectedFilter.overlayColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(selectedFilter.overlayColor)
                    )
                }

                if (isVideoMedia) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VIDEO",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (taggedPeople.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Tagged: ${taggedPeople.joinToString(", ") { "@${it.username}" }}",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Upload Button (Device Photo Picker)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        visualMediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("upload_media_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IgBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Photo or Video", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            // Filter Selector Row (for images)
            if (!isVideoMedia) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, top = 4.dp, bottom = 6.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filters) { filter ->
                        val isFilterSelected = filter == selectedFilter
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isFilterSelected) 2.dp else 1.dp,
                                        color = if (isFilterSelected) IgBlue else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = currentImage,
                                    contentDescription = filter.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (filter.overlayColor != Color.Transparent) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(filter.overlayColor)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = filter.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isFilterSelected) IgBlue else MaterialTheme.colorScheme.onSurface
                                ),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Quick Gallery Selector
            Text(
                text = "Preset Gallery Items",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 6.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleGalleryImages) { imgUrl ->
                    val isSelected = mediaUri == imgUrl
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) IgBlue else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                isVideoMedia = false
                                mediaUri = imgUrl
                            }
                    ) {
                        AsyncImage(
                            model = imgUrl,
                            contentDescription = "Preset gallery item",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Gemini AI Caption Studio Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("ai_caption_studio_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = IgPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Caption Studio (Gemini)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = aiTopicText,
                            onValueChange = { aiTopicText = it },
                            placeholder = { Text("Describe the photo vibe or mood...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_caption_topic_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                onGenerateAiCaptions(
                                    aiTopicText.ifBlank { captionText },
                                    locationText,
                                    selectedFilter.name
                                )
                            },
                            enabled = aiCaptionState !is AiCaptionUiState.Loading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IgBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("generate_ai_captions_button")
                        ) {
                            if (aiCaptionState is AiCaptionUiState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Generate", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    when (aiCaptionState) {
                        is AiCaptionUiState.Success -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Tap a suggestion to use it:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            aiCaptionState.result.captions.forEach { option ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            val tags = aiCaptionState.result.hashtags.take(4).joinToString(" ")
                                            captionText = "$option\n\n$tags".trim()
                                        }
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            if (aiCaptionState.result.hashtags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(aiCaptionState.result.hashtags) { tag ->
                                        AssistChip(
                                            onClick = {
                                                captionText = if (captionText.isEmpty()) tag else "$captionText $tag"
                                            },
                                            label = { Text(tag, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        is AiCaptionUiState.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiCaptionState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        else -> Unit
                    }
                }
            }

            // Caption Text Input
            TextField(
                value = captionText,
                onValueChange = { captionText = it },
                placeholder = {
                    Text(
                        text = "Write a caption...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp)
                    .testTag("create_post_caption_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Hashtag Quick Tap Suggestions
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(popularHashtags) { tag ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            captionText = if (captionText.isEmpty()) tag else "$captionText $tag"
                        }
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Add Location Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                TextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    placeholder = { Text("Add location", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Tag People Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTagPeopleSheet = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("tag_people_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tag people",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (taggedPeople.isNotEmpty()) {
                        Text(
                            text = taggedPeople.joinToString(", ") { "@${it.username}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = IgBlue
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Add Music Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Add audio track • Ludovico Einaudi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Tag People Bottom Sheet
    if (showTagPeopleSheet) {
        TagPeopleBottomSheet(
            alreadyTagged = taggedPeople,
            onDismiss = { showTagPeopleSheet = false },
            onToggleTag = { user ->
                if (taggedPeople.any { it.id == user.id }) {
                    taggedPeople.removeIf { it.id == user.id }
                } else {
                    taggedPeople.add(user)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagPeopleBottomSheet(
    alreadyTagged: List<User>,
    onDismiss: () -> Unit,
    onToggleTag: (User) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Tag People",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            SampleData.sampleUsers.forEach { user ->
                val isTagged = alreadyTagged.any { it.id == user.id }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleTag(user) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.username,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isTagged) {
                        Surface(
                            shape = CircleShape,
                            color = IgBlue,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Tagged",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onToggleTag(user) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Tag", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
