// APM.kt - 俇淕党蜊唳
package me.kdufse.apatch.plus.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ApmBulkInstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlineModuleScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kdufse.apatch.plus.APApplication
import me.kdufse.apatch.plus.R
import me.kdufse.apatch.plus.apApp
import me.kdufse.apatch.plus.ui.WebUIActivity
import me.kdufse.apatch.plus.ui.component.ConfirmResult
import me.kdufse.apatch.plus.ui.component.ModuleRemoveButton
import me.kdufse.apatch.plus.ui.component.ModuleStateIndicator
import me.kdufse.apatch.plus.ui.component.ModuleUpdateButton
import me.kdufse.apatch.plus.ui.component.SearchAppBar
import me.kdufse.apatch.plus.ui.component.WallpaperAwareDropdownMenu
import me.kdufse.apatch.plus.ui.component.WallpaperAwareDropdownMenuItem
import me.kdufse.apatch.plus.ui.component.rememberConfirmDialog
import me.kdufse.apatch.plus.ui.component.rememberLoadingDialog
import me.kdufse.apatch.plus.ui.viewmodel.APModuleViewModel
import me.kdufse.apatch.plus.util.DownloadListener
import me.kdufse.apatch.plus.util.ModuleBackupUtils
import me.kdufse.apatch.plus.util.download
import me.kdufse.apatch.plus.util.hasMagisk
import me.kdufse.apatch.plus.util.reboot
import me.kdufse.apatch.plus.util.toggleModule
import me.kdufse.apatch.plus.util.ui.LocalSnackbarHost
import me.kdufse.apatch.plus.util.uninstallModule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun APModuleScreen(navigator: DestinationsNavigator) {
    val snackBarHost = LocalSnackbarHost.current
    val context = LocalContext.current

    // First use dialog state
    val prefs = remember { APApplication.sharedPreferences }
    var showFirstTimeDialog by remember { 
        mutableStateOf(!prefs.getBoolean("apm_first_use_shown", false)) 
    }
    var dontShowAgain by remember { mutableStateOf(false) }

    var showMoreModuleInfo by remember { mutableStateOf(prefs.getBoolean("show_more_module_info", false)) }
    var useBanner by remember { mutableStateOf(prefs.getBoolean("use_banner", true)) }
    
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            when (key) {
                "show_more_module_info" -> showMoreModuleInfo = sharedPrefs.getBoolean("show_more_module_info", false)
                "use_banner" -> useBanner = sharedPrefs.getBoolean("use_banner", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    if (state != APApplication.State.ANDROIDPATCH_INSTALLED && state != APApplication.State.ANDROIDPATCH_NEED_UPDATE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(id = R.string.apm_not_installed),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    val viewModel = viewModel<APModuleViewModel>()

    LaunchedEffect(Unit) {
        if (viewModel.moduleList.isEmpty() || viewModel.isNeedRefresh) {
            viewModel.fetchModuleList()
        }
    }

    var pendingInstallUri by remember { mutableStateOf<Uri?>(null) }
    val installConfirmDialog = rememberConfirmDialog(
        onConfirm = {
            pendingInstallUri?.let { uri ->
                navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                viewModel.markNeedRefresh()
            }
            pendingInstallUri = null
        },
        onDismiss = {
            pendingInstallUri = null
        }
    )

    val webUILauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.fetchModuleList() }
    //TODO: FIXME -> val isSafeMode = Natives.getSafeMode()
    val isSafeMode = false
    val hasMagisk = hasMagisk()
    val hideInstallButton = isSafeMode || hasMagisk

    val moduleListState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    val filteredModuleList = remember(viewModel.moduleList, searchQuery) {
        if (searchQuery.isEmpty()) {
            viewModel.moduleList
        } else {
            viewModel.moduleList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
        TopBar(navigator, viewModel, snackBarHost, searchQuery) { searchQuery = it }
    }, floatingActionButton = if (hideInstallButton) {
        { /* Empty */ }
    } else {
        {
            val selectZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode != RESULT_OK) {
                    return@rememberLauncherForActivityResult
                }
                val data = it.data ?: return@rememberLauncherForActivityResult
                val uri = data.data ?: return@rememberLauncherForActivityResult

                Log.i("ModuleScreen", "select zip result: $uri")

                val prefs = APApplication.sharedPreferences
                if (prefs.getBoolean("apm_install_confirm_enabled", true)) {
                    pendingInstallUri = uri
                    val fileName = try {
                        var name = uri.path ?: "Module"
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex >= 0) {
                                name = cursor.getString(nameIndex)
                            }
                        }
                        name
                    } catch (e: Exception) {
                        "Module"
                    }
                    installConfirmDialog.showConfirm(
                        title = context.getString(R.string.apm_install_confirm_title),
                        content = context.getString(R.string.apm_install_confirm_content, fileName),
                        markdown = false
                    )
                } else {
                    navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                    viewModel.markNeedRefresh()
                }
            }

            FloatingActionButton(
                contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                onClick = {
                    // select the zip file to install
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "application/zip"
                    selectZipLauncher.launch(intent)
                }) {
                Icon(
                    painter = painterResource(id = R.drawable.package_import),
                    contentDescription = null
                )
            }
        }
    }, snackbarHost = { SnackbarHost(snackBarHost) }) { innerPadding ->
        when {
            hasMagisk -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.apm_magisk_conflict),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                ModuleList(
                    navigator,
                    viewModel = viewModel,
                    modules = filteredModuleList,
                    showMoreModuleInfo = showMoreModuleInfo,
                    useBanner = useBanner,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    state = moduleListState,
                    onInstallModule = {
                        navigator.navigate(InstallScreenDestination(it, MODULE_TYPE.APM))
                    },
                    onClickModule = { id, name, hasWebUi ->
                        if (hasWebUi) {
                            webUILauncher.launch(
                                Intent(
                                    context, WebUIActivity::class.java
                                ).setData("apatch://webui/$id".toUri()).putExtra("id", id)
                                    .putExtra("name", name)
                            )
                        }
                    },
                    snackBarHost = snackBarHost,
                    context = context
                )
            }
        }
    }

    // First Use Dialog
    if (showFirstTimeDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                if (dontShowAgain) {
                    prefs.edit().putBoolean("apm_first_use_shown", true).apply()
                }
                showFirstTimeDialog = false
            },
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(350.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.apm_first_use_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.apm_first_use_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Text(
                            text = stringResource(R.string.kpm_autoload_do_not_show_again),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            if (dontShowAgain) {
                                prefs.edit().putBoolean("apm_first_use_shown", true).apply()
                            }
                            showFirstTimeDialog = false
                        }) {
                            Text(stringResource(R.string.kpm_autoload_first_time_confirm))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleList(
    navigator: DestinationsNavigator,
    viewModel: APModuleViewModel,
    modules: List<APModuleViewModel.ModuleInfo>,
    showMoreModuleInfo: Boolean,
    useBanner: Boolean,
    modifier: Modifier = Modifier,
    state: LazyListState,
    onInstallModule: (Uri) -> Unit,
    onClickModule: (id: String, name: String, hasWebUi: Boolean) -> Unit,
    snackBarHost: SnackbarHostState,
    context: Context
) {
    val failedEnable = stringResource(R.string.apm_failed_to_enable)
    val failedDisable = stringResource(R.string.apm_failed_to_disable)
    val failedUninstall = stringResource(R.string.apm_uninstall_failed)
    val successUninstall = stringResource(R.string.apm_uninstall_success)
    val reboot = stringResource(id = R.string.reboot)
    val rebootToApply = stringResource(id = R.string.apm_reboot_to_apply)
    val moduleStr = stringResource(id = R.string.apm)
    val uninstall = stringResource(id = R.string.apm_remove)
    val cancel = stringResource(id = android.R.string.cancel)
    val moduleUninstallConfirm = stringResource(id = R.string.apm_uninstall_confirm)
    val updateText = stringResource(R.string.apm_update)
    val changelogText = stringResource(R.string.apm_changelog)
    val downloadingText = stringResource(R.string.apm_downloading)
    val startDownloadingText = stringResource(R.string.apm_start_downloading)

    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog()

    suspend fun onModuleUpdate(
        module: APModuleViewModel.ModuleInfo,
        changelogUrl: String,
        downloadUrl: String,
        fileName: String
    ) {
        val changelog = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                if (Patterns.WEB_URL.matcher(changelogUrl).matches()) {
                    apApp.okhttpClient.newCall(
                        okhttp3.Request.Builder().url(changelogUrl).build()
                    ).execute().body!!.string()
                } else {
                    changelogUrl
                }
            }
        }


        if (changelog.isNotEmpty()) {
            // changelog is not empty, show it and wait for confirm
            val confirmResult = confirmDialog.awaitConfirm(
                changelogText,
                content = changelog,
                markdown = true,
                confirm = updateText,
            )

            if (confirmResult != ConfirmResult.Confirmed) {
                return
            }
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context, startDownloadingText.format(module.name), Toast.LENGTH_SHORT
            ).show()
        }

        val downloading = downloadingText.format(module.name)
        withContext(Dispatchers.IO) {
            download(
                context,
                downloadUrl,
                fileName,
                downloading,
                onDownloaded = onInstallModule,
                onDownloading = {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, downloading, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    suspend fun onModuleUninstall(module: APModuleViewModel.ModuleInfo) {
        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = moduleUninstallConfirm.format(module.name),
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                uninstallModule(module.id)
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }
        val message = if (success) {
            successUninstall.format(module.name)
        } else {
            failedUninstall.format(module.name)
        }
        val actionLabel = if (success) {
            reboot
        } else {
            null
        }
        val result = snackBarHost.showSnackbar(
            message = message, actionLabel = actionLabel, duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            reboot()
        }
    }

    PullToRefreshBox(
        modifier = modifier,
        onRefresh = { viewModel.fetchModuleList() },
        isRefreshing = viewModel.isRefreshing
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = remember {
                PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + 16.dp + 56.dp /*  Scaffold Fab Spacing + Fab container height */
                )
            },
        ) {
            when {
                modules.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.apm_empty), textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    items(modules) { module ->
                        var isChecked by rememberSaveable(module) { mutableStateOf(module.enabled) }
                        val scope = rememberCoroutineScope()
                        val updatedModule by produceState(initialValue = Triple("", "", "")) {
                            scope.launch(Dispatchers.IO) {
                                value = viewModel.checkUpdate(module)
                            }
                        }

                        ModuleItem(
                            navigator,
                            module,
                            isChecked,
                            updatedModule.first,
                            showMoreModuleInfo = showMoreModuleInfo,
                            useBanner = useBanner,
                            onUninstall = {
                                scope.launch { onModuleUninstall(module) }
                            },
                            onCheckChanged = {
                                scope.launch {
                                    val success = loadingDialog.withLoading {
                                        withContext(Dispatchers.IO) {
                                            toggleModule(module.id, !isChecked)
                                        }
                                    }
                                    if (success) {
                                        isChecked = it
                                        viewModel.fetchModuleList()

                                        val result = snackBarHost.showSnackbar(
                                            message = rebootToApply,
                                            actionLabel = reboot,
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            reboot()
                                        }
                                    } else {
                                        val message = if (isChecked) failedDisable else failedEnable
                                        snackBarHost.showSnackbar(message.format(module.name))
                                    }
                                }
                            },
                            onUpdate = {
                                scope.launch {
                                    onModuleUpdate(
                                        module,
                                        updatedModule.third,
                                        updatedModule.first,
                                        "${module.name}-${updatedModule.second}.zip"
                                    )
                                }
                            },
                            onClick = {
                                onClickModule(it.id, it.name, it.hasWebUi)
                            })
                        // fix last item shadow incomplete in LazyColumn
                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }

        DownloadListener(context, onInstallModule)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    navigator: DestinationsNavigator,
    viewModel: APModuleViewModel,
    snackBarHost: SnackbarHostState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val confirmDialog = rememberConfirmDialog()
    val scope = rememberCoroutineScope()
    val disableAllTitle = stringResource(R.string.apm_disable_all_title)
    val disableAllConfirm = stringResource(R.string.apm_disable_all_confirm)
    val confirm = stringResource(android.R.string.ok)
    val cancel = stringResource(android.R.string.cancel)
    val context = LocalContext.current

    var showDisableAllButton by remember {
        mutableStateOf(APApplication.sharedPreferences.getBoolean("show_disable_all_modules", false))
    }
    var showMenu by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gzip")) { uri ->
        uri?.let {
            scope.launch {
                ModuleBackupUtils.backupModules(context, snackBarHost, it)
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                ModuleBackupUtils.restoreModules(context, snackBarHost, it)
                viewModel.fetchModuleList()
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "show_disable_all_modules") {
                showDisableAllButton = prefs.getBoolean("show_disable_all_modules", false)
            }
        }
        APApplication.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            APApplication.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    SearchAppBar(
        title = { Text(stringResource(R.string.apm)) },
        searchText = searchQuery,
        onSearchTextChange = onSearchQueryChange,
        onClearClick = { onSearchQueryChange("") },
        leadingActions = {
            if (showDisableAllButton) {
                IconButton(onClick = {
                    scope.launch {
                        val result = confirmDialog.awaitConfirm(
                            title = disableAllTitle,
                            content = disableAllConfirm,
                            confirm = confirm,
                            dismiss = cancel
                        )
                        if (result == ConfirmResult.Confirmed) {
                            viewModel.disableAllModules()
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = disableAllTitle
                    )
                }
            }
            IconButton(onClick = {
                navigator.navigate(OnlineModuleScreenDestination)
            }) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Online Modules"
                )
            }
            IconButton(onClick = {
                navigator.navigate(ApmBulkInstallScreenDestination)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Bulk Install"
                )
            }
        },
        dropdownContent = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                WallpaperAwareDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.apm_backup_title)) },
                        onClick = {
                            showMenu = false
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            backupLauncher.launch("FolkPatch_Modules_Backup_$timeStamp.tar.gz")
                        }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.apm_restore_title)) },
                        onClick = {
                            showMenu = false
                            restoreLauncher.launch(arrayOf("application/gzip", "application/x-gzip", "application/x-tar"))
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ModuleItem(
    navigator: DestinationsNavigator,
    module: APModuleViewModel.ModuleInfo,
    isChecked: Boolean,
    updateUrl: String,
    showMoreModuleInfo: Boolean,
    useBanner: Boolean,
    onUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (APModuleViewModel.ModuleInfo) -> Unit,
    onClick: (APModuleViewModel.ModuleInfo) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val decoration = if (!module.remove) TextDecoration.None else TextDecoration.LineThrough
    val moduleAuthor = stringResource(id = R.string.apm_author)
    val viewModel = viewModel<APModuleViewModel>()

    val sizeStr by produceState(initialValue = "0 KB", key1 = module.id) {
        value = withContext(Dispatchers.IO) {
            viewModel.getModuleSize(module.id)
        }
    }
    
    // 读取banner数据
    val bannerData by produceState<Any?>(initialValue = null, key1 = module.id, key2 = module.banner, key3 = useBanner) {
        if (!useBanner || module.banner.isEmpty()) {
            value = null
            return@produceState
        }
        
        value = withContext(Dispatchers.IO) {
            try {
                // 检查banner是否是URL
                if (Patterns.WEB_URL.matcher(module.banner).matches()) {
                    // 如果是URL，直接返回URL字符串
                    module.banner
                } else {
                    // 如果是相对路径，从模块目录读取
                    // 检查APatch模块目录
                    val apPath = "/data/adb/ap/modules/${module.id}/${module.banner}"
                    // 检查Magisk模块目录（兼容性）
                    val magiskPath = "/data/adb/modules/${module.id}/${module.banner}"
                    
                    val paths = listOf(apPath, magiskPath)
                    
                    for (path in paths) {
                        val file = SuFile(path)
                        if (file.exists() && file.canRead()) {
                            // 返回文件的URI
                            return@withContext Uri.fromFile(file).toString()
                        }
                    }
                    
                    null // 如果没有找到文件，返回null
                }
            } catch (e: Exception) {
                Log.e("ModuleItem", "Failed to load banner for module ${module.id}: ${e.message}")
                null
            }
        }
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(module) },
            contentAlignment = Alignment.Center
        ) {
            // 如果有banner数据且启用banner显示
            if (useBanner && bannerData != null) {
                // 显示banner图片作为背景
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(bannerData)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 如果没有banner，显示纯色背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                )
            }
            
            // 整个内容区域覆盖半透明黑色背景
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(all = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .alpha(alpha = alpha)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            textDecoration = decoration,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White // 白色文字确保在深色背景上可读
                        )

                        Text(
                            text = "${module.version}, $moduleAuthor ${module.author}",
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = decoration,
                            color = Color.White.copy(alpha = 0.8f) // 半透明白色
                        )
                    }

                    Switch(
                        enabled = !module.update,
                        checked = isChecked,
                        onCheckedChange = onCheckChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Text(
                    modifier = Modifier
                        .alpha(alpha = alpha)
                        .padding(horizontal = 16.dp),
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = decoration,
                    color = Color.White.copy(alpha = 0.7f) // 半透明白色
                )

                if (showMoreModuleInfo) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        ) {
                            Text(
                                text = module.id,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        ) {
                            Text(
                                text = sizeStr,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (updateUrl.isNotEmpty()) {
                        // 修改更新按钮样式
                        FilledTonalButton(
                            onClick = { onUpdate(module) },
                            enabled = true,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.device_mobile_down),
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.apm_update),
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                softWrap = false,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    if (module.hasWebUi) {
                        FilledTonalButton(
                            onClick = { onClick(module) },
                            enabled = true,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.webui),
                                contentDescription = null,
                                tint = Color.White
                            )

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.apm_webui_open),
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                softWrap = false,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (module.hasActionScript) {
                        FilledTonalButton(
                            onClick = {
                                navigator.navigate(ExecuteAPMActionScreenDestination(module.id))
                                viewModel.markNeedRefresh()
                            }, 
                            enabled = true, 
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.settings),
                                contentDescription = null,
                                tint = Color.White
                            )

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.apm_action),
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                softWrap = false,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    // 修改删除按钮样式
                    FilledTonalButton(
                        onClick = { onUninstall(module) },
                        enabled = !module.remove,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Red.copy(alpha = 0.6f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Color.White
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.apm_remove),
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false,
                            color = Color.White
                        )
                    }
                }
            }

            if (module.remove) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Red.copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.trash),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }
            if (module.update) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Blue.copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.device_mobile_down),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}