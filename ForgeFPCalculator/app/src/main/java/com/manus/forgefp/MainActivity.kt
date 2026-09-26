package com.manus.forgefp

import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manus.forgefp.data.GreatBuilding
import com.manus.forgefp.data.GreatBuildingCatalog
import com.manus.forgefp.data.GreatBuildingImageMap
import com.manus.forgefp.data.GreatBuildingLevel
import com.manus.forgefp.data.GreatBuildingRepository
import com.manus.forgefp.domain.CalculationResult
import com.manus.forgefp.domain.ContributionCalculator
import com.manus.forgefp.domain.LevelInput
import com.manus.forgefp.domain.PlacementResult
import com.manus.forgefp.document.DocumentContent
import com.manus.forgefp.document.DocumentKind
import com.manus.forgefp.document.DocumentStore
import com.manus.forgefp.document.DocumentViewer
import com.manus.forgefp.document.StoredDocument
import com.manus.forgefp.document.loadDocument
import com.manus.forgefp.ui.BackIcon
import com.manus.forgefp.ui.BuildingIcon
import com.manus.forgefp.ui.CheckIcon
import com.manus.forgefp.ui.ChevronRightIcon
import com.manus.forgefp.ui.CloseIcon
import com.manus.forgefp.ui.CopyIcon
import com.manus.forgefp.ui.DeleteIcon
import com.manus.forgefp.ui.ErrorIcon
import com.manus.forgefp.ui.FolderIcon
import com.manus.forgefp.ui.ImportDocumentIcon
import com.manus.forgefp.ui.InfoIcon
import com.manus.forgefp.ui.PdfIcon
import com.manus.forgefp.ui.SearchIcon
import com.manus.forgefp.ui.ShareIcon
import com.manus.forgefp.ui.ShieldIcon
import com.manus.forgefp.ui.TableIcon
import com.manus.forgefp.ui.TrendingUpIcon
import com.manus.forgefp.ui.WarningIcon
import com.manus.forgefp.ui.components.ActionButton
import com.manus.forgefp.ui.components.AssistantCard
import com.manus.forgefp.ui.components.DataColumn
import com.manus.forgefp.ui.components.DataTable
import com.manus.forgefp.ui.components.EmptyState
import com.manus.forgefp.ui.components.ForgeCard
import com.manus.forgefp.ui.components.ForgeHeader
import com.manus.forgefp.ui.components.ForgeStatus
import com.manus.forgefp.ui.components.PrimaryButton
import com.manus.forgefp.ui.components.SectionHeader
import com.manus.forgefp.ui.components.StatusBadge
import com.manus.forgefp.ui.components.rememberAssetBitmap
import com.manus.forgefp.ui.theme.ForgePalette
import com.manus.forgefp.ui.theme.ForgeRadii
import com.manus.forgefp.ui.theme.ForgeSpacing
import com.manus.forgefp.ui.theme.ForgeFpTheme
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ForgeFpTheme { ForgeFpApp() } }
    }
}

private enum class EntryMode { CATALOGUE, MANUAL }

private val frenchIntegers: NumberFormat = NumberFormat.getIntegerInstance(Locale.FRANCE)
private val frenchDecimals = DecimalFormat("0.##", DecimalFormatSymbols(Locale.FRANCE))
private fun Int.fp(): String = frenchIntegers.format(this)
private fun Double.reward(): String = frenchDecimals.format(this)

@Composable
private fun ForgeFpApp() {
    val context = LocalContext.current
    var catalog by remember { mutableStateOf<GreatBuildingCatalog?>(null) }
    var imageMap by remember { mutableStateOf(GreatBuildingImageMap()) }
    var loadingError by remember { mutableStateOf<String?>(null) }
    var showDocuments by remember { mutableStateOf(false) }
    var selectedBuildingId by remember { mutableStateOf<String?>(null) }
    var selectedLevel by remember { mutableIntStateOf(0) }

    // Document importé (PDF ou Excel) et son état de chargement.
    var importedDocument by remember { mutableStateOf<DocumentContent?>(null) }
    var documentLoading by remember { mutableStateOf(false) }
    var documentName by remember { mutableStateOf("") }
    var libraryVersion by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }

    // L'analyse des classeurs (images et mises en forme incluses) et leur copie
    // locale ne doivent pas bloquer le thread UI ni empêcher le loader d'apparaître.
    fun openDocument(uri: android.net.Uri, name: String, kind: DocumentKind, save: Boolean) {
        documentName = name
        documentLoading = true
        scope.launch {
            try {
                val (saved, content) = withContext(Dispatchers.IO) {
                    val stored = if (save) DocumentStore.save(context, uri, name, kind) else null
                    // Lire l'URI d'origine : son MIME reste disponible même si le nom
                    // conservé localement n'a pas d'extension (.xls notamment).
                    stored to loadDocument(context, uri, kind)
                }
                if (saved != null) libraryVersion++
                importedDocument = content
            } finally {
                documentLoading = false
            }
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val name = documentDisplayName(context, uri)
            openDocument(uri, name, documentKindFor(uri, context, name), save = true)
        }
    }

    LaunchedEffect(Unit) {
        val repository = GreatBuildingRepository(context)
        runCatching { repository.loadCatalog() }
            .onSuccess { loaded ->
                catalog = loaded
                val initial = loaded.buildings.firstOrNull { it.id == "Arc" } ?: loaded.buildings.first()
                selectedBuildingId = initial.id
                selectedLevel = initial.levels.first().level
            }
            .onFailure { loadingError = it.message ?: "Impossible de charger les données embarquées." }
        imageMap = repository.loadImageMap()
    }

    BackHandler(enabled = showDocuments && importedDocument == null) { showDocuments = false }

    BoxWithConstraints(Modifier.fillMaxSize().background(ForgePalette.Surface)) {
        val wide = maxWidth >= 720.dp
        Scaffold(
            containerColor = ForgePalette.Surface,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    loadingError != null -> ErrorScreen(loadingError!!)
                    catalog == null -> LoadingScreen()
                    showDocuments -> DocumentsScreen(
                        version = libraryVersion,
                        onBack = { showDocuments = false },
                        onImport = {
                            documentPicker.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.oasis.opendocument.spreadsheet",
                                ),
                            )
                        },
                        onOpen = { stored ->
                            openDocument(android.net.Uri.fromFile(stored.file), stored.displayName, stored.kind, save = false)
                        },
                        onDelete = { stored ->
                            DocumentStore.delete(context, stored.id)
                            libraryVersion++
                        },
                        onNotify = notify,
                    )
                    else -> {
                        val loaded = catalog!!
                        CalculatorScreen(
                            catalog = loaded,
                            imageMap = imageMap,
                            wide = wide,
                            selectedBuildingId = selectedBuildingId ?: loaded.buildings.first().id,
                            onSelectBuilding = { id, level ->
                                selectedBuildingId = id
                                selectedLevel = level
                            },
                            selectedLevel = selectedLevel,
                            onSelectLevel = { selectedLevel = it },
                            onOpenDocuments = { showDocuments = true },
                            onNotify = notify,
                        )
                    }
                }
            }
        }
    }

    if (documentLoading) {
        DocumentLoadingOverlay()
    }

    importedDocument?.let { content ->
        DocumentViewerDialog(
            title = documentName,
            content = content,
            onDismiss = { importedDocument = null },
        )
    }
}

