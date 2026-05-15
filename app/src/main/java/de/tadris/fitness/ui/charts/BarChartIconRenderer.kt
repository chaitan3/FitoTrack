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

package de.tadris.fitness.ui.charts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import de.tadris.fitness.R

/**
 * Custom BarCharRenderer, capable of drawing icons as labels in X-axis.
 *
 * NOT capable of handling stacked data sets
 * NOT capable of handling icons drawn instead of values on top of a bar entry
 *
 * Based on https://stackoverflow.com/a/45143200/3617426
 */
class BarChartIconRenderer(aChart: BarDataProvider, aAnimator: ChartAnimator,
                           aViewPortHandler: ViewPortHandler, aImageList: ArrayList<Bitmap>,
                           aContext: Context)
    : BarChartRenderer(aChart, aAnimator, aViewPortHandler) {

    private val mContext = aContext
    private val mImageList = aImageList

    override fun drawValues(aCanvas: Canvas) {
        if (!isDrawingValuesAllowed(mChart)) {
            return
        }

        val datasets = mChart.barData.dataSets
        val valueOffsetPlus = Utils.convertDpToPixel(3f)
        val imOffsetPlus = Utils.convertDpToPixel(22f)
        val valueTextHeight = Utils.calcTextHeight(mValuePaint, "8")

        for ((index, set) in datasets.withIndex()) {
            if (!shouldDrawValues(set)) {
                continue
            }

            applyValueTextStyle(set)

            var posOffset = if (mChart.isDrawValueAboveBarEnabled) {
                -valueOffsetPlus
            } else {
                valueTextHeight + valueOffsetPlus
            }

            var negOffset = if (mChart.isDrawValueAboveBarEnabled) {
                valueTextHeight + valueOffsetPlus
            } else {
                -valueOffsetPlus
            }
            var negOffsetImLabel = if (mChart.isDrawValueAboveBarEnabled) {
                valueTextHeight + imOffsetPlus
            } else {
                -imOffsetPlus
            }

            val inverted = mChart.isInverted(set.axisDependency)
            if (inverted) {
                posOffset = -posOffset - valueTextHeight
                negOffset = -negOffset - valueTextHeight
                negOffsetImLabel = - negOffsetImLabel - valueTextHeight
            }

            val buffer = mBarBuffers[index]
            val phaseY = mAnimator.phaseY
            val formatter = set.valueFormatter
            var j = -4

            // iconsOffset

            while (true) {
                j += 4
                if (j >= buffer.buffer.size * mAnimator.phaseX) {
                    break
                }

                val left = buffer.buffer[j]
                val top = buffer.buffer[j + 1]
                val right = buffer.buffer[j + 2]
                val bottom = buffer.buffer[j + 3]

                val x = (left + right) / 2f

                if (!mViewPortHandler.isInBoundsRight(x)) {
                    break
                }

                if (!mViewPortHandler.isInBoundsY(top) or !mViewPortHandler.isInBoundsLeft(x)) {
                    continue
                }

                val entry = set.getEntryForIndex(j / 4)
                val y = if (entry.y >= 0) {
                    top + posOffset
                } else {
                    bottom + negOffset
                }

                if (set.isDrawValuesEnabled) {
                    drawValue(aCanvas, formatter.getBarLabel(entry), x, y,
                            set.getValueTextColor(j / 4))
                }

                val bitmap = mImageList.getOrNull(j / 4)
                if (bitmap != null) {
                    val scaledBitmap = getScaledBitmap(bitmap)
                    aCanvas.drawBitmap(scaledBitmap, x - scaledBitmap.width / 2f,
                            (bottom + 0.5f * negOffsetImLabel) - scaledBitmap.width / 2f, null)
                }
            }
        }
    }

    private fun getScaledBitmap(aBitmap: Bitmap): Bitmap {
        val width = mContext.resources.getDimension(R.dimen.dimen_18).toInt()
        val height = mContext.resources.getDimension(R.dimen.dimen_18).toInt()
        return Bitmap.createScaledBitmap(aBitmap, width, height, true)
    }

}