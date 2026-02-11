package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
enum class InvestmentType {
    STOCKS,
    BONDS,
    MUTUAL_FUNDS,
    ETF,
    CRYPTOCURRENCY,
    REAL_ESTATE,
    COMMODITIES,
    FIXED_DEPOSIT,
    MONEY_MARKET,
    OTHER
}
