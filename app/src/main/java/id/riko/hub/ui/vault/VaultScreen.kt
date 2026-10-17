package id.riko.hub.ui.vault

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.data.remote.drive.DriveFile
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.theme.ChefButter
import id.riko.hub.ui.theme.ChefCanvas
import id.riko.hub.ui.theme.ChefCoral
import id.riko.hub.ui.theme.ChefGround
import id.riko.hub.ui.theme.ChefInk
import id.riko.hub.ui.theme.ChefLime
import id.riko.hub.ui.theme.ChefLine
import id.riko.hub.ui.theme.ChefMuted
import id.riko.hub.ui.theme.ChefPanel
import id.riko.hub.ui.theme.ChefRaised
import id.riko.hub.ui.theme.ChefSage
import id.riko.hub.ui.theme.NotoCondensedBlack
import id.riko.hub.ui.theme.Spacing

@Composable
fun VaultScreen(onNavigate: (Destination) -> Unit, onComingSoon: () -> Unit, sharedModel: VaultViewModel? = null) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: VaultViewModel = sharedModel ?: viewModel(factory = viewModelFactory {
        initializer { VaultViewModel(app.container.materials, app.container.driveAuthorization) }
    })
    VaultRoute(model, onNavigate)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun VaultRoute(model: VaultViewModel, onNavigate: (Destination) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val messageHost = remember { SnackbarHostState() }
    val authorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        model.authorizationResult(result.resultCode == android.app.Activity.RESULT_OK, result.data)
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.upload(uri) else model.cancelUpload()
    }
    val unableToOpen = stringResource(R.string.vault_open_unavailable)
    val openError = stringResource(R.string.vault_open_failed)

    LaunchedEffect(model) {
        model.effects.collect { effect ->
            when (effect) {
                is VaultEffect.LaunchAuthorization -> runCatching {
                    authorizationLauncher.launch(IntentSenderRequest.Builder(effect.pendingIntent).build())
                }.onFailure { model.authorizationLaunchFailed(); messageHost.showSnackbar(openError) }
                VaultEffect.SelectUploadFile -> runCatching { documentLauncher.launch(UPLOAD_MIME_TYPES) }
                    .onFailure { model.cancelUpload(); messageHost.showSnackbar(unableToOpen) }
                is VaultEffect.OpenFile -> openDriveFile(context, model, effect.file, effect.session, messageHost, unableToOpen, openError)
            }
        }
    }

    BackHandler(state.breadcrumbs.size > 1) { model.backFolder() }
    var fileFilter by rememberSaveable(state.folderId, state.search, state.connectedEmail) { mutableStateOf("all") }
    val visibleFiles = state.files.filter { file ->
        when (fileFilter) {
            "folder" -> file.isFolder
            "pdf" -> file.mimeType == "application/pdf"
            "image" -> file.mimeType.startsWith("image/")
            else -> true
        }
    }
    val folders = visibleFiles.filter { it.isFolder }
    val documents = visibleFiles.filterNot { it.isFolder }

    Box(
        Modifier
            .fillMaxSize()
            .background(ChefSage)
            .testTag("screen-Vault"),
    ) {
        Column(Modifier.fillMaxSize()) {
            // TOP SAGE STAGE (Clean Header + Whimsical Character Cook Illustration)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Header Bar
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "GUDANG MATERI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ChefGround.copy(alpha = 0.7f),
                        )
                        Text(
                            text = "REPORTS",
                            fontFamily = NotoCondensedBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = ChefGround,
                            modifier = Modifier.semantics { heading() },
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "semua",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ChefGround,
                            )
                            Text("⌄", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChefGround)
                        }
                    }
                }

                // Whimsical Character Cook Illustration (Reproduction from full-app-v1.html line 479)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(width = 180.dp, height = 100.dp)) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        val cy = h / 2f

                        // Antennae
                        drawLine(color = ChefGround, start = Offset(cx - 30.dp.toPx(), cy - 20.dp.toPx()), end = Offset(cx - 45.dp.toPx(), cy - 38.dp.toPx()), strokeWidth = 2.dp.toPx())
                        drawCircle(color = ChefCoral, radius = 6.dp.toPx(), center = Offset(cx - 45.dp.toPx(), cy - 38.dp.toPx()))

                        drawLine(color = ChefGround, start = Offset(cx + 30.dp.toPx(), cy - 20.dp.toPx()), end = Offset(cx + 45.dp.toPx(), cy - 38.dp.toPx()), strokeWidth = 2.dp.toPx())
                        drawCircle(color = ChefGround, radius = 7.dp.toPx(), center = Offset(cx + 45.dp.toPx(), cy - 38.dp.toPx()))

                        // Oven character box (Butter Yellow)
                        drawRoundRect(
                            color = ChefButter,
                            topLeft = Offset(cx - 45.dp.toPx(), cy - 20.dp.toPx()),
                            size = Size(90.dp.toPx(), 65.dp.toPx()),
                            cornerRadius = CornerRadius(14.dp.toPx()),
                        )
                        drawRoundRect(
                            color = ChefGround,
                            topLeft = Offset(cx - 45.dp.toPx(), cy - 20.dp.toPx()),
                            size = Size(90.dp.toPx(), 65.dp.toPx()),
                            cornerRadius = CornerRadius(14.dp.toPx()),
                            style = Stroke(width = 2.5.dp.toPx()),
                        )

                        // Dark screen window
                        drawRoundRect(
                            color = ChefGround,
                            topLeft = Offset(cx - 35.dp.toPx(), cy - 12.dp.toPx()),
                            size = Size(70.dp.toPx(), 30.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                        )

                        // 3 dial knobs
                        drawCircle(color = ChefCoral, radius = 5.dp.toPx(), center = Offset(cx - 24.dp.toPx(), cy + 30.dp.toPx()))
                        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(cx, cy + 30.dp.toPx()))
                        drawCircle(color = ChefLime, radius = 5.dp.toPx(), center = Offset(cx + 24.dp.toPx(), cy + 30.dp.toPx()))
                    }
                }
            }

            // DARK SLIDING BOTTOM SHEET (Panel for Material Cards)
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = ChefCanvas,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Search Pill Bar (⌕ input ||||)
                    item("search-pill-bar") {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = ChefPanel,
                            border = BorderStroke(1.dp, ChefLine),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("⌕", fontSize = 16.sp, color = ChefMuted, fontWeight = FontWeight.Bold)
                                BasicTextField(
                                    value = state.search,
                                    onValueChange = model::search,
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    cursorBrush = SolidColor(ChefLime),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = ChefInk),
                                    decorationBox = { innerTextField ->
                                        if (state.search.isEmpty()) {
                                            Text("Cari laporan atau materi...", style = MaterialTheme.typography.bodyMedium, color = ChefMuted)
                                        }
                                        innerTextField()
                                    },
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ChefRaised,
                                ) {
                                    Text(
                                        text = "||||",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = ChefMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Count Row + Sort Button
                    item("count-row") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${visibleFiles.size} materi & laporan tersimpan",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChefMuted,
                            )
                            Text(
                                text = "URUTKAN ⌄",
                                fontFamily = NotoCondensedBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = ChefLime,
                            )
                        }
                    }

                    // Filter Chips Row
                    item("browse-types") {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "all" to R.string.library_all,
                                "folder" to R.string.library_folders,
                                "pdf" to R.string.library_pdf,
                                "image" to R.string.library_images,
                            ).forEach { (key, label) ->
                                val selected = fileFilter == key
                                FilterChip(
                                    selected = selected,
                                    onClick = { fileFilter = key },
                                    label = { Text(stringResource(label), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChefLime,
                                        selectedLabelColor = ChefGround,
                                        containerColor = ChefPanel,
                                        labelColor = ChefMuted,
                                    ),
                                    modifier = Modifier.heightIn(min = 36.dp),
                                )
                            }
                        }
                    }

                    // Drive Connection Card
                    item("drive-card") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ChefPanel,
                            border = BorderStroke(1.dp, ChefLine),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        Modifier.size(36.dp).clip(CircleShape).background(ChefSage),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(painterResource(R.drawable.ic_cloud), null, tint = ChefGround, modifier = Modifier.size(20.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("KONEKSI GOOGLE DRIVE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ChefMuted)
                                        Text(
                                            text = state.connectedName ?: if (state.connectedEmail == null) "Belum Terhubung" else state.connectedEmail.orEmpty(),
                                            fontFamily = NotoCondensedBlack,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = ChefInk,
                                        )
                                    }
                                }
                                if (state.connectedEmail == null) {
                                    Button(
                                        onClick = model::connect,
                                        enabled = !state.authenticating,
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(999.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround),
                                    ) {
                                        Text("Sambungkan Drive", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = model::refresh,
                                            enabled = !state.authenticating,
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(999.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ChefPanel, contentColor = ChefInk),
                                            border = BorderStroke(1.dp, ChefLine),
                                        ) {
                                            Text("Segarkan", fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = model::disconnect) {
                                            Text("Putus", color = ChefCoral)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Breadcrumb location row
                    if (state.breadcrumbs.size > 1) {
                        item("breadcrumbs") {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                state.breadcrumbs.forEachIndexed { index, crumb ->
                                    if (index > 0) Text("/", color = ChefMuted)
                                    TextButton(onClick = { model.navigateToBreadcrumb(index) }) {
                                        Text(crumb.second, color = ChefLime, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Files & Folders List (PRIME COST REPORT style cards from reference)
                    if (folders.isNotEmpty()) {
                        item("folders-heading") {
                            Text("FOLDER TERSIMPAN", fontFamily = NotoCondensedBlack, fontWeight = FontWeight.Black, fontSize = 14.sp, color = ChefMuted)
                        }
                        items(folders, key = { "file-${it.id}" }) { file ->
                            MChefFileCard(file = file, onClick = { model.open(file) })
                        }
                    }

                    if (documents.isNotEmpty()) {
                        item("docs-heading") {
                            Text("BERKAS & LAPORAN", fontFamily = NotoCondensedBlack, fontWeight = FontWeight.Black, fontSize = 14.sp, color = ChefMuted)
                        }
                        items(documents, key = { "file-${it.id}" }) { file ->
                            MChefFileCard(file = file, onClick = { model.open(file) })
                        }
                    }

                    if (!state.loading && state.error == null && visibleFiles.isEmpty()) {
                        item("empty-vault") {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ChefPanel,
                                border = BorderStroke(1.dp, ChefLine),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "Belum Ada Berkas",
                                        fontFamily = NotoCondensedBlack,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = ChefInk,
                                    )
                                    Text(
                                        text = "Sambungkan Drive atau unggah materi riset.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ChefMuted,
                                    )
                                }
                            }
                        }
                    }

                    item("spacer-bottom") { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // Floating Butter Yellow FAB for Upload / Add
        FloatingActionButton(
            onClick = model::startUpload,
            shape = CircleShape,
            containerColor = ChefButter,
            contentColor = ChefGround,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .size(54.dp),
        ) {
            Text("＋", fontFamily = NotoCondensedBlack, fontWeight = FontWeight.Black, fontSize = 24.sp, color = ChefGround)
        }

        SnackbarHost(messageHost, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MChefFileCard(file: DriveFile, onClick: () -> Unit) {
    val (sqColor, sqLabel) = when {
        file.isFolder -> ChefSage to "DIR"
        file.mimeType == "application/pdf" -> ChefCoral to "PDF"
        file.mimeType.startsWith("image/") -> ChefLime to "IMG"
        file.mimeType.contains("sheet") || file.mimeType.contains("csv") -> ChefSage to "CSV"
        else -> ChefButter to "TXT"
    }
    val textColor = if (sqColor == ChefCoral) Color.White else ChefGround

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Square Monogram Tile (PDF, TXT, CSV, DIR)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = sqColor,
                modifier = Modifier.size(width = 46.dp, height = 46.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = sqLabel,
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = textColor,
                    )
                }
            }

            // File details
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = ChefInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(fileTypeLabel(file)),
                    style = MaterialTheme.typography.bodySmall,
                    color = ChefMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Arrow button ↗
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ChefRaised),
                contentAlignment = Alignment.Center,
            ) {
                Text("↗", color = ChefInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun fileTypeLabel(file: DriveFile): Int = when {
    file.isFolder -> R.string.library_folder_type
    file.mimeType == "application/pdf" -> R.string.library_pdf
    file.mimeType.startsWith("image/") -> R.string.library_image_type
    file.mimeType == "application/vnd.google-apps.document" -> R.string.library_google_document
    file.mimeType == "application/vnd.google-apps.spreadsheet" -> R.string.library_google_sheet
    file.mimeType == "application/vnd.google-apps.presentation" -> R.string.library_google_slides
    else -> R.string.library_file_type
}

private fun errorString(code: String): Int = when (code) {
    "reauth" -> R.string.vault_error_auth
    "permission" -> R.string.vault_error_permission
    "offline" -> R.string.vault_error_offline
    "not_found" -> R.string.vault_error_not_found
    "quota" -> R.string.vault_error_quota
    else -> R.string.vault_error_general
}

private val UPLOAD_MIME_TYPES = arrayOf(
    "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.oasis.opendocument.text", "application/vnd.oasis.opendocument.spreadsheet", "application/vnd.oasis.opendocument.presentation",
    "application/rtf", "application/epub+zip", "text/plain", "text/markdown", "text/csv", "image/*",
)

private suspend fun openDriveFile(
    context: android.content.Context,
    model: VaultViewModel,
    file: DriveFile,
    session: Int,
    host: SnackbarHostState,
    unavailable: String,
    failed: String,
) {
    try {
        if (!model.hasActiveSession(session)) return
        if (file.mimeType.startsWith("application/vnd.google-apps.")) {
            if (model.hasActiveSession(session) && openWorkspaceLink(context, file)) return
            throw ActivityNotFoundException()
        }
        try {
            val cached = model.download(file, session) ?: return
            if (!model.hasActiveSession(session)) return
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.materials.files", cached)
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, file.mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply {
                    clipData = ClipData.newUri(context.contentResolver, file.name, uri)
                }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: ActivityNotFoundException) { if (!model.hasActiveSession(session)) return else if (!openWorkspaceLink(context, file)) throw ActivityNotFoundException() }
        catch (error: Exception) { if (!model.hasActiveSession(session)) return else if (!openWorkspaceLink(context, file)) throw error }
    } catch (_: ActivityNotFoundException) { if (model.hasActiveSession(session)) host.showSnackbar(unavailable) }
    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
    catch (_: SecurityException) { if (model.hasActiveSession(session)) host.showSnackbar(failed) }
    catch (_: Exception) { if (model.hasActiveSession(session)) host.showSnackbar(failed) }
}

private fun openWorkspaceLink(context: android.content.Context, file: DriveFile): Boolean {
    val link = file.webViewLink?.let(Uri::parse) ?: return false
    if (link.scheme != "https" || link.host !in setOf("drive.google.com", "docs.google.com")) return false
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, link).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) { false }
}
