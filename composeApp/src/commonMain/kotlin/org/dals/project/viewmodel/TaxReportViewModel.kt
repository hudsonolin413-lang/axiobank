package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dals.project.repository.*
import org.dals.project.API_BASE_URL

data class TaxReportUiState(
    val isLoading: Boolean = false,
    val reports: List<TaxReportRecord> = emptyList(),
    val taxSummary: TaxSummary? = null,
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val showGenerateDialog: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class TaxReportViewModel : ViewModel() {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }

    private val repository = TaxReportRepository(httpClient, API_BASE_URL)

    private val _uiState = MutableStateFlow(TaxReportUiState())
    val uiState: StateFlow<TaxReportUiState> = _uiState.asStateFlow()

    /**
     * Load initial data (available years and set current year as default)
     */
    fun loadInitialData(customerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load available years
                val yearsResult = repository.getAvailableYears(customerId)
                if (yearsResult.isSuccess) {
                    val years = yearsResult.getOrNull() ?: emptyList()
                    val currentYear = if (years.isNotEmpty()) years.first() else java.time.Year.now().value

                    _uiState.update {
                        it.copy(
                            availableYears = years,
                            selectedYear = currentYear,
                            isLoading = false
                        )
                    }

                    // Load reports and summary for current year
                    loadReportsForYear(customerId, currentYear)
                } else {
                    _uiState.update {
                        it.copy(
                            error = yearsResult.exceptionOrNull()?.message,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to load data: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Load all reports for a specific year
     */
    fun loadReportsForYear(customerId: String, year: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, selectedYear = year) }

            try {
                // Load reports
                val reportsResult = repository.getReportsByYear(customerId, year)

                // Load tax summary
                val summaryResult = repository.getTaxSummary(customerId, year)

                if (reportsResult.isSuccess && summaryResult.isSuccess) {
                    _uiState.update {
                        it.copy(
                            reports = reportsResult.getOrNull() ?: emptyList(),
                            taxSummary = summaryResult.getOrNull(),
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            error = reportsResult.exceptionOrNull()?.message
                                ?: summaryResult.exceptionOrNull()?.message,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to load reports: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Generate a new tax report
     */
    fun generateReport(customerId: String, year: Int, reportType: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

            val request = GenerateTaxReportRequest(
                customerId = customerId,
                year = year,
                reportType = reportType
            )

            val result = repository.generateReport(request)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Tax report generated successfully",
                        showGenerateDialog = false
                    )
                }
                // Reload reports for the year
                loadReportsForYear(customerId, year)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to generate report",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Delete a tax report
     */
    fun deleteReport(customerId: String, reportId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.deleteReport(reportId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Report deleted successfully"
                    )
                }
                // Reload reports
                _uiState.value.selectedYear?.let { year ->
                    loadReportsForYear(customerId, year)
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to delete report",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Show/hide generate dialog
     */
    fun setShowGenerateDialog(show: Boolean) {
        _uiState.update { it.copy(showGenerateDialog = show) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Refresh all data
     */
    fun refresh(customerId: String) {
        _uiState.value.selectedYear?.let { year ->
            loadReportsForYear(customerId, year)
        } ?: loadInitialData(customerId)
    }
}
