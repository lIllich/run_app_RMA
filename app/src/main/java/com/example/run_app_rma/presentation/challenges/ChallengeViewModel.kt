package com.example.run_app_rma.presentation.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.dao.ChallengeDao
import com.example.run_app_rma.domain.model.Challenge
import com.example.run_app_rma.domain.model.ChallengesRepository
import com.example.run_app_rma.domain.model.UserChallengeProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChallengeUiState(
    val challenge: Challenge,
    val progress: UserChallengeProgress
)

class ChallengeViewModel(
    private val challengeDao: ChallengeDao
) : ViewModel() {

    private val _challengeUiState = MutableStateFlow<List<ChallengeUiState>>(emptyList())
    val challengeUiState: StateFlow<List<ChallengeUiState>> = _challengeUiState.asStateFlow()

    init {
        loadChallenges()
    }

    private fun loadChallenges() {
        viewModelScope.launch {
            val allChallenges = ChallengesRepository.challenges
            challengeDao.getAllProgress().collectLatest { progressList ->
                val progressMap = progressList.associateBy { it.challengeId }
                val uiStateList = allChallenges.map { challenge ->
                    val progress = progressMap[challenge.id] ?: UserChallengeProgress(challenge.id)
                    ChallengeUiState(challenge, progress)
                }
                _challengeUiState.value = uiStateList
            }
        }
    }

    class Factory(
        private val challengeDao: ChallengeDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChallengeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChallengeViewModel(challengeDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 