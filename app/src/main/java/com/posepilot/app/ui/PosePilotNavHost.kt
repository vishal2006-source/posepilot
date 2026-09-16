package com.posepilot.app.ui

import android.app.Application
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.posepilot.app.data.AppContainer
import com.posepilot.app.ui.camera.CameraScreen
import com.posepilot.app.ui.camera.CoachViewModel
import com.posepilot.app.ui.gallery.GalleryScreen
import com.posepilot.app.ui.home.HomeScreen
import com.posepilot.app.ui.poses.PoseLibraryScreen
import com.posepilot.app.ui.reference.ReferenceScreen
import com.posepilot.app.ui.reference.ReferenceViewModel
import com.posepilot.app.ui.review.ReviewScreen
import com.posepilot.app.ui.review.ReviewViewModel
import com.posepilot.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val COACH = "coach?poseId={poseId}"
    const val LIBRARY = "library"
    const val REFERENCE = "reference"
    const val REVIEW = "review"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"

    fun coach(poseId: String? = null) = if (poseId == null) "coach" else "coach?poseId=$poseId"
}

@Composable
fun PosePilotNavHost(container: AppContainer, nav: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as Application

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onCoach = { nav.navigate(Routes.coach()) },
                onReference = { nav.navigate(Routes.REFERENCE) },
                onLibrary = { nav.navigate(Routes.LIBRARY) },
                onPhotos = { nav.navigate(Routes.GALLERY) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            Routes.COACH,
            arguments = listOf(navArgument("poseId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            val poseId = entry.arguments?.getString("poseId")
            val vm: CoachViewModel = viewModel { CoachViewModel(container, app, poseId) }
            CameraScreen(
                vm = vm,
                container = container,
                onBack = { nav.popBackStack() },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onReview = { nav.navigate(Routes.REVIEW) { launchSingleTop = true } },
            )
        }

        composable(Routes.LIBRARY) {
            PoseLibraryScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onSelect = { id -> nav.navigate(Routes.coach(id)) },
            )
        }

        composable(Routes.REFERENCE) {
            val vm: ReferenceViewModel = viewModel { ReferenceViewModel(container, app) }
            ReferenceScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onStartCoaching = { id -> nav.navigate(Routes.coach(id)) },
            )
        }

        composable(Routes.REVIEW) {
            val vm: ReviewViewModel = viewModel { ReviewViewModel(container) }
            ReviewScreen(
                vm = vm,
                onRetake = { nav.popBackStack() },
                onDone = { nav.popBackStack() },
            )
        }

        composable(Routes.GALLERY) {
            GalleryScreen(container = container, onBack = { nav.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(container = container, onBack = { nav.popBackStack() })
        }
    }
}
