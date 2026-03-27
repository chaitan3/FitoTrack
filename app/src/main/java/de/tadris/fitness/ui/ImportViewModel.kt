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

package de.tadris.fitness.ui

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tadris.fitness.util.io.general.IOHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object ShowingChoice : ImportUiState
    data object ShowingMassImportConfirm : ImportUiState
    data class Importing(val progress: Int?) : ImportUiState
    data class Done(val count: Int) : ImportUiState
    data class Error(val message: String) : ImportUiState
}

class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState

    fun requestImport() {
        _uiState.value = ImportUiState.ShowingChoice
    }

    fun dismissChoice() {
        _uiState.value = ImportUiState.Idle
    }

    fun requestMassImportConfirm() {
        _uiState.value = ImportUiState.ShowingMassImportConfirm
    }

    fun dismissMassImportConfirm() {
        _uiState.value = ImportUiState.Idle
    }

    fun importFile(uri: Uri) {
        _uiState.value = ImportUiState.Importing(null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = getApplication<Application>().contentResolver.openInputStream(uri)
                IOHelper.GpxImporter.importWorkout(getApplication(), stream)
                _uiState.value = ImportUiState.Done(1)
            } catch (e: Exception) {
                _uiState.value =
                    ImportUiState.Error(e.localizedMessage ?: e.message ?: "Unknown error")
            }
        }
    }

    fun importFolder(uri: Uri) {
        _uiState.value = ImportUiState.Importing(0)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val documentFile = DocumentFile.fromTreeUri(getApplication(), uri)
                    ?: throw IllegalStateException("Cannot open folder")
                val files = documentFile.listFiles().filter { it.isFile && it.canRead() }
                var imported = 0
                files.forEachIndexed { i, file ->
                    _uiState.value = ImportUiState.Importing(100 * i / files.size)
                    try {
                        val stream =
                            getApplication<Application>().contentResolver.openInputStream(file.uri)
                        IOHelper.GpxImporter.importWorkout(getApplication(), stream)
                        imported++
                    } catch (e: Exception) {
                        if (imported == 0 && i == files.size - 1) throw e
                    }
                }
                _uiState.value = ImportUiState.Done(imported)
            } catch (e: Exception) {
                _uiState.value =
                    ImportUiState.Error(e.localizedMessage ?: e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _uiState.value = ImportUiState.Idle
    }
}
