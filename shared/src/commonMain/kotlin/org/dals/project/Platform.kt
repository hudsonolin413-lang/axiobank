package org.dals.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform