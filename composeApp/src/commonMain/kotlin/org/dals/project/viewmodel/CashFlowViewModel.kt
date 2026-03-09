package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.model.*
import org.dals.project.repository.CashFlowRepository
import org.dals.project.repository.TransactionRepository

class CashFlowViewModel(
    private val cashFlowRepository: CashFlowRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val cashFlowSummary: StateFlow<CashFlowSummary?> = cashFlowRepository.cashFlowSummary
    val monthlyTrends: StateFlow<List<MonthlyTrend>> = cashFlowRepository.monthlyTrends
    val insights: StateFlow<List<CashFlowInsight>> = cashFlowRepository.insights
    val periodComparison: StateFlow<PeriodComparison?> = cashFlowRepository.periodComparison

    private val _selectedPeriod = MutableStateFlow(CashFlowRepository.AnalysisPeriod.MONTH_30)
    val selectedPeriod: StateFlow<CashFlowRepository.AnalysisPeriod> = _selectedPeriod.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Initial analysis
        analyzeCashFlow()

        // Listen to transaction changes
        viewModelScope.launch {
            transactionRepository.transactions.collect {
                if (it.isNotEmpty()) {
                    analyzeCashFlow()
                }
            }
        }
    }

    fun selectPeriod(period: CashFlowRepository.AnalysisPeriod) {
        _selectedPeriod.value = period
        analyzeCashFlow()
    }

    fun analyzeCashFlow() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cashFlowRepository.analyzeCashFlow(_selectedPeriod.value)
            } catch (e: Exception) {
                println("❌ Error in CashFlowViewModel: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                transactionRepository.refreshData()
                analyzeCashFlow()
            } catch (e: Exception) {
                println("❌ Error refreshing cash flow data: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
