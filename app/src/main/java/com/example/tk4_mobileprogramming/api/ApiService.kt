package com.example.tk4_mobileprogramming.api

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("surveys.php")
    suspend fun getAllSurveysByEmail(
        @Query("surveyor_email") email: String
    ): ApiResponse<List<SurveyResponse>>

    @GET("surveys.php")
    suspend fun getSurvey(
        @Query("id") id: Long,
        @Query("surveyor_email") email: String
    ): ApiResponse<SurveyResponse>

    @POST("surveys.php")
    suspend fun addSurvey(
        @Body survey: SurveyRequest
    ): ApiResponse<SurveyResponse>

    @PUT("surveys.php")
    suspend fun updateSurvey(
        @Body survey: SurveyRequest
    ): ApiResponse<SurveyResponse>

    @HTTP(method = "DELETE", path = "surveys.php", hasBody = true)
    suspend fun deleteSurvey(
        @Body deleteRequest: DeleteRequest
    ): ApiResponse<DeleteResponse>
}
