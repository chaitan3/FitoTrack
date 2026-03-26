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
package de.tadris.fitness.ui.settings

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.ExportTargetConfiguration
import de.tadris.fitness.ui.FitoTrackActivity
import de.tadris.fitness.ui.adapter.ExportTargetConfigurationAdapter
import de.tadris.fitness.ui.adapter.ExportTargetConfigurationAdapter.ExportTargetAdapterListener
import de.tadris.fitness.ui.dialog.ConfigureHttpPostDialog
import de.tadris.fitness.ui.dialog.SelectExportTargetTypeDialog
import de.tadris.fitness.ui.dialog.SelectExportTargetTypeDialog.ExportTargetTypeSelectListener
import de.tadris.fitness.util.autoexport.AutoExportPlanner
import de.tadris.fitness.util.autoexport.source.ExportSource
import de.tadris.fitness.util.autoexport.target.DirectoryTarget
import de.tadris.fitness.util.autoexport.target.ExportTarget
import de.tadris.fitness.util.autoexport.target.HTTPPostTarget

class ConfigureExportTargetsActivity : FitoTrackActivity(),
    ExportTargetAdapterListener, ExportTargetTypeSelectListener {

    companion object {
        const val EXTRA_SOURCE = "source"
        const val FILE_PICKER_CODE = 5
    }

    private var exportSourceId = ""

    private lateinit var adapter: ExportTargetConfigurationAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var explanationText: TextView
    private lateinit var hintText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure_export_targets)
        if (intent.extras != null) {
            exportSourceId = intent.extras!!
                .getString(EXTRA_SOURCE, "")
        }
        if (exportSourceId.isEmpty()) {
            finish()
            return
        }
        setTitle(ExportSource.getTitle(exportSourceId))
        setupActionBar()
        recyclerView = findViewById(R.id.exportTargetsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        hintText = findViewById(R.id.exportTargetsHint)
        explanationText = findViewById(R.id.exportTargetsExplanation)
        findViewById<View>(R.id.exportTargetsAdd).setOnClickListener { showAddDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val configurations =
            Instance.getInstance(this).db.exportTargetDao().findAllFor(exportSourceId)
        adapter = ExportTargetConfigurationAdapter(configurations.toList(), this)
        recyclerView.adapter = adapter
        hintText.visibility = if (configurations.isEmpty()) View.VISIBLE else View.GONE
        explanationText.visibility = if (configurations.isEmpty()) View.GONE else View.VISIBLE
        explanationText.text = getString(ExportSource.getExplanation(exportSourceId))
    }

    private fun showAddDialog() {
        SelectExportTargetTypeDialog(this, this).show()
    }

    override fun onTargetTypeSelect(target: ExportTarget) {
        if (target is DirectoryTarget) {
            showDirectoryPicker()
        } else if (target is HTTPPostTarget) {
            showHttpPostDialog()
        }
    }

    // DIRECTORY RELATED

    private fun showDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, FILE_PICKER_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == FILE_PICKER_CODE) {
            val uri = data?.data ?: return
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            saveDirectoryTarget(uri)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun saveDirectoryTarget(uri: Uri) {
        val configuration = ExportTargetConfiguration()
        configuration.source = exportSourceId
        configuration.type = ExportTarget.TARGET_TYPE_DIRECTORY
        configuration.data = uri.toString()
        configuration.insert()
    }

    // HTTP RELATED

    private fun showHttpPostDialog() {
        ConfigureHttpPostDialog(this) { url ->
            val configuration = ExportTargetConfiguration()
            configuration.source = exportSourceId
            configuration.type = ExportTarget.TARGET_TYPE_HTTP_POST
            configuration.data = url
            configuration.insert()
        }
    }

    private fun ExportTargetConfiguration.insert() {
        Instance.getInstance(this@ConfigureExportTargetsActivity).db.exportTargetDao().insert(this)
        onConfigurationChanged()
    }

    override fun onDelete(configuration: ExportTargetConfiguration) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.deleteExportTargetConfirmation)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _: DialogInterface?, _: Int ->
                delete(configuration)
            }
            .show()
    }

    private fun delete(configuration: ExportTargetConfiguration) {
        Instance.getInstance(this).db.exportTargetDao().delete(configuration)
        onConfigurationChanged()
    }

    private fun onConfigurationChanged() {
        if (exportSourceId == ExportSource.EXPORT_SOURCE_BACKUP) {
            AutoExportPlanner(this).planAutoBackup()
        }
        loadData()
    }

}