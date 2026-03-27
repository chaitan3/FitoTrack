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

package de.tadris.fitness.ui.workout.show

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tadris.fitness.Instance
import de.tadris.fitness.data.CalculatedValues
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.util.WorkoutCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface WorkoutUiState {
    data object Loading : WorkoutUiState
    data class Error(val message: String) : WorkoutUiState
    data class Success(
        val workout: Workout,
        val data: WorkoutData,
        val calculatedValues: CalculatedValues,
        val sets: List<WorkoutCalculator.Set>,
        val isLastWorkout: Boolean,
    ) : WorkoutUiState
}

class ShowWorkoutViewModel(
    application: Application,
    private val workoutId: Long,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Loading)
    val uiState: StateFlow<WorkoutUiState> = _uiState

    init {
        loadWorkout()
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            _uiState.value = withContext(Dispatchers.IO) {
                try {
                    val instance = Instance.getInstance(getApplication())
                    val dao = instance.db.workoutDao()
                    val workout = dao.getWorkoutById(workoutId)
                        ?: return@withContext WorkoutUiState.Error("Workout not found")
                    val data = WorkoutData.fromWorkout(dao, workout)
                    val calculatedValues = CalculatedValues(data)
                    val sets = WorkoutCalculator.getSetsFromWorkout(data)
                    val isLastWorkout = dao.getLastWorkout()?.id == workout.id
                    WorkoutUiState.Success(workout, data, calculatedValues, sets, isLastWorkout)
                } catch (e: Exception) {
                    WorkoutUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun updateComment(comment: String) {
        val current = _uiState.value as? WorkoutUiState.Success ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val updated = current.workout.copy(comment = comment)
                Instance.getInstance(getApplication()).db.workoutDao().updateWorkout(updated)
                _uiState.value = current.copy(workout = updated)
            }
        }
    }

    fun refresh() {
        loadWorkout()
    }

    fun deleteWorkout(onDeleted: () -> Unit) {
        val current = _uiState.value as? WorkoutUiState.Success ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Instance.getInstance(getApplication()).db.workoutDao()
                    .deleteWorkout(current.workout)
            }
            onDeleted()
        }
    }
}
