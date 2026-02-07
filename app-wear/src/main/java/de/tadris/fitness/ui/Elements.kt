/*
 * Copyright (c) 2026 Bart Vullings <dev@windkracht8.com>
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
package de.tadris.fitness.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LocalTextStyle
import androidx.wear.compose.material3.MaterialTheme.colorScheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import de.tadris.fitness.R
import de.tadris.fitness.data.MapStore
import de.tadris.fitness.ui.record.RecordActivity.TargetAchieved
import de.tadris.fitness.util.logD
import de.tadris.fitness.util.logE
import de.tadris.fitness.util.pretty
import de.tadris.fitness.util.prettyDuration
import de.tadris.fitness.util.prettySpeed
import de.tadris.fitness.util.toast
import org.mapsforge.core.graphics.Style
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.model.Model
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import kotlin.math.floor

@Composable
fun Field(
	modifier: Modifier,
	value: String,
	unitText: Int? = null,
	unitIcon: Int? = null,
	unitIconWeight: Float = 1f,
	isActive: Boolean = true
){
	Row(
		modifier = modifier.padding(horizontal = 5.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		BasicText(
			modifier = Modifier.weight(3F),
			text =
				if(unitText == null) value
				else stringResource(unitText, value),
			style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
			color = { if(isActive) ColorWhite else ColorInactive },
			maxLines = 1,
			autoSize = TextAutoSize.StepBased(minFontSize = 8.sp)
		)
		if(unitIcon != null) {
			Icon(
				modifier = Modifier.weight(unitIconWeight),
				imageVector = ImageVector.vectorResource(unitIcon),
				contentDescription = "unit icon"
			)
		}
	}
}

@Composable
fun TargetProgress(
	targetProgress: MutableFloatState,
	targetAchieved: MutableState<TargetAchieved>,
){
	CircularProgressIndicator(
		modifier = Modifier.fillMaxSize(),
		progress = { targetProgress.floatValue },
		colors = ProgressIndicatorDefaults.colors(
			indicatorColor = if(targetAchieved.value == TargetAchieved.FULL) ColorProgressDone else ColorFOT,
			trackColor = if(targetAchieved.value == TargetAchieved.FULL) ColorProgressDoneDark else ColorTransparent,
		),
		strokeWidth = 3.dp
	)
}

@Composable
fun PreviewRoundScreen(){
	CircularProgressIndicator(
		modifier = Modifier.fillMaxSize(),
		progress = { 1.0f },
		strokeWidth = 1.dp
	)
}

data class ChartData(val duration: Long, val value: Double)

@Composable
fun Chart(
	data: List<ChartData>,
	target1: Double? = null,
	target2: Double? = null,
	yMinInit: Double = 0.0,
	yMaxInit: Double = 100.0,
	isSpeed: Boolean = false
) {
	val textMeasurer = rememberTextMeasurer()
	val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(25f, 5f)) }

	val yMin = remember(data.size) {
		minOf(
			data.minOfOrNull { it.value } ?: yMinInit,
			yMinInit, target1 ?: yMinInit, target2 ?: yMinInit
		)
	}
	val yMax = remember(data.size) { maxOf(
		data.maxOfOrNull { it.value } ?: yMaxInit,
		yMaxInit, target1 ?: yMaxInit, target2 ?: yMaxInit
	) }

	val labelHeight = remember { measureHeight(textMeasurer, "00:00") }
	val xLabelWidth = remember { measureWidth(textMeasurer, "0:00:00") }
	val top = remember { labelHeight/2 }

	Canvas(Modifier.fillMaxSize()){
		val height = size.height - labelHeight*1.5f - 2f
		val yLabelWidth = measureWidth(textMeasurer, if(isSpeed) yMax.prettySpeed() else yMax.pretty())
		val left = yLabelWidth + 5f
		fun drawYAxisLabelAndLine(value: Double, yPosition: Float, isFinal: Boolean = false) {
			val textLayoutResult = textMeasurer.measure(
				text = if(isSpeed) value.prettySpeed() else value.pretty(),
				style = TextStyle(color = ColorWhite, fontSize = 12.sp)
			)
			drawText(
				textLayoutResult = textLayoutResult,
				topLeft = Offset(yLabelWidth - textLayoutResult.size.width, yPosition-(labelHeight/2)),
			)
			drawLine(
				color = if(isFinal) ColorWhite else ColorInactive,
				start = Offset(left, yPosition),
				end = Offset(size.width, yPosition),
				strokeWidth = if(isFinal) 2f else 1f,
				pathEffect = if(isFinal) null else dashPathEffect
			)
		}
		val yLabelCount = floor(height / (labelHeight+3f)).toInt()-1
		val yLabelStep = height/yLabelCount
		val yDiff = yMax - yMin
		val yDiffStep = yDiff/yLabelCount
		val yPixelPerDiff = if(yDiff == 0.0) 0f else (height / yDiff).toFloat()
		drawYAxisLabelAndLine(yMax, top)
		for(i in 1..<yLabelCount){
			drawYAxisLabelAndLine(yMax-yDiffStep*i, top + yLabelStep*i)
		}
		drawYAxisLabelAndLine(yMin, top + height, true)

		fun drawXAxisLabelAndLine(duration: Long, xPosition: Float) {
			val textLayoutResult = textMeasurer.measure(
				text = duration.prettyDuration(),
				style = TextStyle(color = ColorWhite, fontSize = 12.sp)
			)
			drawText(
				textLayoutResult = textLayoutResult,
				topLeft = Offset(xPosition-(xLabelWidth/2), size.height - labelHeight),
			)
			drawLine(
				color = ColorInactive,
				start = Offset(xPosition, 0f),
				end = Offset(xPosition, top+height),
				strokeWidth = 1f,
				pathEffect = dashPathEffect
			)
		}
		val width = size.width - left - xLabelWidth/2
		val xLabelCount = maxOf(floor(width / (xLabelWidth+10f)).toInt(), 1)
		val xLabelStep: Float = width/xLabelCount
		val xMin = if(data.isEmpty()) 0L else data.minOf { it.duration }
		val xMax =
			if(data.isEmpty()) 60000L
			else maxOf(data.maxOf { it.duration }, 60000L)
		val xDiff: Long = xMax - xMin
		val xDiffStep: Long = xDiff/xLabelCount//can be long, because it is in ms
		val xPixelPerDiff: Float = width / xDiff
		for(i in 1..<xLabelCount){
			drawXAxisLabelAndLine(xMin+xDiffStep*i, left + xLabelStep*i)
		}
		drawXAxisLabelAndLine(xMax, size.width-(xLabelWidth/2))

		drawLine(
			color = ColorWhite,
			start = Offset(left, 0f),
			end = Offset(left, top+height),
			strokeWidth = 2.0f
		)
		if(target1 != null){
			val yTarget = top + ((yMax - target1) * yPixelPerDiff).toFloat()
			drawLine(
				color = ColorWhite,
				start = Offset(left, yTarget),
				end = Offset(size.width, yTarget),
				strokeWidth = 2f
			)
		}
		if(target2 != null){
			val yTarget = top + ((yMax - target2) * yPixelPerDiff).toFloat()
			drawLine(
				color = ColorWhite,
				start = Offset(left, yTarget),
				end = Offset(size.width, yTarget),
				strokeWidth = 2f
			)
		}
		if(data.isNotEmpty()) {
			val path = Path()
			data.forEachIndexed { index, it ->
				if(index < data.lastIndex && data.size > 800 && index % 2 != 0) return@forEachIndexed
				val x = left + 2 + (it.duration - xMin) * xPixelPerDiff
				val y = top + ((yMax - it.value) * yPixelPerDiff).toFloat()
				if(index == 0) path.moveTo(x, y)
				else path.lineTo(x, y)
			}
			drawPath(
				path = path,
				color = ColorFOT,
				style = Stroke(width = 3f)
			)
		}
	}
}
fun measureHeight(textMeasurer: TextMeasurer, text: String): Float =
	textMeasurer.measure(text = text, style = TextStyle(fontSize = 12.sp)).size.height.toFloat()
fun measureWidth(textMeasurer: TextMeasurer, text: String): Float =
	textMeasurer.measure(text = text, style = TextStyle(fontSize = 12.sp)).size.width.toFloat()

@Composable
fun ConfirmDialog(
	text: Int? = null,
	textString: String? = null,
	confirmText: Int,
	dismissText: Int,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit
){
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(colorScheme.background),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	){
		if(text != null) Text(text = stringResource(text))
		if(textString != null) Text(text = textString)
		Row(Modifier.fillMaxWidth()){
			TextButton(
				modifier = Modifier.weight(1f),
				onClick = onDismiss
			){
				Text(
					text = stringResource(dismissText),
					color = colorScheme.outline
				)
			}
			TextButton(
				modifier = Modifier.weight(1f),
				onClick = onConfirm
			){
				Text(
					text = stringResource(confirmText),
					color = colorScheme.outline
				)
			}
		}
	}
}

fun getTileLayerOnline(context: Context, model: Model): TileDownloadLayer {
	val tileCache = AndroidUtil.createTileCache(
		context,
		"online",
		model.displayModel.tileSize,
		Main.widthPixels,
		Main.heightPixels,
		model.frameBufferModel.overdrawFactor
	)
	val tileSource = OpenStreetMapMapnik.INSTANCE
	tileSource.userAgent = "de.tadris.fitness.wearos"
	val tileLayerOnline = TileDownloadLayer(
		tileCache,
		model.mapViewPosition,
		tileSource,
		AndroidGraphicFactory.INSTANCE
	)
	return tileLayerOnline
}
fun getTileLayerLocal(context: Context, model: Model): TileRendererLayer {
	val tileCache = AndroidUtil.createTileCache(
		context,
		"local",
		model.displayModel.tileSize,
		Main.widthPixels,
		Main.heightPixels,
		model.frameBufferModel.overdrawFactor
	)
	val multiMap = MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL)
	MapStore.getLocalMaps(context).forEach {
		logD("loading: " + it.name)
		try {
			multiMap.addMapDataStore(
				MapFile(it.getLocalFile(context)),
				false,
				false
			)
		} catch(_: Exception) {
			logE("addMapDataStore failed for map: " + it.name)
			context.toast(context.getString(R.string.corrupt_map, it.name))
		}
	}
	return AndroidUtil.createTileRendererLayer(
		tileCache,
		model.mapViewPosition,
		multiMap,
		MapsforgeThemes.OSMARENDER
	)
}
fun getMapPathLine(color: Color = ColorFOT): Polyline{
	val paintStroke = AndroidGraphicFactory.INSTANCE.createPaint()
	paintStroke.setStyle(Style.STROKE)
	paintStroke.strokeWidth = 10f
	paintStroke.color = color.toArgb()
	return Polyline(paintStroke, AndroidGraphicFactory.INSTANCE)
}

@Composable
fun FadingHorizontalDivider(modifier: Modifier = Modifier) {
	Box(
		modifier.fillMaxWidth().height(1.dp)
			.background(
				brush = Brush.horizontalGradient(
					listOf(Color.Transparent, ColorInactive, Color.Transparent)
				)
			)
	)
}

@Composable
fun FadingVerticalDivider(modifier: Modifier = Modifier) {
	Box(
		modifier.fillMaxHeight().width(1.dp)
			.background(
				brush = Brush.verticalGradient(
					listOf(Color.Transparent, ColorInactive, ColorInactive, Color.Transparent)
				)
			)
	)
}
