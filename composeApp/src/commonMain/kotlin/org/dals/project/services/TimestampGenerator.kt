package org.dals.project.services

/**
 * Platform-specific timestamp generator for M-Pesa API
 * Format: YYYYMMDDHHmmss
 */
expect fun generateTimestamp(): String
