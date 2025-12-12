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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.Coil
import coil.ImageLoader
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import kotlinx.coroutines.launch
import me.kdufse.apatch.plus.APApplication
import me.kdufse.apatch.plus.ui.screen.BottomBarDestination
import me.kdufse.apatch.plus.ui.theme.APatchThemeWithBackground
import me.kdufse.apatch.plus.util.PermissionRequestHandler
import me.kdufse.apatch.plus.util.PermissionUtils
import me.kdufse.apatch.plus.util.ui.LocalSnackbarHost
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer

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
                        // 检查是否是用户取消
                        if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                errString,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        // 即使用户取消或认证失败，也继续显示UI
                        setupUI()
                    }

                    override fun onAuthenticationSucceeded(
                        result: androidx.biometric.BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        setupUI()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // 认证失败，但仍继续显示UI
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
        
        // 初始化Coil
        initializeCoil()

        setContent {
            val navController = rememberNavController()
            val snackBarHostState = remember { SnackbarHostState() }
            val bottomBarRoutes = remember {
                BottomBarDestination.entries.map { it.direction.route }.toSet()
            }

            APatchThemeWithBackground(navController = navController) {

                // 检查权限
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    if (!PermissionUtils.hasRequiredPermissions(context)) {
                        permissionHandler.requestPermissions(
                            onGranted = {
                                // 权限已授予
                            },
                            onDenied = {
                                // 权限被拒绝，显示snackbar提示
                                snackBarHostState.showSnackbar(
                                    "需要存储权限以正常使用应用功能",
                                    withDismissAction = true
                                )
                            }
                        )
                    }
                }

                Scaffold(
                    bottomBar = { BottomBar(navController) }
                ) { _ ->
                    CompositionLocalProvider(
                        LocalSnackbarHost provides snackBarHostState,
                    ) {
                        DestinationsNavHost(
                            modifier = Modifier.padding(bottom = 80.dp),
                            navGraph = NavGraphs.root,
                            navController = navController,
                            engine = rememberNavHostEngine(navHostContentAlignment = Alignment.TopCenter),
                            defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                                override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                                    {
                                        // If the target is a detail page (not a bottom navigation page), slide in from the right
                                        if (targetState.destination.route !in bottomBarRoutes) {
                                            slideInHorizontally(initialOffsetX = { it })
                                        } else {
                                            // Otherwise (switching between bottom navigation pages), use fade in
                                            fadeIn(animationSpec = tween(340))
                                        }
                                    }

                                override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                                    {
                                        // If navigating from the home page (bottom navigation page) to a detail page, slide out to the left
                                        if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                                            slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
                                        } else {
                                            // Otherwise (switching between bottom navigation pages), use fade out
                                            fadeOut(animationSpec = tween(340))
                                        }
                                    }

                                override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                                    {
                                        // If returning to the home page (bottom navigation page), slide in from the left
                                        if (targetState.destination.route in bottomBarRoutes) {
                                            slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                                        } else {
                                            // Otherwise (e.g., returning between multiple detail pages), use default fade in
                                            fadeIn(animationSpec = tween(340))
                                        }
                                    }

                                override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                                    {
                                        // If returning from a detail page (not a bottom navigation page), scale down and fade out
                                        if (initialState.destination.route !in bottomBarRoutes) {
                                            scaleOut(targetScale = 0.9f) + fadeOut()
                                        } else {
                                            // Otherwise, use default fade out
                                            fadeOut(animationSpec = tween(340))
                                        }
                                    }
                            }
                        )
                    }
                }
            }
        }

        isLoading = false
    }

    private fun initializeCoil() {
        try {
            val iconSize = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
            Coil.setImageLoader(
                ImageLoader.Builder(this)
                    .components {
                        add(AppIconKeyer())
                        add(AppIconFetcher.Factory(iconSize, false, this@MainActivity))
                    }
                    .build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    // 使用Compose Destinations的API来获取当前路由状态
    val currentDestination: com.ramcosta.composedestinations.spec.Destination? =
        rememberDestinationsNavigator().let { navigator ->
            navigator.appCurrentDestinationAsState().value
        }
    
    // 或者使用原始的NavController API
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        BottomBarDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.direction.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.direction.route) {
                        // 避免重复添加相同的destination
                        launchSingleTop = true
                        // 恢复保存的状态
                        restoreState = true
                        // 如果已经存在，弹出到起始destination
                        navController.graph.findStartDestination()?.let { startDestination ->
                            popUpTo(startDestination.id) {
                                saveState = true
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.label)
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

// 如果BottomBarDestination类不存在，请创建它或者检查现有的定义
// 这里假设BottomBarDestination的定义如下：

/*
enum class BottomBarDestination(
    val direction: Direction,
    val icon: ImageVector,
    @StringRes val label: Int
) {
    Home(
        direction = HomeScreenDestination,
        icon = Icons.Default.Home,
        label = R.string.home
    ),
    Status(
        direction = StatusScreenDestination,
        icon = Icons.Default.Info,
        label = R.string.status
    ),
    // 其他项目...
}
*/

// 如果需要UpdateChecker类，创建一个简单的版本：

/*
object UpdateChecker {
    fun checkUpdate(): Boolean {
        // 检查更新的逻辑
        return false
    }
    
    fun openUpdateUrl(context: android.content.Context) {
        // 打开更新URL的逻辑
        val url = "https://your-update-url.com"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    }
}
*/

// 如果UpdateDialog不存在，创建一个简单的版本：

/*
@Composable
fun UpdateDialog(
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "发现新版本") },
        text = { Text(text = "有新版本可用，是否立即更新？") },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text("更新")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}
*/

// 在PermissionUtils中添加hasRequiredPermissions方法：

/*
fun hasRequiredPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        hasExternalStoragePermission(context) && hasWriteExternalStoragePermission(context)
    }
}
*/