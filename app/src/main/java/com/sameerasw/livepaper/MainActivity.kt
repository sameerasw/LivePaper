package com.sameerasw.livepaper

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    val prefs = remember { PreferencesManager(context) }
    val availableVideos = remember { prefs.getAvailableVideos() }
    var selectedVideo by remember { mutableStateOf(prefs.selectedVideoName) }
    
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
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
            } else null
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
            text = videoName.replace("_", " ").replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}