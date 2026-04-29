package com.sameerasw.livepaper

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.livepaper.ui.theme.LivePaperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LivePaperTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var showSheet by remember { mutableStateOf(true) }

                if (showSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { 
                            showSheet = false
                            finish()
                        },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        VideoPickerSheet(onDismiss = {
                            showSheet = false
                            finish()
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPickerSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { PreferencesManager(context) }
    var availableVideos by remember { mutableStateOf(prefs.getAvailableVideos()) }
    var selectedVideo by remember { mutableStateOf(prefs.selectedVideoName) }
    var playbackTrigger by remember { mutableStateOf(prefs.playbackTrigger) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) { e.printStackTrace() }
                prefs.addCustomVideo(it.toString())
                availableVideos = prefs.getAvailableVideos()
                selectedVideo = it.toString()
                prefs.selectedVideoName = it.toString()
            }
        }
    )
    
    LaunchedEffect(availableVideos) {
        if (selectedVideo == PreferencesManager.DEFAULT_VIDEO && availableVideos.isNotEmpty()) {
            val first = availableVideos.first()
            selectedVideo = first
            prefs.selectedVideoName = first
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Play when",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            val options = listOf(
                PreferencesManager.TRIGGER_UNLOCK to "Unlock",
                PreferencesManager.TRIGGER_SCREEN_ON to "Screen on"
            )
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = playbackTrigger == value,
                    onClick = {
                        playbackTrigger = value
                        prefs.playbackTrigger = value
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(label)
                }
            }
        }

        Text(
            text = "Video",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            item {
                AddVideoItem(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                })
            }

            items(availableVideos) { video ->
                ThumbnailItem(
                    videoName = video,
                    isSelected = video == selectedVideo,
                    onClick = {
                        selectedVideo = video
                        prefs.selectedVideoName = video
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.btn_apply))
        }
    }
}

@Composable
fun ThumbnailItem(videoName: String, isSelected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(videoName) {
        thumbnail = withContext(Dispatchers.IO) {
            val resId = context.resources.getIdentifier(videoName, "raw", context.packageName)
            if (resId != 0) {
                MediaMetadataRetriever().use { retriever ->
                    try {
                        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
                        retriever.setDataSource(context, uri)
                        retriever.getFrameAtTime(0)
                    } catch (e: Exception) { null }
                }
            } else {
                try {
                    val uri = Uri.parse(videoName)
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(context, uri)
                        retriever.getFrameAtTime(0)
                    }
                } catch (e: Exception) { null }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
            onClick()
        }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Box(modifier = Modifier.fillMaxSize().background(Color.Black))

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
                Icon(
                    painter = painterResource(id = R.drawable.rounded_check_circle_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Text(
            text = if (videoName.startsWith("content://")) {
                stringResource(R.string.custom_video)
            } else {
                videoName.replace("_", " ").replaceFirstChar { it.uppercase() }
            },
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun AddVideoItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_add_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = stringResource(R.string.add_video),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}