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
import de.tadris.fitness.data.SettingsStore
import de.tadris.fitness.data.SettingsStore.WorkoutTypeSettings
import de.tadris.fitness.data.WorkoutData.WorkoutType
import de.tadris.fitness.ui.ConfirmDialog
import de.tadris.fitness.ui.record.RecordActivity.TargetAchieved
import de.tadris.fitness.util.logD
import org.mapsforge.core.model.LatLong

enum class SwipeDirection {LEFT, RIGHT, UP, DOWN}
@Composable
fun RecordView(recordActivity: RecordActivity) {
	val navController = rememberSwipeDismissableNavController()
	val showCooldownDialog = remember { mutableStateOf(false) }
	fun navFirstScreen(){
		if(recordActivity.workoutTypeSettings.screens[1].enabled.value){
			navController.navigate("ScreenDefault")
		}else{
			recordActivity.workoutTypeSettings.screens.firstOrNull{ it.isAvailable(recordActivity) }?.let{
				navController.navigate(it.navRoute)
			}
		}
	}

	var swipeDirection: SwipeDirection = SwipeDirection.LEFT
	fun onSwipe(swipeDirection: SwipeDirection){
		val route = navController.currentBackStackEntry?.destination?.route
		//logD("onSwipe $route $swipeDirection")
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
					"TargetEntry/{primary}/{leg}" -> {
						val primary = navController.currentBackStackEntry?.arguments?.getString("primary") == "true"
						if(primary) recordActivity.workoutData.primaryTargetType = null
						else recordActivity.workoutData.secondaryTargetType = null
						navController.popBackStack()
					}
					"RecordSettings/{leg}" -> {
						recordActivity.setScreenOnFlag()
						navController.popBackStack()
					}
					"ScreenMultisport" -> {
						if(recordActivity.workoutData.endTime == 0L)
							navController.popBackStack("ScreenStop", inclusive = false)
					}
					else -> navController.popBackStack()
				}
			}
			SwipeDirection.DOWN -> {
				var routeFound = false
				for(screenSettings in recordActivity.workoutTypeSettings.screens){
					if(routeFound && screenSettings.isAvailable(recordActivity)){
						navController.navigate(screenSettings.navRoute)
						return
					}
					if(screenSettings.navRoute == route) routeFound = true
				}
			}
			SwipeDirection.UP -> {
				var routeFound = false
				for(screenSettings in recordActivity.workoutTypeSettings.screens.asReversed()){
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
		val mapCenter = remember { mutableStateOf(MapCenter.POSITION) }
		val mapZoomLevel = remember { mutableStateOf<Byte>(15) }
		val mapPosition = remember { mutableStateOf<LatLong?>(null) }
		SwipeDismissableNavHost(
			userSwipeEnabled = false,
			navController = navController,
			startDestination = "Prepare"
		) {
			composable("Prepare") {
				Prepare(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					onWorkoutTypeClick = { navController.navigate("TypeSelect/-1/false") },
					onTargetClick = {
						if(recordActivity.workoutData.primaryTargetType == null) {
							navController.navigate("Target/-1")//add Target to the backstack
							navController.navigate("TargetTypeSelect/true/-1")
						} else navController.navigate("Target/-1")
					},
					onStartClick = {
						recordActivity.startExercise()
						navController.navigate("ScreenStop")
						navFirstScreen()
					},
					locationStatus = recordActivity.locationStatus,
					locationAccuracy = recordActivity.locationAccuracy,
					heartRateStatus = recordActivity.heartRateStatus,
					onSettingsClick = {
						if(recordActivity.workoutData.workoutType == WorkoutType.MULTISPORT)
							navController.navigate("Multisport")
						else navController.navigate("RecordSettings/-1")
					}
				)
			}
			composable("TypeSelect/{leg}/{noMultisport}") {
				val leg = it.arguments?.getString("leg")?.toIntOrNull() ?: -1
				val context = LocalContext.current
				TypeSelect (
					modifier = modifierWithSwipe,
					onItemClick = { workoutType ->
						if(leg >= 0) recordActivity.workoutData.legs[leg].workoutType = workoutType
						else{
							recordActivity.workoutData.workoutType = workoutType
							recordActivity.workoutTypeSettings = SettingsStore.getWorkoutTypeSettings(context, workoutType)
							recordActivity.setScreenOnFlag()
						}
						navController.popBackStack()
						if(workoutType == WorkoutType.MULTISPORT)
							navController.navigate("Multisport")
					},
					noMultisport = it.arguments?.getString("noMultisport") == "true"
				)
			}
			composable("Target/{leg}") {
				val leg = it.arguments?.getString("leg")?.toIntOrNull() ?: -1
				val workoutData =
					if(leg >= 0) recordActivity.workoutData.legs[leg]
					else recordActivity.workoutData
				Target(
					modifier = modifierWithSwipe,
					workoutData = workoutData,
					onPrimaryClicked = { navController.navigate("TargetTypeSelect/true/$leg") },
					onSecondaryClicked = { navController.navigate("TargetTypeSelect/false/$leg") }
				)
			}
			composable("TargetTypeSelect/{primary}/{leg}") {
				val primary = it.arguments?.getString("primary") == "true"
				val leg = it.arguments?.getString("leg")?.toIntOrNull() ?: -1
				val workoutData =
					if(leg >= 0) recordActivity.workoutData.legs[leg]
					else recordActivity.workoutData
				TargetTypeSelect(
					modifier = modifierWithSwipe,
					targetType =
						if(primary) workoutData.primaryTargetType
						else workoutData.secondaryTargetType,
					disabledTargetType = null,
					onTargetSelected = { targetType ->
						if(primary) {
							workoutData.primaryTargetType = targetType
							recordActivity.targetProgress.floatValue = 0.0F
						} else {
							workoutData.secondaryTargetType = targetType
						}
						navController.popBackStack()
						if(targetType != null)
							navController.navigate("TargetEntry/$primary/$leg")
					}
				)
			}
			composable("TargetEntry/{primary}/{leg}") {
				val primary = it.arguments?.getString("primary") == "true"
				val leg = it.arguments?.getString("leg")?.toIntOrNull() ?: -1
				val workoutData =
					if(leg >= 0) recordActivity.workoutData.legs[leg]
					else recordActivity.workoutData
				TargetEntry(
					modifier = modifierWithSwipe,
					targetType =
						if(primary) workoutData.primaryTargetType
						else workoutData.secondaryTargetType,
					onBack = {
						if(primary) workoutData.primaryTargetType = null
						else workoutData.secondaryTargetType = null
						navController.popBackStack()
					},
					onDone = { value: Int, valueMax: Int ->
						if(value+valueMax == 0) {
							if(primary) workoutData.primaryTargetType = null
							else workoutData.secondaryTargetType = null
						} else {
							if(primary) {
								workoutData.primaryTargetValue = value
								workoutData.primaryTargetValueHigh = valueMax
							} else {
								workoutData.secondaryTargetValue = value
								workoutData.secondaryTargetValueHigh = valueMax
							}
						}
						if(primary) recordActivity.targetAchieved.value = TargetAchieved.NOT
						navController.popBackStack()
					}
				)
			}
			composable("Multisport") {
				Multisport(
					modifier = modifierWithSwipe,
					workoutData = recordActivity.workoutData,
					navController
				)
			}
			composable("MultisportTransition") {
				MultisportTransition(
					modifier = modifierWithSwipe,
					rootWorkoutData = recordActivity.workoutData.rootWorkout ?: recordActivity.workoutData,
					onNextLegClick = {
						recordActivity.workoutData.nextLeg?.let { nextLeg ->
							logD("switching to next leg")
							recordActivity.workoutData = nextLeg
							recordActivity.startExercise()
							navController.navigate("ScreenStop")
							navFirstScreen()
						}
					}
				)
			}
			composable("RecordSettings/{leg}") {
				val leg = it.arguments?.getString("leg")?.toIntOrNull() ?: -1
				val workoutData =
					if(leg >= 0) recordActivity.workoutData.legs[leg]
					else recordActivity.workoutData
				RecordSettings(
					modifier = modifierWithSwipe,
					showTarget = navController.previousBackStackEntry?.destination?.route != "Prepare",
					workoutData,
					hasLocation = !recordActivity.locationStatus.isDisabled,
					hasHeartRate = !recordActivity.heartRateStatus.isDisabled,
					speakEnabled = recordActivity.speakEnabled,
					toggleSpeak =  recordActivity::toggleSpeak,
					onTargetClicked = { navController.navigate("Target/$leg") }
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
						if(recordActivity.workoutData.nextLeg != null) {
							navController.navigate("MultisportTransition")
						} else {
							if(recordActivity.heartRateStatus.isDisabled) recordActivity.showSummary()
							else showCooldownDialog.value = true
						}
					},
					onSettingsClick = { navController.navigate("RecordSettings/-1") }
				)
			}
			composable("ScreenMap") {
				val hasOtherScreens = recordActivity.workoutTypeSettings.screens.count {
					it.isAvailable(recordActivity)
				} > 1
				ScreenMap(
					workoutData = recordActivity.workoutData,
					targetProgress = recordActivity.targetProgress,
					targetAchieved = recordActivity.targetAchieved,
					isPaused = recordActivity.exerciseState.isPaused,
					hasOtherScreens,
					mapCenter,
					mapZoomLevel,
					mapPosition,
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
			composable("ScreenMultisport") {
				MultisportTransition(
					modifier = modifierWithSwipe,
					rootWorkoutData = recordActivity.workoutData.rootWorkout ?: recordActivity.workoutData,
					onNextLegClick = {}
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

fun WorkoutTypeSettings.Screen.isAvailable(recordActivity: RecordActivity): Boolean =
	if(isMultisport && recordActivity.workoutData.rootWorkout?.workoutType == WorkoutType.MULTISPORT) true
	else if(isTarget && recordActivity.workoutData.primaryTargetType != null) true
	else if(needsLocation &&
		(recordActivity.locationStatus.isDisabled || !recordActivity.workoutData.isWorkoutTypeGps)
	) false
	else if(needsHeartRate && recordActivity.heartRateStatus.isDisabled) false
	else enabled.value
