package com.example.tk4_mobileprogramming.api

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
)

data class SurveyResponse(
    val id: Long,
    @SerializedName("surveyor_email")
    val surveyorEmail: String,
    val name: String,
    val age: Int,
    val address: String,
    val symptoms: String,
    val latitude: Double?,
    val longitude: Double?
)

data class SurveyRequest(
    val id: Long? = null,
    @SerializedName("surveyor_email")
    val surveyorEmail: String,
    val name: String,
    val age: Int,
    val address: String,
    val symptoms: String,
    val latitude: Double?,
    val longitude: Double?
)

data class DeleteRequest(
    val id: Long,
    @SerializedName("surveyor_email")
    val surveyorEmail: String
)

data class DeleteResponse(
    val message: String
)
