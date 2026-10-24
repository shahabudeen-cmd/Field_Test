package com.commissioning.cctv

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Status.color(): Color = when (this) {
    Status.VERIFIED -> Green
    Status.ISSUE -> Red
    Status.IN_PROGRESS -> Amber
    Status.PENDING -> Grey
}

fun Status.label(): String = when (this) {
    Status.VERIFIED -> "Verified"
    Status.ISSUE -> "Issue"
    Status.IN_PROGRESS -> "Partial"
    Status.PENDING -> "Pending"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var nameDialog by remember { mutableStateOf(false) }
    var resetDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val ok = vm.export(uri)
            Toast.makeText(context, if (ok) "Report exported" else "Export failed", Toast.LENGTH_LONG).show()
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            val ok = vm.exportPhotos(uri)
            Toast.makeText(context, if (ok) "Photos exported" else "Export failed", Toast.LENGTH_LONG).show()
        }
    }
    val exportName = "CCTV_Verification_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".csv"

    val list = vm.filtered()
    val verified = vm.count(Status.VERIFIED)
    val issues = vm.count(Status.ISSUE)
    val partial = vm.count(Status.IN_PROGRESS)
    val total = vm.items.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CCTV Commissioning", fontWeight = FontWeight.Bold)
                        Text(
                            if (vm.engineer.isBlank()) "Tap menu to set engineer name" else "Engineer: ${vm.engineer}",
                            fontSize = 12.sp,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = { exportLauncher.launch(exportName) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export report")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Engineer name") }, onClick = {
                                menuOpen = false; nameDialog = true
                            })
                            DropdownMenuItem(text = { Text("Export report (CSV)") }, onClick = {
                                menuOpen = false; exportLauncher.launch(exportName)
                            })
                            DropdownMenuItem(text = { Text("Export photos (ZIP)") }, onClick = {
                                menuOpen = false
                                if (vm.photoCount() == 0) {
                                    Toast.makeText(context, "No photos to export", Toast.LENGTH_SHORT).show()
                                } else {
                                    photoLauncher.launch(
                                        "CCTV_Photos_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".zip"
                                    )
                                }
                            })
                            DropdownMenuItem(text = { Text("Reset all results") }, onClick = {
                                menuOpen = false; resetDialog = true
                            })
                        }
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Checked ${verified + issues} of $total", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (total == 0) 0f else (verified + issues).toFloat() / total },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Green,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Stat("Verified", verified, Green)
                            Stat("Issues", issues, Red)
                            Stat("Partial", partial, Amber)
                            Stat("Pending", total - verified - issues - partial, Grey)
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = vm.query,
                    onValueChange = { vm.query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search camera tag, location, port, SL no.") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (vm.query.isNotEmpty()) {
                            IconButton(onClick = { vm.query = "" }) { Icon(Icons.Default.Clear, "Clear") }
                        }
                    },
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = vm.rack == null, onClick = { vm.rack = null }, label = { Text("All racks") })
                    }
                    items(vm.racks) { r ->
                        FilterChip(selected = vm.rack == r, onClick = { vm.rack = r }, label = { Text(r) })
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StatusFilter.values().toList()) { f ->
                        FilterChip(
                            selected = vm.statusFilter == f,
                            onClick = { vm.statusFilter = f },
                            label = { Text(f.label) },
                        )
                    }
                }
            }
            item {
                Text("${list.size} item(s)", style = MaterialTheme.typography.labelMedium, color = Grey)
            }
            items(list, key = { it.sl }) { item ->
                ItemRow(item, vm.results[item.sl].status()) {
                    vm.open(item.sl, list.map { it.sl })
                }
            }
        }
    }

    if (nameDialog) {
        var name by remember { mutableStateOf(vm.engineer) }
        AlertDialog(
            onDismissRequest = { nameDialog = false },
            title = { Text("Commissioning engineer") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") })
            },
            confirmButton = { TextButton(onClick = { vm.setEngineer(name); nameDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { nameDialog = false }) { Text("Cancel") } },
        )
    }
    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text("Reset all results?") },
            text = { Text("This clears every Yes/No answer and comment on this device. Export first if you need a copy.") },
            confirmButton = { TextButton(onClick = { vm.resetAll(); resetDialog = false }) { Text("Reset", color = Red) } },
            dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color) {
    Column {
        Text(value.toString(), fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
        Text(label, fontSize = 11.sp, color = Grey)
    }
}

@Composable
private fun ItemRow(item: Item, status: Status, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(status.color(), CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.cameraTag.ifBlank { "(no camera tag in schedule)" },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "SL ${item.sl} · ${item.rackName} · ${item.patchPanel}/${item.port} · ${item.sequence}",
                    fontSize = 12.sp,
                    color = Grey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(item.location, fontSize = 12.sp, color = Grey, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier
                    .background(status.color().copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(status.label(), color = status.color(), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
