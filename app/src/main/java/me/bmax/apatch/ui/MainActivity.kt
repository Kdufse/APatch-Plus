package me.bmax.apatch.ui

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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.Coil
import coil.ImageLoader
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.screen.BottomBarDestination
import me.bmax.apatch.ui.theme.APatchThemeWithBackground
import me.bmax.apatch.util.PermissionRequestHandler
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer

class MainActivity : AppCompatActivity() {

    private var isLoading = true
    private lateinit var permissionHandler: PermissionRequestHandler

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(me.bmax.apatch.util.DPIUtils.updateContext(newBase))
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

        // 先初始化Coil，确保图片加载正常
        initializeCoil()

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
                            errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
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

    private fun setupUI() {
        // 加载DPI设置，但在主线程小心应用
        try {
            me.bmax.apatch.util.DPIUtils.load(this)
            runOnUiThread {
                me.bmax.apatch.util.DPIUtils.applyDpi(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val navController = rememberNavController()
            val snackBarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current

            // 检查权限
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

            APatchThemeWithBackground(navController = navController) {
                Scaffold(
                    bottomBar = { BottomBar(navController) },
                    snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
                ) { paddingValues ->
                    CompositionLocalProvider(
                        LocalSnackbarHost provides snackBarHostState,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            DestinationsNavHost(
                                navGraph = NavGraphs.root,
                                navController = navController,
                                engine = rememberNavHostEngine(),
                                defaultTransitions = createNavTransitions()
                            )
                        }
                    }
                }
            }
        }

        isLoading = false
    }

    @Composable
    private fun BottomBar(navController: NavHostController) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val bottomBarRoutes = BottomBarDestination.entries.map { it.direction.route }

        NavigationBar {
            BottomBarDestination.entries.forEach { destination ->
                val selected = currentDestination?.route == destination.direction.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(destination.direction.route) {
                            // 避免重复添加相同的destination
                            launchSingleTop = true
                            // 恢复保存的状态
                            restoreState = true
                            // 如果已经存在，弹出到起始destination
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelResId)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(destination.labelResId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }

    private fun createNavTransitions(): NavHostAnimatedDestinationStyle {
        val bottomBarRoutes = BottomBarDestination.entries.map { it.direction.route }.toSet()

        return object : NavHostAnimatedDestinationStyle() {
            override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                {
                    if (targetState.destination.route !in bottomBarRoutes) {
                        slideInHorizontally(initialOffsetX = { it })
                    } else {
                        fadeIn(animationSpec = tween(340))
                    }
                }

            override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                {
                    if (initialState.destination.route in bottomBarRoutes &&
                        targetState.destination.route !in bottomBarRoutes
                    ) {
                        slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
                    } else {
                        fadeOut(animationSpec = tween(340))
                    }
                }

            override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                {
                    if (targetState.destination.route in bottomBarRoutes) {
                        slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                    } else {
                        fadeIn(animationSpec = tween(340))
                    }
                }

            override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                {
                    if (initialState.destination.route !in bottomBarRoutes) {
                        scaleOut(targetScale = 0.9f) + fadeOut()
                    } else {
                        fadeOut(animationSpec = tween(340))
                    }
                }
        }
    }
}