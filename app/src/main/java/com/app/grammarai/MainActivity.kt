package com.app.grammarai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.grammarai.ui.feed.FeedScreen
import com.app.grammarai.ui.feed.FeedViewModel
import com.app.grammarai.ui.lesson.LessonScreen
import com.app.grammarai.ui.lesson.LessonViewModel
import com.app.grammarai.ui.navigation.Route
import com.app.grammarai.ui.onboarding.OnboardingScreen
import com.app.grammarai.ui.onboarding.OnboardingViewModel
import com.app.grammarai.ui.practice.ExerciseRunnerScreen
import com.app.grammarai.ui.practice.ExerciseRunnerViewModel
import com.app.grammarai.ui.radar.SkillRadarScreen
import com.app.grammarai.ui.radar.SkillRadarViewModel
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.GrammarAITheme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.net.Uri
import android.content.Context
import com.app.grammarai.core.di.AppViewModelProvider
import com.app.grammarai.core.util.LocaleHelper
import com.app.grammarai.ui.settings.SettingsViewModel
import com.app.grammarai.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = runCatching { com.app.grammarai.data.local.PreferencesManager(newBase).appLanguage }.getOrDefault("es")
        val localizedContext = LocaleHelper.createLocalizedContext(newBase, lang)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lang = runCatching { com.app.grammarai.data.local.PreferencesManager(this).appLanguage }.getOrDefault("es")
        LocaleHelper.applyLanguage(this, lang)

        enableEdgeToEdge()
        setContent {
            GrammarAITheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        GrammarAiNavHost()
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val lang = runCatching { com.app.grammarai.data.local.PreferencesManager(this).appLanguage }.getOrDefault("es")
        LocaleHelper.applyLanguage(this, lang)
    }
}

@Composable
fun GrammarAiNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val prefs = runCatching { GrammarApplication.instance.preferencesManager }.getOrNull()
    val startDestination = if (prefs?.isOnboardingCompleted == true) {
        Route.Feed.path
    } else {
        Route.Onboarding.path
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(260))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(260))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(260))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(260))
        }
    ) {
        composable(Route.Onboarding.path) {
            val vm: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory)
            OnboardingScreen(
                viewModel = vm,
                onNavigateToFeed = {
                    navController.navigate(Route.Feed.path) {
                        popUpTo(Route.Onboarding.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Feed.path) {
            val vm: FeedViewModel = viewModel(factory = AppViewModelProvider.Factory)
            FeedScreen(
                viewModel = vm,
                onNavigateToLesson = { lessonId ->
                    val repo = GrammarApplication.instance.curriculumRepository
                    val lesson = repo.getLesson(lessonId)
                    if (lesson != null && lesson.youtubeId.isNullOrBlank()) {
                        navController.navigate(Route.Practice.createRoute(lessonId))
                    } else {
                        navController.navigate(Route.Lesson.createRoute(lessonId))
                    }
                },
                onNavigateToPractice = { lessonId ->
                    navController.navigate(Route.Practice.createRoute(lessonId))
                },
                onNavigateToRadar = {
                    navController.navigate(Route.SkillRadar.path)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.path)
                }
            )
        }

        composable(
            route = Route.Lesson.path,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawLessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val lessonId = android.net.Uri.decode(rawLessonId)
            val vm: LessonViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LessonScreen(
                lessonId = lessonId,
                viewModel = vm,
                onNavigateToPractice = { id ->
                    navController.navigate(Route.Practice.createRoute(id)) {
                        popUpTo(Route.Lesson.path) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Route.Practice.path,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawLessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val lessonId = android.net.Uri.decode(rawLessonId)
            val vm: ExerciseRunnerViewModel = viewModel(factory = AppViewModelProvider.Factory)
            ExerciseRunnerScreen(
                lessonId = lessonId,
                viewModel = vm,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToFeed = {
                    navController.navigate(Route.Feed.path) {
                        popUpTo(Route.Feed.path) { inclusive = true }
                    }
                },
                onNavigateToRadar = {
                    navController.navigate(Route.SkillRadar.path) {
                        popUpTo(Route.Feed.path)
                    }
                }
            )
        }

        composable(Route.SkillRadar.path) {
            val vm: SkillRadarViewModel = viewModel(factory = AppViewModelProvider.Factory)
            SkillRadarScreen(
                viewModel = vm,
                onNavigateToFeed = {
                    navController.navigate(Route.Feed.path) {
                        popUpTo(Route.Feed.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Settings.path) {
            val vm: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
            SettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
