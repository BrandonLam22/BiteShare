package org.example.biteshare

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform