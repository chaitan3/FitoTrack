package de.tadris.fitness.recording.component

import de.tadris.fitness.data.RepetitionSample
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import de.tadris.fitness.recording.event.RecordingStateChangeEvent
import de.tadris.fitness.recording.indoor.exercise.RepetitionRecognizedEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class RepetitionSampleComponent : RecorderServiceComponent {

    private lateinit var service: RecorderService
    val minRepetitionCount get() = service.stateHolder.workoutType.minDistance
    val lastPush = 0L
    val repetitionAggregation = mutableListOf<RepetitionRecognizedEvent>()

    override fun register(service: RecorderService) {
        this.service = service
        EventBus.getDefault().register(this)
    }

    override fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onRepetitionDetected(e: RepetitionRecognizedEvent){
        repetitionAggregation.add(e)
        if(repetitionAggregation.size >= minRepetitionCount){
            pushSample()
        }
    }

    @Subscribe
    fun onRecordingStateChanged(e: RecordingStateChangeEvent){
        pushSample()
    }

    private fun pushSample(){
        if(repetitionAggregation.isEmpty()) return
        val info = AggregatedRecordingValuesEvent.getCurrent()

        val startTime = repetitionAggregation.minOf { it.timestamp }
        val endTime = repetitionAggregation.maxOf { it.timestamp }
        val repetitions = repetitionAggregation.size
        val frequency =
            if(lastPush != 0L) repetitions / ((endTime - lastPush) / 1000.0)
            else if(endTime - startTime > 500) repetitions / ((endTime - startTime) / 1000.0)
            else 0.0

        val sample = RepetitionSample(
            service.stateHolder.workoutId,
            System.nanoTime(),
            startTime,
            endTime,
            info.activeDuration,
            repetitionAggregation.first().type,
            repetitions,
            repetitionAggregation.sumOf { it.intensity } / repetitionAggregation.size,
            frequency
        )

        service.instance.db.workoutDao().insertSample(sample)
        EventBus.getDefault().post(sample)
    }

}