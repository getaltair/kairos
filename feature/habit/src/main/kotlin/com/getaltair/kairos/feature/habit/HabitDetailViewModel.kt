package com.getaltair.kairos.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.usecase.ArchiveHabitUseCase
import com.getaltair.kairos.domain.usecase.BackdateCompletionUseCase
import com.getaltair.kairos.domain.usecase.DeleteHabitUseCase
import com.getaltair.kairos.domain.usecase.GetHabitDetailUseCase
import com.getaltair.kairos.domain.usecase.PauseHabitUseCase
import com.getaltair.kairos.domain.usecase.RestoreHabitUseCase
import com.getaltair.kairos.domain.usecase.ResumeHabitUseCase
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class HabitDetailViewModel(
    private val getHabitDetailUseCase: GetHabitDetailUseCase,
    private val pauseHabitUseCase: PauseHabitUseCase,
    private val resumeHabitUseCase: ResumeHabitUseCase,
    private val archiveHabitUseCase: ArchiveHabitUseCase,
    private val restoreHabitUseCase: RestoreHabitUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase,
    private val backdateCompletionUseCase: BackdateCompletionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    private var habitId: UUID? = null

    fun loadHabit(id: UUID) {
        habitId = id
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = getHabitDetailUseCase(id)) {
                    is Result.Success -> {
                        val detail = result.value
                        _uiState.update {
                            it.copy(
                                habit = detail.habit,
                                recentCompletions = detail.recentCompletions,
                                weeklyCompletionRate = detail.weeklyCompletionRate,
                                isLoading = false,
                                error = null
                            )
                        }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "loadHabit failed: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = ErrorMapper.toUserMessage(result.message)
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error loading habit detail")
                _uiState.update {
                    it.copy(isLoading = false, error = "Something went wrong. Please try again.")
                }
            }
        }
    }

    fun onPause() {
        val id = habitId ?: return
        executeAction { pauseHabitUseCase(id) }
    }

    fun onResume() {
        val id = habitId ?: return
        executeAction { resumeHabitUseCase(id) }
    }

    fun onArchive() {
        val id = habitId ?: return
        executeAction { archiveHabitUseCase(id) }
    }

    fun onRestore() {
        val id = habitId ?: return
        executeAction { restoreHabitUseCase(id) }
    }

    fun onDeleteRequested() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun onDeleteConfirmed() {
        val id = habitId ?: return
        _uiState.update { it.copy(showDeleteConfirmation = false) }
        viewModelScope.launch {
            try {
                when (val result = deleteHabitUseCase(id)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isDeleted = true) }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Delete failed: %s", result.message)
                        _uiState.update {
                            it.copy(actionResult = ErrorMapper.toUserMessage(result.message))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error deleting habit")
                _uiState.update {
                    it.copy(actionResult = "Something went wrong. Please try again.")
                }
            }
        }
    }

    fun onBackdate(date: LocalDate, type: CompletionType, partialPercent: Int? = null) {
        val id = habitId ?: return
        viewModelScope.launch {
            try {
                when (val result = backdateCompletionUseCase(id, date, type, partialPercent)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(actionResult = "Completion added") }
                        loadHabit(id)
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Backdate failed: %s", result.message)
                        _uiState.update {
                            it.copy(actionResult = ErrorMapper.toUserMessage(result.message))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error backdating completion")
                _uiState.update {
                    it.copy(actionResult = "Something went wrong. Please try again.")
                }
            }
        }
    }

    fun clearActionResult() {
        _uiState.update { it.copy(actionResult = null) }
    }

    private fun executeAction(action: suspend () -> Result<*>) {
        val id = habitId ?: return
        viewModelScope.launch {
            try {
                when (val result = action()) {
                    is Result.Success -> {
                        loadHabit(id)
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Action failed: %s", result.message)
                        _uiState.update {
                            it.copy(actionResult = ErrorMapper.toUserMessage(result.message))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error executing action")
                _uiState.update {
                    it.copy(actionResult = "Something went wrong. Please try again.")
                }
            }
        }
    }
}
