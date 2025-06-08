package com.example.run_app_rma.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.dao.ChallengeDao
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val runPostRepository: RunPostRepository,
    private val followRepository: FollowRepository,
    private val challengeDao: ChallengeDao
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

    // --- MERGED STATE FLOWS ---
    // From your branch for titles
    private val _unlockedTitles = MutableStateFlow<List<String>>(emptyList())
    val unlockedTitles: StateFlow<List<String>> = _unlockedTitles.asStateFlow()
    // From main branch for weekly goals
    private val _weeklyStepsProgress = MutableStateFlow<Float?>(null)
    val weeklyStepsProgress: StateFlow<Float?> = _weeklyStepsProgress.asStateFlow()
    private val _weeklyDurationProgress = MutableStateFlow<Float?>(null)
    val weeklyDurationProgress: StateFlow<Float?> = _weeklyDurationProgress.asStateFlow()
    private val _weeklyDistanceProgress = MutableStateFlow<Float?>(null)
    val weeklyDistanceProgress: StateFlow<Float?> = _weeklyDistanceProgress.asStateFlow()
    // --- END MERGED STATE FLOWS ---

    private val TAG = "ProfileViewModel"

    init {
        // Fetch user profile and counts when the ViewModel is initialized
        fetchUserProfileAndCounts()
        observeUnlockedTitles() // From your branch
    }

    private fun observeUnlockedTitles() {
        challengeDao.getUnlockedTitles()
            .onEach { titles ->
                _unlockedTitles.value = titles
            }
            .launchIn(viewModelScope)
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
            _weeklyStepsProgress.value = null // From main
            _weeklyDurationProgress.value = null // From main
            _weeklyDistanceProgress.value = null // From main
            _unlockedTitles.value = emptyList() // From your branch
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
                        calculateWeeklyProgress(user) // From main
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

            if (weeklyGoalSteps == null && weeklyGoalDuration == null && weeklyGoalDistance == null) {
                _weeklyStepsProgress.value = null
                _weeklyDurationProgress.value = null
                _weeklyDistanceProgress.value = null
                Log.d(TAG, "No weekly goals set for user $userId.")
                return@launch
            }

            val currentWeekStart = getStartOfWeekTimestamp()

            if (goalsSetTimestamp != null && goalsSetTimestamp < currentWeekStart) {
                _weeklyStepsProgress.value = 0f
                _weeklyDurationProgress.value = 0f
                _weeklyDistanceProgress.value = 0f
                Log.d(TAG, "Weekly goals set before current week, resetting progress for display.")
                return@launch
            }

            val userPostsResult = runPostRepository.getRunPostsByUsers(listOf(userId))
            if (userPostsResult.isSuccess) {
                val weeklyRuns = userPostsResult.getOrNull()?.filter { runPost: RunPost ->
                    runPost.timestamp != null && runPost.timestamp.time >= currentWeekStart
                } ?: emptyList()

                var totalWeeklyDistance = 0f
                var totalWeeklyDuration = 0L
                var totalWeeklySteps = 0

                weeklyRuns.forEach { runPost ->
                    totalWeeklyDistance += runPost.distance
                    totalWeeklyDuration += (runPost.endTime - runPost.startTime)
                    totalWeeklySteps += estimateStepsFromDistance(runPost.distance)
                }

                _weeklyStepsProgress.value = weeklyGoalSteps?.let {
                    if (it > 0) (totalWeeklySteps.toFloat() / it) * 100f else 0f
                } ?: 0f

                _weeklyDurationProgress.value = weeklyGoalDuration?.let {
                    if (it > 0) (totalWeeklyDuration.toFloat() / it) * 100f else 0f
                } ?: 0f

                _weeklyDistanceProgress.value = weeklyGoalDistance?.let {
                    if (it > 0) (totalWeeklyDistance / it) * 100f else 0f
                } ?: 0f

                Log.d(TAG, "Weekly progress calculated: Steps=${_weeklyStepsProgress.value}%, Duration=${_weeklyDurationProgress.value}%, Distance=${_weeklyDistanceProgress.value}%")

            } else {
                Log.e(TAG, "Error fetching user posts for weekly progress: ${userPostsResult.exceptionOrNull()?.message}")
                _weeklyStepsProgress.value = 0f
                _weeklyDurationProgress.value = 0f
                _weeklyDistanceProgress.value = 0f
            }
        }
    }

    private fun estimateStepsFromDistance(distanceInMeters: Float): Int {
        return (distanceInMeters * 1.3).toInt()
    }

    private fun getStartOfWeekTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun recalculateDistances() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "User not logged in. Cannot recalculate distances."
            Log.w(TAG, "recalculateDistances called but currentUserId is null.")
            return
        }

        if (userId != "2MKIn3Un7HevvAAROb4z3cWaxFy2") {
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
                fetchUserProfileAndCounts()
            }.onFailure { e ->
                _errorMessage.value = "Greška pri preračunavanju udaljenosti: ${e.message}"
                Log.e(TAG, "Failed to recalculate totalDistanceRun for user $userId: ${e.message}", e)
            }
            _isLoading.value = false
        }
    }

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
                val durationMillis = durationMinutes?.let { TimeUnit.MINUTES.toMillis(it.toLong()) }
                val distanceMeters = distanceKm?.let { it * 1000f }

                val result = userRepository.updateWeeklyGoals(
                    userId,
                    steps,
                    durationMillis,
                    distanceMeters
                )
                result.onSuccess {
                    Log.d(TAG, "Weekly goals set successfully for user $userId. Steps: $steps, Duration: $durationMinutes min, Distance: $distanceKm km")
                    _errorMessage.value = "Tjedni ciljevi uspješno postavljeni."
                    fetchUserProfileAndCounts()
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
        // --- MERGED LOGOUT LOGIC ---
        _unlockedTitles.value = emptyList() // From your branch
        _weeklyStepsProgress.value = null // From main
        _weeklyDurationProgress.value = null // From main
        _weeklyDistanceProgress.value = null // From main
        Log.d(TAG, "User logged out. ViewModel state cleared.")
        // --- END MERGED LOGOUT LOGIC ---
    }

    fun clearMessages() {
        _errorMessage.value = null
    }

    class Factory(
        private val userRepository: UserRepository,
        private val firebaseAuth: FirebaseAuth,
        private val authRepository: AuthRepository,
        private val runPostRepository: RunPostRepository,
        private val followRepository: FollowRepository,
        private val challengeDao: ChallengeDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(
                    userRepository,
                    firebaseAuth,
                    authRepository,
                    runPostRepository,
                    followRepository,
                    challengeDao
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}