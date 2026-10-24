package com.commissioning.cctv

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: MainViewModel, sl: Int) {
    val item = vm.item(sl) ?: return
    val check = vm.results[sl] ?: Check()
    var comment by remember(sl) { mutableStateOf(check.comment) }

    val idx = vm.navList.indexOf(sl)
    val total = vm.navList.size
    val status = check.status()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SL ${item.sl}  ·  ${item.rackName}", fontWeight = FontWeight.Bold)
                        Text(
                            if (idx >= 0) "Item ${idx + 1} of $total  ·  ${status.label()}" else status.label(),
                            fontSize = 12.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.close() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { vm.step(-1) },
                        enabled = idx > 0,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, null)
                        Text("Previous")
                    }
                    Button(
                        onClick = { vm.step(+1) },
                        enabled = idx in 0 until total - 1,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text("Next")
                        Icon(Icons.Default.KeyboardArrowRight, null)
                    }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------- Verification ----------
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            "Verification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            vm.setFieldTag(sl, true); vm.setCableTag(sl, true); vm.setCableLocation(sl, true)
                        }) { Text("All Yes") }
                    }
                    YesNoRow(
                        label = "Field Tag",
                        expected = "Camera tag: ${item.cameraTag.ifBlank { "(none in schedule)" }}",
                        value = check.fieldTag,
                        onChange = { vm.setFieldTag(sl, it) },
                    )
                    HorizontalDivider()
                    YesNoRow(
                        label = "Cable Tag",
                        expected = "Patch panel tag: ${item.patchPanelTag}",
                        value = check.cableTag,
                        onChange = { vm.setCableTag(sl, it) },
                    )
                    HorizontalDivider()
                    YesNoRow(
                        label = "Cable Location",
                        expected = "${item.location}  →  ${item.rackName} / ${item.patchPanel} / ${item.port}",
                        value = check.cableLocation,
                        onChange = { vm.setCableLocation(sl, it) },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it; vm.setComment(sl, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Comments / observations") },
                        minLines = 3,
                    )
                    PhotoSection(vm, sl, check.photos)
                }
            }

            // ---------- Reference data from the schedule ----------
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Schedule details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Info("Camera tag", item.cameraTag)
                    Info("Location", item.location)
                    Info("Zone", item.zone)
                    Info("Camera type", item.cameraType)
                    Info("Object view", item.objectView)
                    Info("Rack", "${item.rackName}  (${item.rackLocation})")
                    Info("Patch panel / port", "${item.patchPanel} / ${item.port}")
                    Info("Recorder sequence", item.sequence)
                    Info("Patch panel tag", item.patchPanelTag)
                    Info("Switch", "${item.swPort}  –  ${item.switchType}")
                    Info("Final tag", item.finalTag)
                }
            }
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    if (value.isBlank()) return
    Row {
        Text(label, color = Grey, fontSize = 13.sp, modifier = Modifier.width(120.dp))
        Text(value, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun YesNoRow(label: String, expected: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(expected, fontSize = 12.sp, color = Grey, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceButton("YES", value == true, Green, Modifier.weight(1f)) {
                onChange(if (value == true) null else true)
            }
            ChoiceButton("NO", value == false, Red, Modifier.weight(1f)) {
                onChange(if (value == false) null else false)
            }
        }
    }
}

@Composable
private fun ChoiceButton(text: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
}

@Composable
private fun rememberPhoto(file: File, target: Int): ImageBitmap? {
    val state = produceState<ImageBitmap?>(null, file.name, target) {
        value = withContext(Dispatchers.IO) { Photos.decode(file, target)?.asImageBitmap() }
    }
    return state.value
}

@Composable
private fun PhotoSection(vm: MainViewModel, sl: Int, photos: List<String>) {
    val context = LocalContext.current
    var viewing by remember { mutableStateOf<String?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) vm.addPhotos(sl, uris)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        vm.finishShot(ok)
    }

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("Photos (${photos.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val f = vm.prepareShot(sl)
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
                    camera.launch(uri)
                },
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text("Take photo") }
            OutlinedButton(
                onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text("Choose photos") }
        }
        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                photos.forEach { name ->
                    Box(Modifier.size(104.dp).clip(RoundedCornerShape(8.dp)).background(Grey.copy(alpha = 0.25f))) {
                        val bmp = rememberPhoto(vm.photoFile(name), 300)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clickable { viewing = name },
                            )
                        }
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .clickable { vm.removePhoto(sl, name) },
                            contentAlignment = Alignment.Center,
                        ) { Text("\u00D7", color = Color.White, fontSize = 16.sp) }
                    }
                }
            }
        }
    }

    viewing?.let { name ->
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { viewing = null }) {
                val bmp = rememberPhoto(vm.photoFile(name), 1600)
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
