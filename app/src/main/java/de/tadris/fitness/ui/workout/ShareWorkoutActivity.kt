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
package de.tadris.fitness.ui.workout

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.aggregation.WorkoutInformationManager
import de.tadris.fitness.aggregation.information.Hidden
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.ui.dialog.SelectShareInformationDialog
import de.tadris.fitness.ui.record.InfoViewHolder
import de.tadris.fitness.util.DataManager
import de.tadris.fitness.util.Icon
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

const val TAG = "ShareWorkoutActivity"

class ShareWorkoutActivity : ShowWorkoutColoredMapActivity() {

    private lateinit var informationManager: WorkoutInformationManager

    private lateinit var customizableMetricFieldViewHolders: Array<InfoViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBeforeContent()
        this.setContentView(R.layout.activity_share_workout)

        informationManager = WorkoutInformationManager(this)
        informationManager.addInformation(Hidden(this))

        initRoot()
        initContents()
        initAfterContent()
        fullScreenItems = true
        addMap()
        mapView.isClickable = true
    }

    override fun isDiagramActivity(): Boolean {
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share_workout_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.actionShareWorkout) {
            shareWorkoutActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun initRoot() {
        root = findViewById(R.id.showWorkoutMapParent)
    }

    private fun initContents() {
        this.initViewHolders()
        val activityImage = findViewById<ImageView>(R.id.shareWorkoutActivityIconImageView)
        val activityIcon = workout.getWorkoutType(this).icon
        if (activityIcon != null && Icon.getIcon(activityIcon) != Icon.OTHER.iconRes) {
            // An Icon will only be set if the activity Icon is not null AND the activity is not
            // "other". It would look a bit weird with a "?"-icon on the shared image.
            activityImage.setImageResource(Icon.getIcon(activityIcon))
        } else {
            activityImage.visibility = View.INVISIBLE
        }
    }

    /**
     * This method creates the metric view-holders (clickable text at the bottom of the
     * activity's view)
     */
    private fun initViewHolders() {
        Log.v(TAG, "Initializing Customizable Metric Fields")
        this.customizableMetricFieldViewHolders = arrayOf(
                InfoViewHolder(0, { slot: Int, isLongClick: Boolean -> handleInfoViewClickEvent(slot, isLongClick) },
                        findViewById(R.id.shareWorkoutActivityCustomMetric1LabelTextview),
                        findViewById(R.id.shareWorkoutActivityCustomMetric1ValueTextview)),
                InfoViewHolder(1, { slot: Int, isLongClick: Boolean -> handleInfoViewClickEvent(slot, isLongClick) },
                        findViewById(R.id.shareWorkoutActivityCustomMetric2LabelTextview),
                        findViewById(R.id.shareWorkoutActivityCustomMetric2ValueTextview)),
                InfoViewHolder(2, { slot: Int, isLongClick: Boolean -> handleInfoViewClickEvent(slot, isLongClick) },
                        findViewById(R.id.shareWorkoutActivityCustomMetric3LabelTextview),
                        findViewById(R.id.shareWorkoutActivityCustomMetric3ValueTextview))
        )

        // Update all slots immediately
        for(i in 0 until this.customizableMetricFieldViewHolders.size) {
            this.updateSlot(i)
        }
    }

    private fun shareWorkoutActivity() {
        val bitmap = getBitmapFromView(findViewById(R.id.shareWorkout))
        try {
            val ts = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toString()
            val parent = File(DataManager.getSharedDirectory(this))
            val file = File(parent, "fitotrack-workout_$ts.png")
            if (!parent.exists() && !parent.mkdirs()) {
                throw IOException("Cannot write")
            }
            val fOut = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)
            fOut.flush()
            fOut.close()
            val intent = Intent(Intent.ACTION_SEND)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val imgURI = DataManager.provide(this, file)
                intent.setDataAndType(imgURI, "image/png")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.putExtra(Intent.EXTRA_STREAM, imgURI)
            } else {
                intent.setDataAndType(Uri.fromFile(file), "image/png")
            }
            startActivity(Intent.createChooser(intent, null))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    /**
     * This callback is invoked when the user interacts with one of the customizable metric fields.
     * @param slot An Int that identifies the Text field
     * @param isLongClick is true if the user held the button instead of just tapping it
     */
    private fun handleInfoViewClickEvent(slot: Int, isLongClick: Boolean) {
        Log.v(
            TAG,
            String.format(
                "User clicked on Custom Metric field (Slot #%d), longClick=%b",
                slot,
                isLongClick
            )
        )
        SelectShareInformationDialog(
            this,
            informationManager,
            this.workout,
            slot,
            this::handleWorkoutInformationSelectEvent
        ).show()
    }

    /**
     * This callback is invoked when the user has selected the new metric to display in the provided
     * slot.
     * @param slot the Index of the ViewHolder that has been modified.
     * @param informationTypeId Type of shown information
     */
    private fun handleWorkoutInformationSelectEvent(slot: Int, informationTypeId: String) {
        Instance.getInstance(this).userPreferences.sharingScreenInformationPreferences
            .setIdOfDisplayedInformation(RecordingType.GPS.id, slot, informationTypeId)
        this.updateSlot(slot)
    }

    /**
     * Update a specific custom metric slot
     * @param slot The "slot" to update
     */
    private fun updateSlot(slot: Int) {
        Log.v(TAG, String.format("Updating data in Slot #%d", slot))
        val prefs = Instance.getInstance(this).userPreferences.sharingScreenInformationPreferences;
        // This is the type of Data we want to display in the Slot
        val informationType = prefs.getIdOfDisplayedInformation(RecordingType.GPS.id, slot);
        val information = informationManager.findById(informationType) ?: Hidden(this)

        val infoViewHolder = customizableMetricFieldViewHolders[slot]
        if (information is Hidden) {
            infoViewHolder.setText("           ", "           ")
            return
        }

        val label = getString(information.titleRes);
        val data = information.getFormattedValueFromWorkout(workout)

        infoViewHolder.setText(label, data);
    }
}