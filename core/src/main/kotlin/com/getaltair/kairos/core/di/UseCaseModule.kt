package com.getaltair.kairos.core.di

import com.getaltair.kairos.core.usecase.CompleteHabitUseCase
import com.getaltair.kairos.core.usecase.GetTodayHabitsUseCase
import com.getaltair.kairos.core.usecase.SkipHabitUseCase
import com.getaltair.kairos.core.usecase.UndoCompletionUseCase
import com.getaltair.kairos.domain.usecase.AbandonRoutineUseCase
import com.getaltair.kairos.domain.usecase.AdvanceRoutineStepUseCase
import com.getaltair.kairos.domain.usecase.ArchiveHabitUseCase
import com.getaltair.kairos.domain.usecase.BackdateCompletionUseCase
import com.getaltair.kairos.domain.usecase.CompleteRecoverySessionUseCase
import com.getaltair.kairos.domain.usecase.CompleteRoutineUseCase
import com.getaltair.kairos.domain.usecase.CreateHabitUseCase
import com.getaltair.kairos.domain.usecase.CreateMissedCompletionsUseCase
import com.getaltair.kairos.domain.usecase.CreateRoutineUseCase
import com.getaltair.kairos.domain.usecase.DeleteAccountUseCase
import com.getaltair.kairos.domain.usecase.DeleteHabitUseCase
import com.getaltair.kairos.domain.usecase.DeleteRoutineUseCase
import com.getaltair.kairos.domain.usecase.DetectLapsesUseCase
import com.getaltair.kairos.domain.usecase.EditHabitUseCase
import com.getaltair.kairos.domain.usecase.GetActiveHabitsUseCase
import com.getaltair.kairos.domain.usecase.GetActiveRoutinesUseCase
import com.getaltair.kairos.domain.usecase.GetHabitDetailUseCase
import com.getaltair.kairos.domain.usecase.GetHabitUseCase
import com.getaltair.kairos.domain.usecase.GetPendingRecoveriesUseCase
import com.getaltair.kairos.domain.usecase.GetRoutineDetailUseCase
import com.getaltair.kairos.domain.usecase.GetRoutineExecutionUseCase
import com.getaltair.kairos.domain.usecase.GetWeeklyStatsUseCase
import com.getaltair.kairos.domain.usecase.ObserveAuthStateUseCase
import com.getaltair.kairos.domain.usecase.PauseHabitUseCase
import com.getaltair.kairos.domain.usecase.ResetPasswordUseCase
import com.getaltair.kairos.domain.usecase.RestoreHabitUseCase
import com.getaltair.kairos.domain.usecase.ResumeHabitUseCase
import com.getaltair.kairos.domain.usecase.SignInUseCase
import com.getaltair.kairos.domain.usecase.SignOutUseCase
import com.getaltair.kairos.domain.usecase.SignUpUseCase
import com.getaltair.kairos.domain.usecase.StartRoutineUseCase
import com.getaltair.kairos.domain.usecase.UpdateRoutineUseCase
import org.koin.dsl.module

/**
 * Koin module providing domain-layer use cases.
 * Each use case is a factory (new instance per injection).
 */
val useCaseModule = module {
    factory { GetTodayHabitsUseCase(get(), get()) }
    factory { CompleteHabitUseCase(get(), get()) }
    factory { SkipHabitUseCase(get(), get()) }
    factory { UndoCompletionUseCase(get()) }
    factory { CreateHabitUseCase(get()) }
    factory { GetWeeklyStatsUseCase(get(), get()) }
    factory { EditHabitUseCase(get()) }
    factory { PauseHabitUseCase(get()) }
    factory { ResumeHabitUseCase(get()) }
    factory { ArchiveHabitUseCase(get()) }
    factory { RestoreHabitUseCase(get()) }
    factory { DeleteHabitUseCase(get()) }
    factory { BackdateCompletionUseCase(get(), get()) }
    factory { GetHabitUseCase(get()) }
    factory { GetHabitDetailUseCase(get(), get()) }

    // Auth use cases
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }
    factory { ObserveAuthStateUseCase(get()) }
    factory { DeleteAccountUseCase(get(), get()) }

    // Recovery use cases
    factory { CreateMissedCompletionsUseCase(get(), get()) }
    factory { DetectLapsesUseCase(get(), get(), get()) }
    factory { CompleteRecoverySessionUseCase(get(), get()) }
    factory { GetPendingRecoveriesUseCase(get(), get()) }

    // Routine use cases
    factory { CreateRoutineUseCase(get()) }
    factory { UpdateRoutineUseCase(get()) }
    factory { DeleteRoutineUseCase(get()) }
    factory { GetRoutineDetailUseCase(get(), get()) }
    factory { GetActiveRoutinesUseCase(get()) }
    factory { StartRoutineUseCase(get(), get()) }
    factory { AdvanceRoutineStepUseCase(get(), get()) }
    factory { CompleteRoutineUseCase(get()) }
    factory { AbandonRoutineUseCase(get()) }
    factory { GetRoutineExecutionUseCase(get()) }
    factory { GetActiveHabitsUseCase(get()) }
}
