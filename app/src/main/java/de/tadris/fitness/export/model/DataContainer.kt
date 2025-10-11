package de.tadris.fitness.export.model

import de.tadris.fitness.data.WorkoutType

data class DataContainer(
    val intervalSets: List<IntervalSetContainer>,
    val workoutTypes: List<WorkoutType>,
)