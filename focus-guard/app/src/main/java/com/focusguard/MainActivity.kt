package com.focusguard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focusguard.ml.AttentionSignal
import com.focusguard.ml.MLPipeline
import com.focusguard.session.SessionManager
import com.focusguard.session.SessionPhase
import com.focusguard.state.EarnedItStore
import com.focusguard.ui.BounceScreen
import com.focusguard.ui.EarnedBottomNav
import com.focusguard.ui.HomeScreen
import com.focusguard.ui.InsightsScreen
import com.focusguard.ui.MoreFeatureScreen
import com.focusguard.ui.MorePetDetailScreen
import com.focusguard.ui.MoreScreen
import com.focusguard.ui.OnboardingScreen
import com.focusguard.ui.ResultsScreen
import com.focusguard.ui.SetupScreen
import com.focusguard.ui.SessionScreen
import com.focusguard.ui.SocialScreen
import com.focusguard.ui.theme.FocusGuardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var mlPipeline: MLPipeline
    private val signalBuffer = ArrayDeque<AttentionSignal>()
    @Volatile private var frameCounter = 0
    @Volatile private var baselineYaw: Float? = null
    @Volatile private var baselinePitch: Float? = null
    private var calibrationFrames = mutableListOf<AttentionSignal>()
    @Volatile private var isCalibrating = false
    @Volatile private var lastPhase: SessionPhase? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera()
        else Log.e("FocusGuard_Camera", "Camera permission denied")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.initialize(applicationContext)
        EarnedItStore.initialize(applicationContext)

        mlPipeline = MLPipeline(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                SessionManager.stateFlow.collect { state ->
                    if (state.phase == SessionPhase.FocusActive && lastPhase != SessionPhase.FocusActive) {
                        signalBuffer.clear()
                        calibrationFrames.clear()
                        baselineYaw = null
                        baselinePitch = null
                        isCalibrating = true
                        SessionManager.setCalibrating(true)
                        frameCounter = 0
                    }
                    lastPhase = state.phase
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            FocusGuardTheme {
                val navController = rememberNavController()
                val state by SessionManager.stateFlow.collectAsState()
                val uiState by EarnedItStore.state.collectAsState()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route
                val tabRoutes = setOf("home", "insights", "social", "more")

                // Navigate to bounce screen when a blocked app is detected
                LaunchedEffect(state.blockedPackageName) {
                    if (state.blockedPackageName != null) {
                        navController.navigate("bounce") {
                            launchSingleTop = true
                        }
                    }
                }

                if (!uiState.loaded) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            if (currentRoute in tabRoutes) {
                                EarnedBottomNav(currentRoute = currentRoute) { route ->
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                    ) { padding ->
                        NavHost(
                            navController = navController,
                            startDestination = if (uiState.onboardingComplete) "home" else "onboarding",
                            modifier = Modifier.padding(padding)
                        ) {
                            composable("onboarding") {
                                OnboardingScreen(
                                    onComplete = {
                                        navController.navigate("home") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("home") {
                                HomeScreen(
                                    onStartSession = { navController.navigate("setup") },
                                    onReplayOnboarding = {
                                        EarnedItStore.replayOnboardingForPreview()
                                        navController.navigate("onboarding") {
                                            popUpTo("home") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable("insights") {
                                InsightsScreen(onOpenDetail = { navController.navigate("feature/$it") })
                            }
                            composable("social") {
                                SocialScreen()
                            }
                            composable("more") {
                                MoreScreen(onOpen = { label ->
                                    if (label == "Focus Pet") navController.navigate("pet")
                                    else navController.navigate("feature/$label")
                                })
                            }
                            composable("pet") {
                                MorePetDetailScreen(onBack = { navController.popBackStack() })
                            }
                            composable("feature/{title}") { entry ->
                                MoreFeatureScreen(
                                    title = entry.arguments?.getString("title") ?: "EarnedIt",
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("setup") {
                                SetupScreen(
                                    onBack = { navController.popBackStack() },
                                    onSessionStarted = {
                                        navController.navigate("session") {
                                            popUpTo("home")
                                        }
                                    }
                                )
                            }
                            composable("session") {
                                SessionScreen(
                                    onSessionEnd = { endedEarly ->
                                        val sessionState = SessionManager.stateFlow.value
                                        EarnedItStore.recordSession(
                                            durationSeconds = sessionState.initialDurationSeconds,
                                            focusScore = sessionState.attentionScore * 100f,
                                            distractionCount = sessionState.distractionCount,
                                            endedEarly = endedEarly
                                        )
                                        navController.navigate("results") {
                                            popUpTo("home")
                                        }
                                    }
                                )
                            }
                            composable("results") {
                                ResultsScreen(
                                    onDone = {
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("bounce") {
                                BounceScreen(
                                    onDismiss = {
                                        navController.popBackStack("session", inclusive = false)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("FocusGuard_Camera", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        if (!SessionManager.isBlockingEnabled()) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val signal = mlPipeline.processFrame(rotatedBitmap)
                handleSignal(signal)
            } catch (e: Exception) {
                Log.e("FocusGuard_Camera", "Frame processing failed", e)
            } finally {
                if (rotatedBitmap != bitmap) rotatedBitmap.recycle()
                bitmap.recycle()
                imageProxy.close()
            }
        }
    }

    private fun handleSignal(signal: AttentionSignal) {
        synchronized(this) {
            if (isCalibrating) {
                if (signal.faceDetected) {
                    calibrationFrames.add(signal)
                    Log.d("FocusGuard_Camera", "Calibrating baseline... ${calibrationFrames.size}/45")
                    if (calibrationFrames.size >= 45) {
                        baselineYaw = calibrationFrames.map { it.yaw }.average().toFloat()
                        baselinePitch = calibrationFrames.map { it.pitch }.average().toFloat()
                        isCalibrating = false
                        SessionManager.setCalibrating(false)
                        Log.d("FocusGuard_Camera", "Calibration complete. Baseline Yaw: $baselineYaw, Pitch: $baselinePitch")
                    }
                }
            } else {
                signalBuffer.addLast(signal)
                if (signalBuffer.size > 45) signalBuffer.removeFirst()

                frameCounter++
                if (frameCounter >= 15) {
                    val averagedSignal = AttentionSignal(
                        faceDetected = signalBuffer.count { it.faceDetected } > signalBuffer.size / 2,
                        yaw = signalBuffer.map { it.yaw }.average().toFloat() - (baselineYaw ?: 0f),
                        pitch = signalBuffer.map { it.pitch }.average().toFloat() - (baselinePitch ?: 0f),
                        roll = 0f,
                        eyeAspectRatio = signalBuffer.map { it.eyeAspectRatio }.average().toFloat()
                    )
                    SessionManager.onAttentionSignal(averagedSignal, frameDeltaSeconds = 1f)
                    Log.d("FocusGuard_Camera", "Averaged signal sent: $averagedSignal")
                    frameCounter = 0
                }
            }
        }
    }
}
