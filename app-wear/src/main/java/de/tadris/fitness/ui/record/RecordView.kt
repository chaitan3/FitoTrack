/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.ui.record

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import de.tadris.fitness.R
import de.tadris.fitness.data.SettingsStore.WorkoutSettings
import de.tadris.fitness.ui.ConfirmDialog
import de.tadris.fitness.ui.record.RecordActivity.TargetAchieved
import de.tadris.fitness.util.logD

enum class SwipeDirection {LEFT, RIGHT, UP, DOWN}
@Composable
fun RecordView(recordActivity: RecordActivity) {
	val navController = rememberSwipeDismissableNavController()
	val showCooldownDialog = remember { mutableStateOf(false) }
	fun navFirstScreen(){
		if(recordActivity.workoutSettings.screens[1].enabled.value){
			navController.navigate("ScreenDefault")
		}else{
			recordActivity.workoutSettings.screens.firstOrNull{ it.isAvailable(recordActivity) }?.let{
				navController.navigate(it.navRoute)
			}
		}
	}

	var swipeDirection: SwipeDirection = SwipeDirection.LEFT
	fun onSwipe(swipeDirection: SwipeDirection){
		val route = navController.currentBackStackEntry?.destination?.route
		when(swipeDirection){
			SwipeDirection.LEFT -> if(route == "ScreenStop") navFirstScreen()
			SwipeDirection.RIGHT -> {
				when(route){
					"ScreenDefault", "ScreenTarget", "ScreenElevation", "ScreenHeartRate",
					"ScreenSpeed", "ScreenMap" ->
						navController.popBackStack("ScreenStop", inclusive = false)
					"ScreenStop" -> {}//don't do anything
					"Prepare" -> recordActivity.finish()
					"Cooldown" -> recordActivity.showSummary()
					"TargetEntryPrimary" -> {
						recordActivity.workoutData.primaryTargetType = null
						navController.popBackStack()
					}
					"TargetEntrySecondary" -> {
						recordActivity.workoutData.secondaryTargetType = null
						navController.popBackStack()
					}
					"Settings" -> {
						recordActivity.setScreenOnFlag()
						navController.popBackStack()
					}
					else -> navController.popBackStack()
				}
			}
			SwipeDirection.DOWN -> {
				var routeFound = false
				for(screenSettings in recordActivity.workoutSettings.screens){
					if(routeFound && screenSettings.isAvailable(recordActivity)){
						navController.navigate(screenSettings.navRoute)
						return
					}
					if(screenSettings.navRoute == route) routeFound = true
				}
			}
			SwipeDirection.UP -> {
				var routeFound = false
				for(screenSettings in recordActivity.workoutSettings.screens.asReversed()){
					if(routeFound && screenSettings.isAvailable(recordActivity)){
						navController.navigate(screenSettings.navRoute)
						return
					}
					if(screenSettings.navRoute == route) routeFound = true
				}
			}
		}
	}

	AppScaffold {
		val modifierWithSwipe = Modifier
			.pointerInput(Unit) {
				detectHorizontalDragGestures(
					onDragEnd = { onSwipe(swipeDirection) },
					onHorizontalDrag = { change, amount ->
						change.consume()
						swipeDirection =
							if(amount > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
					}
				)
			}
			.pointerInput(Unit) {
				detectVerticalDragGestures(
					onDragEnd = { onSwipe(swipeDirection) },
					onVerticalDrag = { change, amount ->
						change.consume()
						swipeDirection =
							if(amount > 0) SwipeDirection.DOWN else SwipeDirection.UP
					}
				)
			}
		//Because ScreenMap is really heavy, this cannot be remembered there
		val mapZoomLevel = remember { mutableStateOf<Byte>(15) }
		val mapCenter = remember { mutableStateOf(MapCenter.POSITION) }
		SwipeDismissableNavHost(
			userSwipeEnabled = false,
			navController = navController,
			startDestination = "Prepare"
		) {
			composable("Prepare") {
				Prepare(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					onWorkoutTypeClick = { navController.navigate("TypeSelect") },
					onTargetClick = {
						if(recordActivity.workoutData.primaryTargetType == null) {
							navController.navigate("Target")//add Target to the backstack
							navController.navigate("TargetTypeSelectPrimary")
						} else navController.navigate("Target")
					},
					onStartClick = {
						recordActivity.startExercise()
						navController.navigate("ScreenStop")
						navFirstScreen()
					},
					locationStatus = recordActivity.locationStatus,
					heartRateStatus = recordActivity.heartRateStatus,
					onSettingsClick = { navController.navigate("Settings") }
				)
			}
			composable("TypeSelect") {
				val context = LocalContext.current
				TypeSelect (modifier = modifierWithSwipe){
					recordActivity.workoutData.workoutType = it
					recordActivity.workoutSettings = WorkoutSettings(context, it)
					navController.popBackStack()
				}
			}
			composable("Target") {
				Target(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					onPrimaryClicked = { navController.navigate("TargetTypeSelectPrimary") },
					onSecondaryClicked = { navController.navigate("TargetTypeSelectSecondary") }
				)
			}
			composable("TargetTypeSelectPrimary") {
				TargetTypeSelect(
					modifier = modifierWithSwipe,
					targetType = recordActivity.workoutData.primaryTargetType,
					disabledTargetType = null,
					onTargetSelected = { targetType ->
						recordActivity.workoutData.primaryTargetType = targetType
						recordActivity.targetProgress.floatValue = 0.0F
						navController.popBackStack()
						if(targetType != null) navController.navigate("TargetEntryPrimary")
					}
				)
			}
			composable("TargetTypeSelectSecondary") {
				TargetTypeSelect(
					modifier = modifierWithSwipe,
					targetType = recordActivity.workoutData.secondaryTargetType,
					disabledTargetType = recordActivity.workoutData.primaryTargetType,
					onTargetSelected = { targetType ->
						recordActivity.workoutData.secondaryTargetType = targetType
						navController.popBackStack()
						navController.navigate("TargetEntrySecondary")
					}
				)
			}
			composable("TargetEntryPrimary") {
				TargetEntry(
					modifier = modifierWithSwipe,
					targetType = recordActivity.workoutData.primaryTargetType,
					onBack = {
						recordActivity.workoutData.primaryTargetType = null
						navController.popBackStack()
					},
					onDone = { value: Int, valueMax: Int ->
						if(value+valueMax == 0) {
							recordActivity.workoutData.primaryTargetType = null
						} else {
							recordActivity.workoutData.primaryTargetValue = value
							recordActivity.workoutData.primaryTargetValueHigh = valueMax
						}
						recordActivity.targetAchieved.value = TargetAchieved.NOT
						navController.popBackStack()
					}
				)
			}
			composable("TargetEntrySecondary") {
				TargetEntry(
					modifier = modifierWithSwipe,
					targetType = recordActivity.workoutData.secondaryTargetType,
					onBack = {
						recordActivity.workoutData.secondaryTargetType = null
						navController.popBackStack()
					},
					onDone = { value: Int, valueMax: Int ->
						if(value+valueMax == 0) {
							recordActivity.workoutData.secondaryTargetType = null
						} else {
							recordActivity.workoutData.secondaryTargetValue = value
							recordActivity.workoutData.secondaryTargetValueHigh = valueMax
						}
						navController.popBackStack()
					}
				)
			}
			composable("Settings") {
				Settings(
					modifier = modifierWithSwipe,
					showTarget = navController.previousBackStackEntry?.destination?.route != "Prepare",
					workoutData = recordActivity.workoutData,
					workoutSettings = recordActivity.workoutSettings,
					hasLocation = !recordActivity.locationStatus.isDisabled,
					hasHeartRate = !recordActivity.heartRateStatus.isDisabled,
					onTargetClicked = { navController.navigate("Target") }
				)
			}
			composable("ScreenStop") {
				ScreenStop(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					heartRate =
						if(recordActivity.heartRateStatus.isDisabled) null
						else recordActivity.heartRate,
					targetProgress = recordActivity.targetProgress,
					targetAchieved = recordActivity.targetAchieved,
					isPaused = recordActivity.exerciseState.isPaused,
					onPauseResumeClick = recordActivity::pauseResumeClicked,
					onEndClick = {
						recordActivity.endExercise()
						if(recordActivity.heartRateStatus.isDisabled)
							recordActivity.showSummary()
						else showCooldownDialog.value = true
					},
					onSettingsClick = { navController.navigate("Settings") }
				)
			}
			composable("ScreenMap") {
				val hasOtherScreens = recordActivity.workoutSettings.screens.count {
					it.isAvailable(recordActivity)
				} > 1
				ScreenMap(
					workoutData = recordActivity.workoutData,
					targetProgress = recordActivity.targetProgress,
					targetAchieved = recordActivity.targetAchieved,
					isPaused = recordActivity.exerciseState.isPaused,
					hasOtherScreens,
					zoomLevel = mapZoomLevel,
					mapCenter,
					goBack = {
						if(hasOtherScreens) onSwipe(SwipeDirection.DOWN)
						else onSwipe(SwipeDirection.RIGHT)
					}
				)
			}
			composable("ScreenDefault") {
				ScreenDefault(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					speed = recordActivity.lastSpeed,
					heartRate =
						if(recordActivity.heartRateStatus.isDisabled) null
						else recordActivity.heartRate,
					targetProgress = recordActivity.targetProgress,
					targetAchieved = recordActivity.targetAchieved,
					isPaused = recordActivity.exerciseState.isPaused
				)
			}
			composable("ScreenTarget") {
				ScreenTarget(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					speed = recordActivity.lastSpeed,
					heartRate =
						if(recordActivity.heartRateStatus.isDisabled) null
						else recordActivity.heartRate,
					targetProgress = recordActivity.targetProgress,
					targetAchieved = recordActivity.targetAchieved,
					isPaused = recordActivity.exerciseState.isPaused
				)
			}
			composable("ScreenElevation") {
				ScreenElevation(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					elevation = recordActivity.lastElevation,
					timeToDuration = recordActivity::timeToDuration,
					targetProgress = recordActivity.targetProgress,
					targetAchieved = recordActivity.targetAchieved,
					isPaused = recordActivity.exerciseState.isPaused
				)
			}
			composable("ScreenHeartRate") {
				ScreenHeartRate(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					heartRate = recordActivity.heartRate,
					timeToDuration = recordActivity::timeToDuration,
					targetProgress = recordActivity.targetProgress,
					targetAchieved = recordActivity.targetAchieved,
					isPaused = recordActivity.exerciseState.isPaused
				)
			}
			composable("ScreenSpeed") {
				ScreenSpeed(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					speed = recordActivity.lastSpeed,
					timeToDuration = recordActivity::timeToDuration,
					targetProgress = recordActivity.targetProgress,
					targetAchieved = recordActivity.targetAchieved,
					isPaused = recordActivity.exerciseState.isPaused
				)
			}
			composable("Cooldown") {
				Cooldown(
					modifier = modifierWithSwipe,
					duration = recordActivity.cooldownDuration,
					heartRate = recordActivity.heartRate,
					onStopClick = { recordActivity.showSummary() }
				)
			}
		}
		if(showCooldownDialog.value) {
			ConfirmDialog(
				text = R.string.dialog_cooldown,
				dismissText = R.string.dialog_cooldown_no,
				confirmText = R.string.dialog_cooldown_yes,
				onConfirm = {
					recordActivity.startCooldown()
					navController.navigate("Cooldown")
					showCooldownDialog.value = false
				},
				onDismiss = { recordActivity.showSummary() }
			)
		}
	}
}

fun WorkoutSettings.Screen.isAvailable(recordActivity: RecordActivity): Boolean =
	this.enabled.value &&
			(!this.needsLocation || !recordActivity.locationStatus.isDisabled) &&
			(!this.needsHeartRate || !recordActivity.heartRateStatus.isDisabled)
