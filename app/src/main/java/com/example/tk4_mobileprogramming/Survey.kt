package com.example.tk4_mobileprogramming

data class Survey(
    val id: Long,
    val name: String,
    val age: Int,
    val address: String,
    val symptoms: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val surveyorEmail: String? = null
)
