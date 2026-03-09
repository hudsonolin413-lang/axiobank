package org.dals.project.viewmodel

import org.dals.project.repository.*

class OfflineViewModel(
    val transactionRepository: TransactionRepository,
    val cardRepository: CardRepository,
    val loanRepository: LoanRepository,
    val settingsRepository: SettingsRepository,
    val authRepository: AuthRepository
) {
    fun syncOfflineData() {
        // Placeholder sync logic
    }
}
