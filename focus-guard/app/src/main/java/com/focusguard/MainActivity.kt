package com.focusguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focusguard.session.SessionManager
import com.focusguard.state.EarnedItStore
import com.focusguard.ui.BounceScreen
import com.focusguard.ui.CoachScreen
import com.focusguard.ui.EarnedBottomNav
import com.focusguard.ui.HomeScreen
import com.focusguard.ui.InsightsScreen
import com.focusguard.ui.MoreFeatureScreen
import com.focusguard.ui.MorePetDetailScreen
import com.focusguard.ui.MoreScreen
import com.focusguard.ui.OnboardingScreen
import com.focusguard.ui.ResultsScreen
import com.focusguard.ui.SessionScreen
import com.focusguard.ui.SocialScreen
import com.focusguard.ui.SetupScreen
import com.focusguard.ui.theme.FocusGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.initialize(applicationContext)
        EarnedItStore.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            FocusGuardTheme {
                val navController = rememberNavController()
                val state by SessionManager.stateFlow.collectAsState()
                val uiState by EarnedItStore.state.collectAsState()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route
                val tabRoutes = setOf("home", "insights", "social", "coach", "more")

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
                        composable("coach") {
                            CoachScreen()
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
}
