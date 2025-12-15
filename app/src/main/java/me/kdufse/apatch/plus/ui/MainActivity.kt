package me.kdufse.apatch.plus.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.Coil
import coil.ImageLoader
import me.kdufse.apatch.plus.APApplication
import me.kdufse.apatch.plus.ui.theme.APatchThemeWithBackground
import androidx.compose.material3.MaterialTheme
import me.kdufse.apatch.plus.util.PermissionRequestHandler
import me.kdufse.apatch.plus.util.PermissionUtils
import me.kdufse.apatch.plus.util.ui.LocalSnackbarHost
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import me.kdufse.apatch.plus.util.UpdateChecker
import me.kdufse.apatch.plus.ui.component.UpdateDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import me.kdufse.apatch.plus.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

class MainActivity : AppCompatActivity() {

    private var isLoading = true
    private lateinit var permissionHandler: PermissionRequestHandler

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(me.kdufse.apatch.plus.util.DPIUtils.updateContext(newBase))
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen().setKeepOnScreenCondition { isLoading }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        // 初始化权限处理器
        permissionHandler = PermissionRequestHandler(this)

        val prefs = APApplication.sharedPreferences
        val biometricLogin = prefs.getBoolean("biometric_login", false)
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

        if (biometricLogin && canAuthenticate) {
            val biometricPrompt = androidx.biometric.BiometricPrompt(
                this,
                androidx.core.content.ContextCompat.getMainExecutor(this),
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        android.widget.Toast.makeText(this@MainActivity, errString, android.widget.Toast.LENGTH_SHORT).show()
                        finishAndRemoveTask()
                    }

                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        setupUI()
                    }
                })
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.action_biometric))
                .setSubtitle(getString(R.string.msg_biometric))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .build()
            biometricPrompt.authenticate(promptInfo)
        } else {
            setupUI()
        }
    }

    private fun setupUI() {
        
        // Load DPI settings
        me.kdufse.apatch.plus.util.DPIUtils.load(this)
        me.kdufse.apatch.plus.util.DPIUtils.applyDpi(this)
        
        // 检查并请求权限
        if (!PermissionUtils.hasExternalStoragePermission(this) || 
            !PermissionUtils.hasWriteExternalStoragePermission(this)) {
            permissionHandler.requestPermissions(
                onGranted = {
                    // 权限已授予
                },
                onDenied = {
                    // 权限被拒绝，可以显示一个提示
                }
            )
        }

        setContent {
            val navController = rememberNavController()
            val snackBarHostState = remember { SnackbarHostState() }
            // 底部导航栏的路由列表
            val bottomBarRoutes = remember {
                listOf("home", "apps", "patches", "settings").toSet()
            }

            APatchThemeWithBackground(navController = navController) {
                
                val showUpdateDialog = remember { mutableStateOf(false) }
                val context = LocalContext.current
                
                LaunchedEffect(Unit) {
                    val prefs = APApplication.sharedPreferences
                    if (prefs.getBoolean("auto_update_check", true)) {
                        withContext(Dispatchers.IO) {
                             // Delay a bit to wait for network connection
                             kotlinx.coroutines.delay(2000)
                             val hasUpdate = me.kdufse.apatch.plus.util.UpdateChecker.checkUpdate()
                             if (hasUpdate) {
                                 showUpdateDialog.value = true
                             }
                        }
                    }
                }

                if (showUpdateDialog.value) {
                    UpdateDialog(
                        onDismiss = { showUpdateDialog.value = false },
                        onUpdate = {
                            showUpdateDialog.value = false
                            UpdateChecker.openUpdateUrl(context)
                        }
                    )
                }

                Scaffold(
                    bottomBar = {
                        MyBottomBar(navController = navController)
                    }
                ) { paddingValues ->
                    CompositionLocalProvider(
                        LocalSnackbarHost provides snackBarHostState,
                    ) {
                        // 使用标准的 NavHost，不依赖 Compose Destinations
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(bottom = 80.dp)
                        ) {
                            // 这里添加你的屏幕组合函数
                            // 你需要创建这些屏幕并在这里引用它们
                            composable("home") {
                                // HomeScreen()
                                PlaceholderScreen("Home Screen")
                            }
                            composable("apps") {
                                // AppsScreen()
                                PlaceholderScreen("Apps Screen")
                            }
                            composable("patches") {
                                // PatchesScreen()
                                PlaceholderScreen("Patches Screen")
                            }
                            composable("settings") {
                                // SettingsScreen()
                                PlaceholderScreen("Settings Screen")
                            }
                        }
                    }
                }
            }
        }

        // Initialize Coil
        val iconSize = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory(iconSize, false, this@MainActivity))
                }
                .build()
        )

        isLoading = false
    }
}

// 底部导航目的地定义
sealed class BottomBarDestination(
    val route: String,
    val labelResId: Int,
    val iconResId: Int
) {
    object Home : BottomBarDestination("home", R.string.home, R.drawable.ic_home)
    object Apps : BottomBarDestination("apps", R.string.apps, R.drawable.ic_apps)
    object Patches : BottomBarDestination("patches", R.string.patches, R.drawable.ic_patches)
    object Settings : BottomBarDestination("settings", R.string.settings, R.drawable.ic_settings)

    companion object {
        val items = listOf(Home, Apps, Patches, Settings)
    }
}

@Composable
fun MyBottomBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        // 遍历所有底部栏目的地
        BottomBarDestination.items.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.route) {
                            // 清除返回栈，避免重复堆叠相同的页面
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = androidx.compose.ui.res.vectorResource(destination.iconResId),
                        contentDescription = stringResource(destination.labelResId)
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelResId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

// 占位符屏幕，用于测试
@Composable
fun PlaceholderScreen(screenName: String) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        androidx.compose.material3.Text(
            text = screenName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}