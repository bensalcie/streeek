package com.bizilabs.streeek.feature.tabs.screens.leaderboard

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.bizilabs.streeek.lib.design.components.DialogState
import com.bizilabs.streeek.lib.domain.helpers.DataResult
import com.bizilabs.streeek.lib.domain.models.AccountDomain
import com.bizilabs.streeek.lib.domain.models.LeaderboardAccountDomain
import com.bizilabs.streeek.lib.domain.models.LeaderboardDomain
import com.bizilabs.streeek.lib.domain.repositories.AccountRepository
import com.bizilabs.streeek.lib.domain.repositories.LeaderboardRepository
import com.bizilabs.streeek.lib.domain.repositories.TauntRepository
import com.bizilabs.streeek.lib.domain.repositories.WorkerType
import com.bizilabs.streeek.lib.domain.repositories.WorkersRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.dsl.module
import timber.log.Timber
import kotlin.collections.get

internal val LeaderboardModule =
    module {
        factory<LeaderboardListScreenModel> {
            LeaderboardListScreenModel(
                accountRepository = get(),
                leaderboardRepository = get(),
                tauntRepository = get(),
                workersRepository = get(),
            )
        }
    }

data class LeaderboardListScreenState(
    val isSyncing: Boolean = false,
    val leaderboardName: String? = null,
    val leaderboard: LeaderboardDomain? = null,
    val leaderboards: List<LeaderboardDomain> = emptyList(),
    val showConfetti: Boolean = false,
    val account: AccountDomain? = null,
    val dialogState: DialogState? = null,
) {
    val list: List<LeaderboardAccountDomain>
        get() =
            when {
                leaderboard == null -> emptyList()
                leaderboard.page == 1 -> leaderboard.list.filterIndexed { index, _ -> index > 2 }
                else -> leaderboard.list
            }
}

class LeaderboardListScreenModel(
    private val accountRepository: AccountRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val tauntRepository: TauntRepository,
    private val workersRepository: WorkersRepository,
) : StateScreenModel<LeaderboardListScreenState>(LeaderboardListScreenState()) {
    private val selectedLeaderboard =
        combine(
            leaderboardRepository.selectedLeaderBoardId,
            leaderboardRepository.leaderboards,
        ) { id, map ->
            map[id]
        }

    init {
        observeAccount()
        observeLeaderboards()
        observeSyncingLeaderboards()
        observeSelectedLeaderboard()
    }

    private fun observeAccount() {
        screenModelScope.launch {
            accountRepository.account.collectLatest { value ->
                mutableState.update { it.copy(account = value) }
            }
        }
    }

    private fun observeSelectedLeaderboard() {
        screenModelScope.launch {
            selectedLeaderboard.collectLatest { value ->
                mutableState.update { it.copy(leaderboard = value) }
                Timber.d("Top -> ${value?.top}")
                val position = value?.rank?.current?.position ?: Long.MAX_VALUE
                val points = value?.rank?.current?.points ?: 0
                if (position <= 10L && points > 0) showConfetti()
            }
        }
    }

    private fun observeSyncingLeaderboards() {
        screenModelScope.launch {
            leaderboardRepository.syncing.collectLatest { value ->
                mutableState.update { it.copy(isSyncing = value) }
            }
        }
    }

    private fun observeLeaderboards() {
        screenModelScope.launch {
            leaderboardRepository.leaderboards.collectLatest { map ->
                mutableState.update { it.copy(leaderboards = map.map { (_, value) -> value }) }
            }
        }
    }

    private fun showConfetti() {
        screenModelScope.launch {
            mutableState.update { it.copy(showConfetti = true) }
            delay(2000)
            mutableState.update { it.copy(showConfetti = false) }
        }
    }

    fun onValueChangeLeaderboard(leaderboard: LeaderboardDomain) {
        screenModelScope.launch {
            leaderboardRepository.set(leaderboard = leaderboard)
        }
    }

    fun onClickViewMore() {
        screenModelScope.launch {
            mutableState.update { it.copy(leaderboardName = mutableState.value.leaderboard?.name) }
            delay(250)
            mutableState.update { it.copy(leaderboardName = null) }
        }
    }

    fun onTriggerRefreshLeaderboards() {
        workersRepository.runSyncLeaderboard(type = WorkerType.Once)
    }

    fun onClickDismissDialog() {
        mutableState.update { it.copy(dialogState = null) }
    }

    fun onClickMember(
        teamMemberPoints: Long,
        teamMemberId: Long,
        teamMemberName: String,
    ) {
        mutableState.update { it.copy(dialogState = DialogState.Loading()) }
        screenModelScope.launch {
            val memberPoints =
                selectedLeaderboard.first()?.list?.find { it.account.id == state.value.account?.id }?.rank?.points
                    ?: 0L

            if (teamMemberId == (state.value.account?.id ?: "") ||
                teamMemberPoints > memberPoints
            ) {
                mutableState.update {
                    it.copy(
                        dialogState =
                            DialogState.Error(
                                title = "Oops",
                                message = "You can only taunt members below you",
                            ),
                    )
                }
            } else {
                when (val result = tauntRepository.taunt(teamMemberId.toString())) {
                    is DataResult.Success -> {
                        mutableState.update {
                            it.copy(
                                dialogState =
                                    DialogState.Success(
                                        title = "Success",
                                        message = "Taunt delivered to $teamMemberName",
                                    ),
                            )
                        }
                    }

                    is DataResult.Error -> {
                        mutableState.update {
                            it.copy(
                                dialogState =
                                    DialogState.Error(
                                        title = "Error",
                                        message = result.message,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
