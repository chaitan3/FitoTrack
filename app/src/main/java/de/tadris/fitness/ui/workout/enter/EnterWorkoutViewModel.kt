/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.ui.workout.enter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutBuilder
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.util.unit.DistanceUnitSystem
import de.tadris.fitness.util.unit.UnitUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class EnterWorkoutFormState(
    val workoutType: WorkoutType,
    val start: Calendar,
    val durationMillis: Long,
    val distanceText: String,
    val comment: String,
    val isEditMode: Boolean,
    val unitSystem: DistanceUnitSystem,
)

sealed interface EnterWorkoutUiState {
    data object Loading : EnterWorkoutUiState
    data class Ready(
        val form: EnterWorkoutFormState,
        val allWorkoutTypes: List<WorkoutType>,
        val distanceError: Boolean = false,
        val snackbarMessage: String? = null,
    ) : EnterWorkoutUiState

    data class Saved(val workoutId: Long) : EnterWorkoutUiState
}

class EnterWorkoutViewModel(
    application: Application,
    private val workoutId: Long,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<EnterWorkoutUiState>(EnterWorkoutUiState.Loading)
    val uiState: StateFlow<EnterWorkoutUiState> = _uiState

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        _uiState.value = EnterWorkoutUiState.Loading
        viewModelScope.launch {
            _uiState.value = withContext(Dispatchers.IO) {
                val instance = Instance.getInstance(getApplication())
                val unitSystem = instance.distanceUnitUtils.getDistanceUnitSystem()
                val allTypes = WorkoutTypeManager.getInstance().getAllTypesSorted(getApplication())

                if (workoutId != 0L) {
                    val workout = instance.db.workoutDao().getWorkoutById(workoutId)
                    if (workout == null) {
                        return@withContext EnterWorkoutUiState.Ready(
                            form = defaultForm(unitSystem),
                            allWorkoutTypes = allTypes,
                            snackbarMessage = getApplication<Application>().getString(R.string.cannotFindWorkout),
                        )
                    }
                    val builder = WorkoutBuilder.fromWorkout(getApplication(), workout)
                    EnterWorkoutUiState.Ready(
                        form = EnterWorkoutFormState(
                            workoutType = builder.workoutType,
                            start = builder.start,
                            durationMillis = builder.duration,
                            distanceText = formatDistance(builder.distance, unitSystem),
                            comment = builder.comment ?: "",
                            isEditMode = true,
                            unitSystem = unitSystem,
                        ),
                        allWorkoutTypes = allTypes,
                    )
                } else {
                    EnterWorkoutUiState.Ready(
                        form = defaultForm(unitSystem),
                        allWorkoutTypes = allTypes,
                    )
                }
            }
        }
    }

    private fun defaultForm(unitSystem: DistanceUnitSystem): EnterWorkoutFormState {
        val defaultType = WorkoutTypeManager.getInstance()
            .getWorkoutTypeById(getApplication(), WorkoutTypeManager.WORKOUT_TYPE_ID_RUNNING)
        return EnterWorkoutFormState(
            workoutType = defaultType,
            start = Calendar.getInstance(),
            durationMillis = 1000L * 60 * 10,
            distanceText = formatDistance(500.0, unitSystem),
            comment = "",
            isEditMode = false,
            unitSystem = unitSystem,
        )
    }

    private fun formatDistance(meters: Double, unitSystem: DistanceUnitSystem): String =
        UnitUtils.roundDouble(unitSystem.getDistanceFromKilometers(meters / 1000.0), 3).toString()

    private fun updateReady(update: (EnterWorkoutUiState.Ready) -> EnterWorkoutUiState.Ready) {
        val current = _uiState.value as? EnterWorkoutUiState.Ready ?: return
        _uiState.value = update(current)
    }

    private fun updateForm(update: (EnterWorkoutFormState) -> EnterWorkoutFormState) =
        updateReady { it.copy(form = update(it.form)) }

    fun setWorkoutType(type: WorkoutType) = updateForm { it.copy(workoutType = type) }

    fun setDate(year: Int, month: Int, day: Int) = updateForm {
        val newStart = it.start.clone() as Calendar
        newStart.set(year, month, day)
        it.copy(start = newStart)
    }

    fun setTime(hour: Int, minute: Int) = updateForm {
        val newStart = it.start.clone() as Calendar
        newStart.set(Calendar.HOUR_OF_DAY, hour)
        newStart.set(Calendar.MINUTE, minute)
        newStart.set(Calendar.SECOND, 0)
        it.copy(start = newStart)
    }

    fun setDuration(millis: Long) = updateForm { it.copy(durationMillis = millis) }

    fun setDistanceText(text: String) =
        updateReady { it.copy(form = it.form.copy(distanceText = text), distanceError = false) }

    fun setComment(comment: String) = updateForm { it.copy(comment = comment) }

    fun clearSnackbar() = updateReady { it.copy(snackbarMessage = null) }

    fun save() {
        val current = _uiState.value as? EnterWorkoutUiState.Ready ?: return
        val form = current.form
        val app = getApplication<Application>()

        val distanceInLongUnit = form.distanceText.toDoubleOrNull()
        if (distanceInLongUnit == null) {
            _uiState.value = current.copy(distanceError = true)
            return
        }
        if (form.start.timeInMillis > System.currentTimeMillis()) {
            _uiState.value =
                current.copy(snackbarMessage = app.getString(R.string.errorWorkoutAddFuture))
            return
        }
        if (form.durationMillis < 1000) {
            _uiState.value =
                current.copy(snackbarMessage = app.getString(R.string.errorEnterValidDuration))
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val distanceMeters = form.unitSystem.getMetersFromLongDistance(distanceInLongUnit)

                val builder = if (form.isEditMode) {
                    val workout =
                        Instance.getInstance(app).db.workoutDao().getWorkoutById(workoutId)!!
                    WorkoutBuilder.fromWorkout(app, workout)
                } else {
                    WorkoutBuilder(app)
                }

                builder.workoutType = form.workoutType
                builder.start = form.start
                builder.duration = form.durationMillis
                builder.distance = distanceMeters
                builder.comment = form.comment

                val workout = builder.saveWorkout(app)
                _uiState.value = EnterWorkoutUiState.Saved(workout.id)
            }
        }
    }
}
