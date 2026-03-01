package org.example.biteshare.domain

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class User (
    val id: String = Random.nextInt(0, 1000000).toString(),
    val username: String,
    val email: String,
    val password: String,
)