// ============================================================
// ÉCRAN CALCULATEUR
// ============================================================

@Composable
private fun CalculatorScreen(
    catalog: GreatBuildingCatalog,
    imageMap: GreatBuildingImageMap,
    wide: Boolean,
    selectedBuildingId: String,
    onSelectBuilding: (String, Int) -> Unit,
    selectedLevel: Int,
    onSelectLevel: (Int) -> Unit,
    onOpenDocuments: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val selectedBuilding = remember(catalog, selectedBuildingId) {
        catalog.buildings.firstOrNull { it.id == selectedBuildingId } ?: catalog.buildings.first()
    }
    var mode by remember { mutableStateOf(EntryMode.CATALOGUE) }
    var showBuildingPicker by remember { mutableStateOf(false) }
    var showLevelPicker by remember { mutableStateOf(false) }
    var requiredFp by remember { mutableStateOf("") }
    var rewards by remember { mutableStateOf(List(5) { "" }) }
    var ownerFp by remember { mutableStateOf("0") }
    var multiplier by remember { mutableStateOf("1.9") }
    var playerName by remember { mutableStateOf("") }
    var buildingNameOverride by remember { mutableStateOf("") }

    val catalogLevel = selectedBuilding.levels.firstOrNull { it.level == selectedLevel } ?: selectedBuilding.levels.first()
    val effectiveCost = if (mode == EntryMode.CATALOGUE) catalogLevel.cost else requiredFp.toIntOrNull() ?: 0
    val effectiveRewards = if (mode == EntryMode.CATALOGUE) catalogLevel.rewardsAsDouble() else rewards.map { it.replace(',', '.').toDoubleOrNull() ?: 0.0 }
    val effectiveOwner = ownerFp.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val effectiveMultiplier = multiplier.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.9
    val effectiveBuildingName = if (mode == EntryMode.CATALOGUE) selectedBuilding.name else buildingNameOverride
    val displayBuildingName = effectiveBuildingName.ifBlank { "Bâtiment personnalisé" }
    val imagePath = imageMap.imageFor(selectedBuilding.id)

    val result = remember(effectiveCost, effectiveRewards, effectiveOwner, effectiveMultiplier) {
        if (effectiveCost > 0) {
            ContributionCalculator.calculate(LevelInput(effectiveCost, effectiveRewards, effectiveOwner, effectiveMultiplier))
        } else {
            null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ForgeSpacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(ForgeSpacing.lg),
    ) {
        item {
            ForgeHeader(
                buildingName = displayBuildingName,
                buildingId = if (mode == EntryMode.CATALOGUE) selectedBuilding.id else "",
                imagePath = if (mode == EntryMode.CATALOGUE) imagePath else null,
                level = if (mode == EntryMode.CATALOGUE) catalogLevel.level else null,
                wide = wide,
                onOpenDocuments = onOpenDocuments,
            )
        }
        item {
            PageContent(wide) {
                Column(verticalArrangement = Arrangement.spacedBy(ForgeSpacing.lg)) {
                    FlowHeading("01", "PRÉPARATION", "Préparer un niveau", "Choisissez vos données ; le calcul se met à jour automatiquement.")
                    ModeSelector(mode = mode, onModeChange = { mode = it })
                }
            }
        }

        if (wide) {
            item {
                PageContent(wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.lg), verticalAlignment = Alignment.Top) {
                        if (mode == EntryMode.CATALOGUE) {
                            SelectionCard(selectedBuilding, catalogLevel, imagePath,
                                onBuildingClick = { showBuildingPicker = true },
                                onLevelClick = { showLevelPicker = true },
                                modifier = Modifier.weight(1f))
                        } else {
                            ManualEntryCard(requiredFp, { requiredFp = digitsOnly(it) }, rewards,
                                { index, value -> rewards = rewards.toMutableList().also { it[index] = decimalOnly(value) } },
                                modifier = Modifier.weight(1f))
                        }
                        SettingsCard(multiplier, { multiplier = decimalOnly(it) }, ownerFp,
                            { ownerFp = digitsOnly(it) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            item {
                PageContent(wide) {
                    if (mode == EntryMode.CATALOGUE) {
                        SelectionCard(selectedBuilding, catalogLevel, imagePath,
                            onBuildingClick = { showBuildingPicker = true },
                            onLevelClick = { showLevelPicker = true })
                    } else {
                        ManualEntryCard(requiredFp, { requiredFp = digitsOnly(it) }, rewards,
                            { index, value -> rewards = rewards.toMutableList().also { it[index] = decimalOnly(value) } })
                    }
                }
            }
            item {
                PageContent(wide) {
                    SettingsCard(multiplier, { multiplier = decimalOnly(it) }, ownerFp,
                        { ownerFp = digitsOnly(it) })
                }
            }
        }

        if (result != null) {
            item {
                PageContent(wide) {
                    Column(verticalArrangement = Arrangement.spacedBy(ForgeSpacing.md)) {
                        FlowHeading("02", "RÉSULTAT", "Votre stratégie en un coup d'œil", "Les montants sont recalculés à chaque modification.")
                        ResultPreviewCard(
                            buildingName = displayBuildingName,
                            level = if (mode == EntryMode.CATALOGUE) catalogLevel.level else null,
                            result = result,
                        )
                    }
                }
            }
            item {
                PageContent(wide) {
                    PlacementsHeader(
                        playerName = playerName,
                        onPlayerNameChange = { playerName = it },
                        buildingName = effectiveBuildingName,
                        onBuildingNameChange = { buildingNameOverride = it },
                        buildingNameEditable = mode == EntryMode.MANUAL,
                        wide = wide,
                        onCopy = {
                            copyToClipboard(
                                context = context,
                                label = "Emplacements à publier",
                                text = buildPlacementsClipboardText(result, playerName, displayBuildingName),
                            )
                            onNotify("Emplacements copiés dans le presse-papiers")
                        },
                        onShare = {
                            shareText(context, buildPlacementsClipboardText(result, playerName, displayBuildingName))
                        },
                    )
                }
            }
            item { PageContent(wide) { PlacementsTable(result) } }
            if (wide) {
                items(result.placements.chunked(2), key = { it.first().placement }) { pair ->
                    PageContent(wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.lg)) {
                            pair.forEach { placement ->
                                PlacementCard(placement, result.ownerAlreadyInvested, Modifier.weight(1f))
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(result.placements, key = { it.placement }) { placement ->
                    PageContent(wide) { PlacementCard(placement, result.ownerAlreadyInvested) }
                }
            }
            item { PageContent(wide) { SequenceCard(result) } }
            item { PageContent(wide) { MethodCard(result.multiplier) } }
        } else {
            item {
                PageContent(wide) {
                    EmptyState(
                        title = "Calcul en attente",
                        message = "Saisissez un coût de niveau supérieur à 0 pour lancer le calcul.",
                        icon = InfoIcon,
                    )
                }
            }
        }
    }

    if (showBuildingPicker) {
        BuildingPickerDialog(
            buildings = catalog.buildings,
            imageMap = imageMap,
            selectedId = selectedBuilding.id,
            onSelect = { building ->
                onSelectBuilding(building.id, building.levels.first().level)
                showBuildingPicker = false
            },
            onDismiss = { showBuildingPicker = false },
        )
    }
    if (showLevelPicker) {
        LevelPickerDialog(
            building = selectedBuilding,
            selectedLevel = catalogLevel.level,
            onSelect = { level -> onSelectLevel(level); showLevelPicker = false },
            onDismiss = { showLevelPicker = false },
        )
    }
}

@Composable
private fun PageContent(wide: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = if (wide) ForgeSpacing.xxxl else ForgeSpacing.lg),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.widthIn(max = 1040.dp).fillMaxWidth()) { content() }
    }
}

@Composable
private fun FlowHeading(step: String, category: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(ForgeSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.sm)) {
            Box(Modifier.width(20.dp).height(2.dp).background(ForgePalette.GoldStrong))
            Text("$step / $category", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = ForgePalette.GoldStrong, letterSpacing = 1.sp)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = ForgePalette.TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ForgePalette.TextSecondary)
    }
}

@Composable
private fun ModeSelector(mode: EntryMode, onModeChange: (EntryMode) -> Unit) {
    Surface(shape = RoundedCornerShape(ForgeRadii.md), color = ForgePalette.Card,
        border = BorderStroke(1.dp, ForgePalette.Border)) {
        Row(Modifier.fillMaxWidth().padding(ForgeSpacing.xs), horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.xs)) {
            ModeSelectorItem("Catalogue", mode == EntryMode.CATALOGUE, Modifier.weight(1f)) { onModeChange(EntryMode.CATALOGUE) }
            ModeSelectorItem("Saisie manuelle", mode == EntryMode.MANUAL, Modifier.weight(1f)) { onModeChange(EntryMode.MANUAL) }
        }
    }
}

@Composable
private fun ModeSelectorItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(ForgeRadii.sm),
        color = if (selected) ForgePalette.BackgroundDark else Color.Transparent,
        contentColor = if (selected) ForgePalette.OnDarkPrimary else ForgePalette.TextSecondary,
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = ForgeSpacing.md), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SelectionCard(
    building: GreatBuilding,
    level: GreatBuildingLevel,
    imagePath: String?,
    onBuildingClick: () -> Unit,
    onLevelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ForgeCard(modifier = modifier) {
        SectionHeader("Sélection du bâtiment", subtitle = "Choisissez le bâtiment et le niveau à préparer.")
        Spacer(Modifier.height(ForgeSpacing.lg))
        SelectorRow("GRAND BÂTIMENT", building.name, building.id, BuildingIcon, imagePath, onBuildingClick)
        Spacer(Modifier.height(ForgeSpacing.sm))
        SelectorRow("NIVEAU CIBLE", "Niveau ${level.level}", "${level.cost.fp()} FP requis", TrendingUpIcon, null, onLevelClick)
        Spacer(Modifier.height(ForgeSpacing.xl))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RÉCOMPENSES DE BASE", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = ForgePalette.TextSecondary, letterSpacing = 0.7.sp)
            Text("EN FP", style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary)
        }
        Spacer(Modifier.height(ForgeSpacing.sm))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 480.dp) 5 else 3
            Column(verticalArrangement = Arrangement.spacedBy(ForgeSpacing.sm)) {
                level.rewardsAsDouble().withIndex().toList().chunked(columns).forEach { group ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.sm)) {
                        group.forEach { (index, reward) -> RewardChip(index + 1, reward, Modifier.weight(1f)) }
                        repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorRow(
    label: String,
    main: String,
    support: String,
    icon: ImageVector,
    imagePath: String?,
    onClick: () -> Unit,
) {
    val bitmap = rememberAssetBitmap(imagePath, targetWidthPx = 120)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ForgeRadii.md),
        color = ForgePalette.SurfaceSecondary,
        border = BorderStroke(1.dp, ForgePalette.Border),
    ) {
        Row(Modifier.padding(ForgeSpacing.md), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.md)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(ForgeRadii.sm))
                .background(ForgePalette.BackgroundDark), contentAlignment = Alignment.Center) {
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(ForgeSpacing.xs))
                } else {
                    Icon(icon, contentDescription = null, tint = ForgePalette.Gold, modifier = Modifier.size(21.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(main, style = MaterialTheme.typography.titleSmall, color = ForgePalette.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(support, style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(ChevronRightIcon, contentDescription = null, tint = ForgePalette.PrimaryStrong, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RewardChip(placement: Int, reward: Double, modifier: Modifier) {
    val highlighted = placement == 1
    Surface(
        modifier = modifier,
        color = if (highlighted) ForgePalette.GoldSoft else ForgePalette.SurfaceSecondary,
        shape = RoundedCornerShape(ForgeRadii.sm),
    ) {
        Column(Modifier.padding(vertical = ForgeSpacing.sm, horizontal = ForgeSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("P$placement", style = MaterialTheme.typography.labelSmall,
                color = if (highlighted) ForgePalette.GoldStrong else ForgePalette.TextSecondary)
            Text(reward.reward(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = ForgePalette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ManualEntryCard(
    requiredFp: String,
    onRequiredFpChange: (String) -> Unit,
    rewards: List<String>,
    onRewardChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ForgeCard(modifier = modifier) {
        SectionHeader("Saisie manuelle", subtitle = "Renseignez le coût et les cinq récompenses de base.")
        Spacer(Modifier.height(ForgeSpacing.lg))
        NumericField("Coût du niveau", requiredFp, onRequiredFpChange, suffix = "FP")
        Spacer(Modifier.height(ForgeSpacing.lg))
        Text("RÉCOMPENSES DE BASE", style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, color = ForgePalette.TextSecondary, letterSpacing = 0.7.sp)
        Spacer(Modifier.height(ForgeSpacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(ForgeSpacing.sm)) {
            rewards.withIndex().toList().chunked(2).forEach { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.sm)) {
                    group.forEach { (index, value) ->
                        NumericField("Place P${index + 1}", value, { onRewardChange(index, it) }, suffix = "FP",
                            modifier = Modifier.weight(1f))
                    }
                    if (group.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    multiplier: String,
    onMultiplierChange: (String) -> Unit,
    ownerFp: String,
    onOwnerFpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ForgeCard(modifier = modifier) {
        SectionHeader("Paramètres du fil", subtitle = "Adaptez le calcul à votre guilde et à votre avance.")
        Spacer(Modifier.height(ForgeSpacing.lg))
        NumericField("Multiplicateur", multiplier, onMultiplierChange, keyboardType = KeyboardType.Decimal, suffix = "×")
        Spacer(Modifier.height(ForgeSpacing.md))
        NumericField("FP propriétaire déjà placés", ownerFp, onOwnerFpChange, suffix = "FP")
        Spacer(Modifier.height(ForgeSpacing.lg))
        AssistantCard(
            title = "Bon à savoir",
            message = "Indiquez uniquement vos propres FP. Les places précédentes sont prises en compte dans les seuils ci-dessous.",
        )
    }
}

@Composable
private fun NumericField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Number,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        suffix = { Text(suffix, color = ForgePalette.TextSecondary, style = MaterialTheme.typography.labelMedium) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(ForgeRadii.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ForgePalette.Primary,
            unfocusedBorderColor = ForgePalette.Border,
            focusedContainerColor = ForgePalette.Card,
            unfocusedContainerColor = ForgePalette.Card,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Résumé sans second visuel du bâtiment : l'illustration figure déjà dans le bandeau. */
@Composable
private fun ResultPreviewCard(
    buildingName: String,
    level: Int?,
    result: CalculationResult,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ForgeRadii.xl)
    Surface(modifier = modifier, shape = shape, color = ForgePalette.BackgroundDark,
        border = BorderStroke(1.dp, ForgePalette.Gold.copy(alpha = 0.35f))) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(
                listOf(ForgePalette.BackgroundDark, ForgePalette.BackgroundDarkSecondary),
            )).padding(ForgeSpacing.xxl),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("BILAN DU NIVEAU", style = MaterialTheme.typography.labelSmall,
                        color = ForgePalette.Gold, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(buildingName + (level?.let { " · Niv. $it" } ?: ""),
                        style = MaterialTheme.typography.titleSmall, color = ForgePalette.OnDarkSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(ForgeSpacing.sm))
                Text("× ${result.multiplier.reward()}", style = MaterialTheme.typography.labelMedium,
                    color = ForgePalette.OnDarkPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(ForgeRadii.pill))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.xs))
            }
            Spacer(Modifier.height(ForgeSpacing.xl))
            Text(result.requiredFp.fp(), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 46.sp),
                color = ForgePalette.OnDarkPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("FP nécessaires pour ce niveau", style = MaterialTheme.typography.bodySmall, color = ForgePalette.OnDarkSecondary)
            Spacer(Modifier.height(ForgeSpacing.xl))
            HorizontalDivider(color = ForgePalette.DarkDivider)
            Spacer(Modifier.height(ForgeSpacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.md)) {
                ResultMetric("À AJOUTER AU TOTAL", "${result.totalOwnerAdd.fp()} FP", ForgePalette.Gold, Modifier.weight(1f))
                ResultMetric("DÉJÀ INVESTIS", "${result.ownerAlreadyInvested.fp()} FP", ForgePalette.OnDarkPrimary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(ForgeSpacing.md))
            Text("Investissement propriétaire final : ${result.finalOwnerInvestment.fp()} FP",
                style = MaterialTheme.typography.labelSmall, color = ForgePalette.OnDarkSecondary)
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ForgeSpacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ForgePalette.OnDarkSecondary, maxLines = 2)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlacementsHeader(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    buildingName: String,
    onBuildingNameChange: (String) -> Unit,
    buildingNameEditable: Boolean,
    wide: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ForgeSpacing.md)) {
        FlowHeading("03", "CONTRIBUTIONS", "Emplacements à publier", "Personnalisez le message, puis publiez de P1 à P5.")
        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.md)) {
                PlacementTextField(playerName, onPlayerNameChange, "Nom du joueur (facultatif)", Modifier.weight(1f))
                PlacementTextField(buildingName, onBuildingNameChange, "Grand Bâtiment", Modifier.weight(1f),
                    readOnly = !buildingNameEditable)
            }
        } else {
            PlacementTextField(playerName, onPlayerNameChange, "Nom du joueur (facultatif)")
            PlacementTextField(buildingName, onBuildingNameChange, "Grand Bâtiment", readOnly = !buildingNameEditable)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.sm)) {
            ActionButton(label = if (wide) "Copier le message" else "Copier", onClick = onCopy, icon = CopyIcon, modifier = Modifier.weight(1f))
            ActionButton(label = "Partager", onClick = onShare, icon = ShareIcon, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlacementTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        readOnly = readOnly, singleLine = true,
        shape = RoundedCornerShape(ForgeRadii.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ForgePalette.Primary,
            unfocusedBorderColor = ForgePalette.Border,
            focusedContainerColor = ForgePalette.Card,
            unfocusedContainerColor = ForgePalette.Card,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Tableau complet sur tablette ; synthèse lisible sans défilement horizontal sur téléphone. */
@Composable
private fun PlacementsTable(result: CalculationResult, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth >= 680.dp) {
            val columns = listOf(
                DataColumn("Place", weight = 0.7f),
                DataColumn("Récompense", weight = 1.2f, align = androidx.compose.ui.text.style.TextAlign.End),
                DataColumn("À publier", weight = 1.2f, align = androidx.compose.ui.text.style.TextAlign.End),
                DataColumn("Seuil proprio", weight = 1.2f, align = androidx.compose.ui.text.style.TextAlign.End),
                DataColumn("À ajouter", weight = 1.1f, align = androidx.compose.ui.text.style.TextAlign.End),
            )
            val rows = result.placements.map { placement ->
                listOf("P${placement.placement}", "${placement.baseReward.reward()} FP",
                    "${placement.donorContribution.fp()} FP", "${placement.antiSnipeOwnerTotal.fp()} FP",
                    if (placement.ownerToAdd > 0) "+${placement.ownerToAdd.fp()} FP" else "—")
            }
            ForgeCard {
                Text("Vue d'ensemble", style = MaterialTheme.typography.titleMedium, color = ForgePalette.TextPrimary)
                Spacer(Modifier.height(ForgeSpacing.md))
                DataTable(columns = columns, rows = rows)
            }
        } else {
            ForgeCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VUE RAPIDE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = ForgePalette.TextSecondary, letterSpacing = 0.8.sp)
                    Text("${result.placements.size} PLACES", style = MaterialTheme.typography.labelSmall, color = ForgePalette.GoldStrong)
                }
                Spacer(Modifier.height(ForgeSpacing.sm))
                result.placements.forEachIndexed { index, placement ->
                    if (index > 0) HorizontalDivider(color = ForgePalette.Border)
                    Row(Modifier.fillMaxWidth().padding(vertical = ForgeSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(ForgePalette.GoldSoft, RoundedCornerShape(ForgeRadii.sm)),
                            contentAlignment = Alignment.Center) {
                            Text("P${placement.placement}", style = MaterialTheme.typography.labelLarge,
                                color = ForgePalette.GoldStrong, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(ForgeSpacing.md))
                        Column(Modifier.weight(1f)) {
                            Text("À PUBLIER", style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary)
                            Text("${placement.donorContribution.fp()} FP", style = MaterialTheme.typography.titleSmall,
                                color = ForgePalette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("À AJOUTER", style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary)
                            Text(if (placement.ownerToAdd > 0) "+${placement.ownerToAdd.fp()} FP" else "0 FP",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (placement.ownerToAdd > 0) ForgePalette.PrimaryStrong else ForgePalette.SuccessStrong,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacementCard(placement: PlacementResult, ownerAlreadyInvested: Int, modifier: Modifier = Modifier) {
    val protected = ownerAlreadyInvested >= placement.antiSnipeOwnerTotal
    ForgeCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(if (protected) ForgePalette.SurfaceSecondary else ForgePalette.GoldSoft,
                RoundedCornerShape(ForgeRadii.md)), contentAlignment = Alignment.Center) {
                Text("P${placement.placement}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = if (protected) ForgePalette.SuccessStrong else ForgePalette.GoldStrong)
            }
            Spacer(Modifier.width(ForgeSpacing.sm))
            Column(Modifier.weight(1f)) {
                Text("PLACE ${placement.placement}", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = ForgePalette.TextSecondary, letterSpacing = 0.6.sp)
                Text("Récompense ${placement.baseReward.reward()} FP", style = MaterialTheme.typography.labelSmall,
                    color = ForgePalette.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusBadge(status = if (protected) ForgeStatus.SUCCESS else ForgeStatus.WARNING,
                text = if (protected) "Protégée" else "À sécuriser",
                icon = if (protected) ShieldIcon else WarningIcon)
        }
        Spacer(Modifier.height(ForgeSpacing.lg))
        Text("MONTANT À PUBLIER", style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, color = ForgePalette.TextSecondary, letterSpacing = 0.7.sp)
        Text("${placement.donorContribution.fp()} FP", style = MaterialTheme.typography.headlineSmall,
            color = ForgePalette.TextPrimary)
        Spacer(Modifier.height(ForgeSpacing.md))
        Row(
            Modifier.fillMaxWidth().background(ForgePalette.SurfaceSecondary, RoundedCornerShape(ForgeRadii.md))
                .padding(ForgeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("SEUIL PROPRIÉTAIRE", style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary, maxLines = 2)
                Text("${placement.antiSnipeOwnerTotal.fp()} FP", style = MaterialTheme.typography.titleSmall,
                    color = ForgePalette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.width(1.dp).height(38.dp).background(ForgePalette.Border))
            Column(Modifier.weight(1f).padding(start = ForgeSpacing.md)) {
                Text("À AJOUTER", style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary)
                Text(if (placement.ownerToAdd > 0) "+${placement.ownerToAdd.fp()} FP" else "0 FP",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (placement.ownerToAdd > 0) ForgePalette.PrimaryStrong else ForgePalette.SuccessStrong,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(ForgeSpacing.sm))
        Text("Places supérieures prises en compte : ${placement.previousDonorContributions.fp()} FP",
            style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary)
    }
}

@Composable
private fun SequenceCard(result: CalculationResult, modifier: Modifier = Modifier) {
    ForgeCard(modifier = modifier) {
        FlowHeading("04", "PAS À PAS", "L'ordre recommandé", "Ajoutez vos FP au fur et à mesure des attributions.")
        Spacer(Modifier.height(ForgeSpacing.xl))
        result.sequentialOwnerAdd.forEachIndexed { index, add ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(32.dp).background(if (index == 0) ForgePalette.GoldSoft else ForgePalette.SurfaceSecondary,
                        CircleShape), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                            color = if (index == 0) ForgePalette.GoldStrong else ForgePalette.TextPrimary)
                    }
                    if (index < result.sequentialOwnerAdd.lastIndex) {
                        Box(Modifier.width(2.dp).height(32.dp).background(ForgePalette.Border))
                    }
                }
                Spacer(Modifier.width(ForgeSpacing.md))
                Column(Modifier.weight(1f)) {
                    Text(if (index == 0) "Avant de publier P1" else "Après attribution de P$index",
                        style = MaterialTheme.typography.titleSmall, color = ForgePalette.TextPrimary)
                    Text(if (add == 0) "Aucun ajout nécessaire" else "Ajout propriétaire",
                        style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary)
                }
                Spacer(Modifier.width(ForgeSpacing.sm))
                Text(if (add == 0) "0 FP" else "+${add.fp()} FP", style = MaterialTheme.typography.labelLarge,
                    color = if (add == 0) ForgePalette.SuccessStrong else ForgePalette.PrimaryStrong,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(ForgeSpacing.md))
        Row(
            Modifier.fillMaxWidth().background(ForgePalette.GoldSoft, RoundedCornerShape(ForgeRadii.md))
                .padding(ForgeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Propriétaire au final", style = MaterialTheme.typography.labelLarge,
                color = ForgePalette.GoldStrong, modifier = Modifier.weight(1f))
            Text("${result.finalOwnerInvestment.fp()} FP", style = MaterialTheme.typography.titleSmall,
                color = ForgePalette.GoldStrong)
        }
    }
}

@Composable
private fun MethodCard(multiplier: Double, modifier: Modifier = Modifier) {
    AssistantCard(
        modifier = modifier,
        title = "Comment sont calculés les seuils ?",
        message = "Contribution = ⌈récompense × ${multiplier.reward()}⌉. " +
            "Seuil propriétaire Pn = coût du niveau − contributions P1…P(n−1) − 2 × contribution Pn. " +
            "Les ajouts supposent que les places précédentes sont attribuées au montant affiché.",
        status = ForgeStatus.GOLD,
    )
}

// ============================================================
// ÉCRAN DOCUMENTS
// ============================================================

@Composable
private fun DocumentsScreen(
    version: Int,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onOpen: (StoredDocument) -> Unit,
    onDelete: (StoredDocument) -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val documents = remember(version) { DocumentStore.list(context) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = ForgeSpacing.lg, bottom = ForgeSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(ForgeSpacing.lg),
        ) {
            item {
                PageContent(wide) {
                    Surface(shape = RoundedCornerShape(ForgeRadii.xl), color = ForgePalette.BackgroundDark) {
                        Column(
                            Modifier.fillMaxWidth().background(Brush.linearGradient(
                                listOf(ForgePalette.BackgroundDark, ForgePalette.BackgroundDarkSecondary),
                            )).padding(ForgeSpacing.xxl),
                        ) {
                            Surface(
                                onClick = onBack,
                                color = Color.White.copy(alpha = 0.10f),
                                contentColor = ForgePalette.OnDarkPrimary,
                                shape = RoundedCornerShape(ForgeRadii.pill),
                            ) {
                                Row(Modifier.padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(BackIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(ForgeSpacing.xs))
                                    Text("Calculateur", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Spacer(Modifier.height(ForgeSpacing.xxl))
                            Text("VOTRE ESPACE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp, color = ForgePalette.Gold)
                            Text("Documents", style = MaterialTheme.typography.headlineMedium, color = ForgePalette.OnDarkPrimary)
                            Text("Gardez vos PDF et vos classeurs à portée de main.",
                                style = MaterialTheme.typography.bodySmall, color = ForgePalette.OnDarkSecondary)
                            Spacer(Modifier.height(ForgeSpacing.xxl))
                            PrimaryButton("Importer un document", onImport, icon = ImportDocumentIcon)
                        }
                    }
                }
            }
            item {
                PageContent(wide) {
                    SectionHeader("Ma bibliothèque", subtitle = "${documents.size} document${if (documents.size > 1) "s" else ""} enregistré${if (documents.size > 1) "s" else ""}")
                }
            }
            if (documents.isEmpty()) {
                item {
                    PageContent(wide) {
                        EmptyState(
                            title = "Aucun document pour le moment",
                            message = "Importez un PDF ou un classeur Excel pour le retrouver ici.",
                            icon = FolderIcon,
                        )
                    }
                }
            } else {
                items(documents, key = { it.id }) { document ->
                    PageContent(wide) {
                        DocumentRow(
                            document = document,
                            onOpen = { onOpen(document) },
                            onDelete = {
                                onDelete(document)
                                onNotify("Document supprimé")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(document: StoredDocument, onOpen: () -> Unit, onDelete: () -> Unit) {
    ForgeCard(contentPadding = PaddingValues(ForgeSpacing.sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onOpen, modifier = Modifier.weight(1f), color = Color.Transparent,
                shape = RoundedCornerShape(ForgeRadii.md)) {
                Row(Modifier.padding(ForgeSpacing.sm), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.md)) {
                    Box(Modifier.size(44.dp).background(
                        if (document.kind == DocumentKind.PDF) ForgePalette.GoldSoft else ForgePalette.SurfaceSecondary,
                        RoundedCornerShape(ForgeRadii.md)), contentAlignment = Alignment.Center) {
                        Icon(if (document.kind == DocumentKind.PDF) PdfIcon else TableIcon,
                            contentDescription = null,
                            tint = if (document.kind == DocumentKind.PDF) ForgePalette.GoldStrong else ForgePalette.PrimaryStrong,
                            modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(document.displayName, style = MaterialTheme.typography.titleSmall,
                            color = ForgePalette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${if (document.kind == DocumentKind.PDF) "PDF" else "Excel"} · ${formatSize(document.sizeBytes)}",
                            style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary)
                    }
                    Icon(ChevronRightIcon, contentDescription = null,
                        tint = ForgePalette.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(DeleteIcon, contentDescription = "Supprimer ${document.displayName}",
                    tint = ForgePalette.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ============================================================
// ÉCRANS D'ÉTAT
// ============================================================

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ForgeSpacing.lg)) {
            CircularProgressIndicator()
            Text("Chargement des Grands Bâtiments…", color = ForgePalette.TextSecondary)
        }
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Box(Modifier.fillMaxSize().padding(ForgeSpacing.xxl), contentAlignment = Alignment.Center) {
        ForgeCard(containerColor = ForgePalette.SurfaceSecondary) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ErrorIcon, contentDescription = null, tint = ForgePalette.Danger, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(ForgeSpacing.sm))
                Text("Erreur de chargement", style = MaterialTheme.typography.titleSmall, color = ForgePalette.DangerStrong)
            }
            Spacer(Modifier.height(ForgeSpacing.sm))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ForgePalette.TextSecondary)
        }
    }
}

@Composable
private fun DocumentLoadingOverlay() {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(ForgeRadii.lg), color = ForgePalette.Card) {
            Column(
                Modifier.padding(ForgeSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ForgeSpacing.md),
            ) {
                LinearProgressIndicator(Modifier.width(180.dp))
                Text("Ouverture du document…", color = ForgePalette.TextPrimary)
            }
        }
    }
}

/** Affiche le document importé en plein écran, avec un en-tête et un bouton de fermeture. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DocumentViewerDialog(title: String, content: DocumentContent, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = ForgePalette.Card) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(title.ifBlank { "Document importé" }, fontWeight = FontWeight.Bold, maxLines = 1, color = ForgePalette.TextPrimary)
                            Text(
                                when (content) {
                                    is DocumentContent.Pdf -> "PDF • ${content.reader.pageCount} page(s)"
                                    is DocumentContent.Spreadsheet -> "Classeur • ${content.data.sheets.size} feuille(s)"
                                    is DocumentContent.Failure -> "Lecture impossible"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = ForgePalette.TextSecondary,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = CloseIcon, contentDescription = "Fermer", tint = ForgePalette.TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ForgePalette.Card),
                )
                HorizontalDivider(color = ForgePalette.Border)
                DocumentViewer(content, Modifier.weight(1f))
            }
        }
    }
}

// ============================================================
// SÉLECTEURS
// ============================================================

@Composable
private fun BuildingPickerDialog(
    buildings: List<GreatBuilding>,
    imageMap: GreatBuildingImageMap,
    selectedId: String,
    onSelect: (GreatBuilding) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filtered = buildings.filter {
        it.name.contains(search, ignoreCase = true) || it.id.contains(search, ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(ForgeRadii.xl),
        containerColor = ForgePalette.Card,
        title = { Text("Choisir un Grand Bâtiment", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Rechercher un bâtiment") },
                    leadingIcon = { Icon(SearchIcon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(ForgeRadii.md),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(ForgeSpacing.md))
                Text("${filtered.size} BÂTIMENT${if (filtered.size > 1) "S" else ""}",
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                    color = ForgePalette.TextSecondary, letterSpacing = 0.7.sp)
                Spacer(Modifier.height(ForgeSpacing.xs))
                if (filtered.isEmpty()) {
                    Text("Aucun bâtiment trouvé.", style = MaterialTheme.typography.bodyMedium,
                        color = ForgePalette.TextSecondary, modifier = Modifier.padding(vertical = ForgeSpacing.xxl))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(ForgeSpacing.xs)) {
                        items(filtered, key = { it.id }) { building ->
                            val selected = building.id == selectedId
                            val bitmap = rememberAssetBitmap(imageMap.imageFor(building.id), targetWidthPx = 120)
                            Surface(onClick = { onSelect(building) },
                                color = if (selected) ForgePalette.SurfaceSecondary else Color.Transparent,
                                shape = RoundedCornerShape(ForgeRadii.md)) {
                                Row(Modifier.fillMaxWidth().padding(ForgeSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(ForgeRadii.sm))
                                        .background(ForgePalette.BackgroundDark), contentAlignment = Alignment.Center) {
                                        if (bitmap != null) {
                                            Image(bitmap.asImageBitmap(), contentDescription = null,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize().padding(ForgeSpacing.xs))
                                        } else {
                                            Icon(BuildingIcon, contentDescription = null,
                                                tint = ForgePalette.OnDarkSecondary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(ForgeSpacing.md))
                                    Column(Modifier.weight(1f)) {
                                        Text(building.name, style = MaterialTheme.typography.titleSmall,
                                            color = if (selected) ForgePalette.PrimaryStrong else ForgePalette.TextPrimary,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Niveaux ${building.levels.first().level}–${building.levels.last().level}",
                                            style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary)
                                    }
                                    if (selected) Icon(CheckIcon, contentDescription = "Sélectionné",
                                        tint = ForgePalette.PrimaryStrong, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

@Composable
private fun LevelPickerDialog(building: GreatBuilding, selectedLevel: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val selectedIndex = remember(building, selectedLevel) {
        building.levels.indexOfFirst { it.level == selectedLevel }.coerceAtLeast(0)
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(ForgeRadii.xl),
        containerColor = ForgePalette.Card,
        title = {
            Column {
                Text("Niveau cible", style = MaterialTheme.typography.titleLarge)
                Text(building.name, style = MaterialTheme.typography.bodySmall, color = ForgePalette.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            LazyColumn(state = listState, modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(ForgeSpacing.xs)) {
                items(building.levels, key = { it.level }) { level ->
                    val selected = level.level == selectedLevel
                    Surface(onClick = { onSelect(level.level) },
                        color = if (selected) ForgePalette.SurfaceSecondary else Color.Transparent,
                        shape = RoundedCornerShape(ForgeRadii.md)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.md),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Niveau ${level.level}", style = MaterialTheme.typography.titleSmall,
                                color = if (selected) ForgePalette.PrimaryStrong else ForgePalette.TextPrimary,
                                modifier = Modifier.weight(1f))
                            Text("${level.cost.fp()} FP", style = MaterialTheme.typography.labelMedium,
                                color = ForgePalette.TextSecondary)
                            if (selected) {
                                Spacer(Modifier.width(ForgeSpacing.sm))
                                Icon(CheckIcon, contentDescription = "Sélectionné",
                                    tint = ForgePalette.PrimaryStrong, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

// ============================================================
// UTILITAIRES
// ============================================================

private fun digitsOnly(value: String): String = value.filter(Char::isDigit)
private fun decimalOnly(value: String): String = value.filter { it.isDigit() || it == ',' || it == '.' }.let {
    val firstSeparator = it.indexOfFirst { char -> char == ',' || char == '.' }
    if (firstSeparator < 0) it else it.take(firstSeparator + 1) + it.drop(firstSeparator + 1).filter { char -> char.isDigit() }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(Locale.FRANCE, "%.1f Mo", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format(Locale.FRANCE, "%.0f Ko", bytes / 1024.0)
    else -> "$bytes o"
}

/** Le nom d'un document SAF n'est pas nécessairement dans lastPathSegment. */
private fun documentDisplayName(context: android.content.Context, uri: android.net.Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Document"

/** Détermine le type de document à partir de son URI (extension ou type MIME). */
private fun documentKindFor(uri: android.net.Uri, context: android.content.Context, displayName: String): DocumentKind {
    val name = displayName.lowercase()
    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()?.lowercase() ?: ""
    val isExcel = name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ods") ||
        mime.contains("spreadsheet") || mime.contains("ms-excel") || mime.contains("excel")
    return if (isExcel) DocumentKind.EXCEL else DocumentKind.PDF
}

/**
 * Construit un résumé en texte brut, prêt à coller (P5..P1), incluant le nom
 * du joueur et le nom du Grand Bâtiment.
 *
 * Format : Nom du joueur Grand Bâtiment P5 (10)  P4 (48)  P3 (181)  P2 (542)  P1 (1083)
 */
private fun buildPlacementsClipboardText(result: CalculationResult, playerName: String, buildingName: String): String {
    val placements = result.placements
        .sortedByDescending { it.placement }
        .joinToString("  ") { placement -> "P${placement.placement} (${placement.donorContribution})" }
    val player = if (playerName.isNotBlank()) "$playerName " else ""
    return "$player$buildingName  $placements"
}

private fun copyToClipboard(context: android.content.Context, label: String, text: String) {
    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

private fun shareText(context: android.content.Context, text: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Partager les emplacements"))
}
