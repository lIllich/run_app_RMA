package com.example.run_app_rma.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.data.remote.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val runPostRepository: RunPostRepository,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    private val _followersCount = MutableStateFlow(0)
    val followersCount: StateFlow<Int> = _followersCount.asStateFlow()

    private val _postCount = MutableStateFlow(0)
    val postCount: StateFlow<Int> = _postCount.asStateFlow()

    // New StateFlows for weekly goal progress percentages (0-100)
    private val _weeklyStepsProgress = MutableStateFlow<Float?>(null)
    val weeklyStepsProgress: StateFlow<Float?> = _weeklyStepsProgress.asStateFlow()

    private val _weeklyDurationProgress = MutableStateFlow<Float?>(null)
    val weeklyDurationProgress: StateFlow<Float?> = _weeklyDurationProgress.asStateFlow()

    private val _weeklyDistanceProgress = MutableStateFlow<Float?>(null)
    val weeklyDistanceProgress: StateFlow<Float?> = _weeklyDistanceProgress.asStateFlow()

    private val TAG = "ProfileViewModel"

    init {
        // Fetch user profile and counts when the ViewModel is initialized
        fetchUserProfileAndCounts()
    }

    /**
     * Fetches the current user's profile, their following/followers counts,
     * and triggers the calculation of weekly goal progress.
     * This function is called on init and after profile updates.
     */
    fun fetchUserProfileAndCounts() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            // If user is not logged in, reset all relevant state flows
            _errorMessage.value = "User not logged in."
            _currentUser.value = null
            _followingCount.value = 0
            _followersCount.value = 0
            _postCount.value = 0
            _weeklyStepsProgress.value = null
            _weeklyDurationProgress.value = null
            _weeklyDistanceProgress.value = null
            Log.d(TAG, "fetchUserProfileAndCounts called but currentUserId is null. User not authenticated.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null // Clear previous error messages
        viewModelScope.launch {
            try {
                // Fetch user profile
                val userResult = userRepository.getUserProfile(userId)
                if (userResult.isSuccess) {
                    _currentUser.value = userResult.getOrNull()
                    // After successfully fetching user, calculate weekly progress
                    _currentUser.value?.let { user ->
                        calculateWeeklyProgress(user)
                    }
                } else {
                    _errorMessage.value = userResult.exceptionOrNull()?.message ?: "Failed to load profile."
                    _currentUser.value = null
                }

                // Fetch following count
                val followingCountResult = followRepository.getFollowingCount(userId)
                if (followingCountResult.isSuccess) {
                    _followingCount.value = followingCountResult.getOrNull() ?: 0
                } else {
                    Log.e(TAG, "Error fetching following count: ${followingCountResult.exceptionOrNull()?.message}")
                    _followingCount.value = 0
                }

                // Fetch followers count
                val followersCountResult = followRepository.getFollowersCount(userId)
                if (followersCountResult.isSuccess) {
                    _followersCount.value = followersCountResult.getOrNull() ?: 0
                } else {
                    Log.e(TAG, "Error fetching followers count: ${followersCountResult.exceptionOrNull()?.message}")
                    _followersCount.value = 0
                }

                // Fetch post count
                val userPostsResult = runPostRepository.getRunPostsByUsers(listOf(userId))
                if (userPostsResult.isSuccess) {
                    _postCount.value = userPostsResult.getOrNull()?.size ?: 0
                } else {
                    Log.e(TAG, "Error fetching user posts count: ${userPostsResult.exceptionOrNull()?.message}")
                    _postCount.value = 0
                }

            } catch (e: Exception) {
                // Catch any unexpected exceptions during the fetch operations
                _errorMessage.value = e.message ?: "An unexpected error occurred while fetching profile and counts."
                Log.e(TAG, "Unexpected error in fetchUserProfileAndCounts: ${e.message}", e)
            } finally {
                // Always set isLoading to false when the operation completes
                _isLoading.value = false
                Log.d(TAG, "fetchUserProfileAndCounts finished. isLoading set to false.")
            }
        }
    }

    /**
     * Calculates the progress towards weekly goals based on the user's run posts.
     * The progress is expressed as a percentage (0-100).
     */
    private fun calculateWeeklyProgress(user: User) {
        viewModelScope.launch {
            val userId = user.id
            val weeklyGoalSteps = user.weeklyGoalSteps
            val weeklyGoalDuration = user.weeklyGoalDuration // in milliseconds
            val weeklyGoalDistance = user.weeklyGoalDistance // in meters
            val goalsSetTimestamp = user.weeklyGoalsSetTimestamp

            // If no goals are set, reset progress to null
            if (weeklyGoalSteps == null && weeklyGoalDuration == null && weeklyGoalDistance == null) {
                _weeklyStepsProgress.value = null
                _weeklyDurationProgress.value = null
                _weeklyDistanceProgress.value = null
                Log.d(TAG, "No weekly goals set for user $userId.")
                return@launch // Use return@launch to exit the coroutine lambda
            }

            // Get the timestamp for the start of the current week (Monday 00:00:00)
            val currentWeekStart = getStartOfWeekTimestamp()

            // If goals were set before the current week, consider them expired for display purposes
            // and show 0% progress. A more robust solution might involve server-side logic
            // or specific weekly goal documents.
            if (goalsSetTimestamp != null && goalsSetTimestamp < currentWeekStart) {
                _weeklyStepsProgress.value = 0f
                _weeklyDurationProgress.value = 0f
                _weeklyDistanceProgress.value = 0f
                Log.d(TAG, "Weekly goals set before current week, resetting progress for display.")
                return@launch
            }

            // Fetch all run posts for the user to calculate current week's activity
            val userPostsResult = runPostRepository.getRunPostsByUsers(listOf(userId))
            if (userPostsResult.isSuccess) {
                // Filter posts that occurred within the current week
                val weeklyRuns = userPostsResult.getOrNull()?.filter { runPost: RunPost ->
                    // Ensure runPost.timestamp is not null and is within the current week
                    runPost.timestamp != null && runPost.timestamp.time >= currentWeekStart
                } ?: emptyList()

                var totalWeeklyDistance = 0f // in meters
                var totalWeeklyDuration = 0L // in milliseconds
                var totalWeeklySteps = 0

                // Aggregate data from weekly runs
                weeklyRuns.forEach { runPost ->
                    totalWeeklyDistance += runPost.distance
                    totalWeeklyDuration += (runPost.endTime - runPost.startTime)
                    // Placeholder for step estimation:
                    // Ideally, your RunPost data model would include actual step count.
                    // If not, you can estimate based on distance or average pace.
                    totalWeeklySteps += estimateStepsFromDistance(runPost.distance)
                }

                // Calculate and update progress percentages
                _weeklyStepsProgress.value = weeklyGoalSteps?.let {
                    if (it > 0) (totalWeeklySteps.toFloat() / it) * 100f else 0f
                } ?: 0f // If goal is null or zero, progress is 0%

                _weeklyDurationProgress.value = weeklyGoalDuration?.let {
                    if (it > 0) (totalWeeklyDuration.toFloat() / it) * 100f else 0f
                } ?: 0f

                _weeklyDistanceProgress.value = weeklyGoalDistance?.let {
                    if (it > 0) (totalWeeklyDistance / it) * 100f else 0f
                } ?: 0f

                Log.d(TAG, "Weekly progress calculated: Steps=${_weeklyStepsProgress.value}%, Duration=${_weeklyDurationProgress.value}%, Distance=${_weeklyDistanceProgress.value}%")

            } else {
                Log.e(TAG, "Error fetching user posts for weekly progress: ${userPostsResult.exceptionOrNull()?.message}")
                // Reset progress if there's an error fetching posts
                _weeklyStepsProgress.value = 0f
                _weeklyDurationProgress.value = 0f
                _weeklyDistanceProgress.value = 0f
            }
        }
    }

    /**
     * Estimates steps based on distance. This is a placeholder.
     * Replace with actual step data from your RunPost if available.
     * Assumes roughly 1.3 steps per meter.
     */
    private fun estimateStepsFromDistance(distanceInMeters: Float): Int {
        return (distanceInMeters * 1.3).toInt()
    }

    /**
     * Returns the timestamp (in milliseconds) for the start of the current week (Monday 00:00:00).
     */
    private fun getStartOfWeekTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY // Set Monday as the first day of the week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // Set to Monday of the current week
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }


    /**
     * Triggers the recalculation and saving of totalDistanceRun for the current user.
     * This function should only be called by authorized users/logic.
     */
    fun recalculateDistances() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "User not logged in. Cannot recalculate distances."
            Log.w(TAG, "recalculateDistances called but currentUserId is null.")
            return
        }

        // only allow specific user to run this for now (consider making this dynamic for admin roles)
        if (userId != "2MKIn3Un7HevvAAROb4z3cWaxFy2") { // IMPORTANT: Replace with actual admin ID or proper authorization
            _errorMessage.value = "You do not have permission to perform this action."
            Log.w(TAG, "Unauthorized user $userId attempted to recalculate distances.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            Log.d(TAG, "Attempting to recalculate distances for user: $userId")
            val result = userRepository.recalculateAndSaveTotalDistanceRun(userId)
            result.onSuccess {
                Log.d(TAG, "Total distance recalculated successfully for user $userId.")
                _errorMessage.value = "Uspješno preračunata ukupna udaljenost."
                fetchUserProfileAndCounts() // Refresh the UI to show the new value and recalculate weekly progress
            }.onFailure { e ->
                _errorMessage.value = "Greška pri preračunavanju udaljenosti: ${e.message}"
                Log.e(TAG, "Failed to recalculate totalDistanceRun for user $userId: ${e.message}", e)
            }
            _isLoading.value = false
        }
    }

    /**
     * Sets the weekly goals for the current user in Firestore.
     * It updates the user's document with the new goals and a timestamp.
     *
     * @param steps The target number of steps for the week. Null if not set.
     * @param durationMinutes The target duration of running in minutes for the week. Null if not set.
     * @param distanceKm The target distance of running in kilometers for the week. Null if not set.
     */
    fun setWeeklyGoals(steps: Int?, durationMinutes: Int?, distanceKm: Float?) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "User not logged in. Cannot set weekly goals."
            Log.w(TAG, "setWeeklyGoals called but currentUserId is null.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // Convert duration to milliseconds and distance to meters for storage
                val durationMillis = durationMinutes?.let { TimeUnit.MINUTES.toMillis(it.toLong()) }
                val distanceMeters = distanceKm?.let { it * 1000f } // Convert km to meters

                val result = userRepository.updateWeeklyGoals(
                    userId,
                    steps,
                    durationMillis,
                    distanceMeters
                )
                result.onSuccess {
                    Log.d(TAG, "Weekly goals set successfully for user $userId. Steps: $steps, Duration: $durationMinutes min, Distance: $distanceKm km")
                    _errorMessage.value = "Tjedni ciljevi uspješno postavljeni."
                    fetchUserProfileAndCounts() // Refresh profile and recalculate progress with new goals
                }.onFailure { e ->
                    _errorMessage.value = "Greška pri postavljanju tjednih ciljeva: ${e.message}"
                    Log.e(TAG, "Failed to set weekly goals for user $userId: ${e.message}", e)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while setting weekly goals."
                Log.e(TAG, "Unexpected error in setWeeklyGoals: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Handles user logout. Resets all ViewModel state to initial values.
     */
    fun logout() {
        authRepository.logout()
        _currentUser.value = null
        _errorMessage.value = null
        _followingCount.value = 0
        _followersCount.value = 0
        _postCount.value = 0
        _weeklyStepsProgress.value = null
        _weeklyDurationProgress.value = null
        _weeklyDistanceProgress.value = null
        Log.d(TAG, "User logged out. ViewModel state cleared.")
    }

    /**
     * Clears the current error message.
     */
    fun clearMessages() {
        _errorMessage.value = null
    }

    /**
     * Factory for creating instances of ProfileViewModel.
     * This allows for dependency injection of repositories and Firebase components.
     */
    class Factory(
        private val userRepository: UserRepository,
        private val firebaseAuth: FirebaseAuth,
        private val authRepository: AuthRepository,
        private val runPostRepository: RunPostRepository,
        private val followRepository: FollowRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(
                    userRepository,
                    firebaseAuth,
                    authRepository,
                    runPostRepository,
                    followRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
