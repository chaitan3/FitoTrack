package de.tadris.fitness.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutDao
import de.tadris.fitness.ui.theme.FitoTrackTheme

@Composable
fun DashboardScreen(workoutDao: WorkoutDao){
    val workouts by workoutDao.getWorkoutsFlow().collectAsState(initial = emptyList())
    DashboardScreen(workouts)
}

@Composable
fun DashboardScreen(workouts: List<Workout>){

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val workout = Workout(0, "", System.currentTimeMillis() - 60000, System.currentTimeMillis(), 40000, "Test", 170.0, 10, 1000.0, "walking", -1)

    FitoTrackTheme {
        DashboardScreen(listOf(
            workout,
            workout,
            workout,
        ))
    }
}