package com.example.tk4_mobileprogramming

import com.example.tk4_mobileprogramming.api.*
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class DatabaseHelper(context: Context) {
    private val apiService: ApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://tkmobileprogramming.xyz/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    fun addSurvey(survey: Survey): Long {
        return runBlocking(Dispatchers.IO) {
            try {
                val request = SurveyRequest(
                    surveyorEmail = survey.surveyorEmail ?: throw IllegalArgumentException("Surveyor email is required"),
                    name = survey.name,
                    age = survey.age,
                    address = survey.address,
                    symptoms = survey.symptoms,
                    latitude = survey.latitude,
                    longitude = survey.longitude
                )

                val response = apiService.addSurvey(request)
                response.data?.id ?: throw Exception("Failed to get ID of created survey")
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun getAllSurveysByEmail(email: String): List<Survey> {
        return runBlocking(Dispatchers.IO) {
            try {
                val response = apiService.getAllSurveysByEmail(email)
                response.data?.map { it.toSurvey() } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun getSurvey(id: Long, email: String): Survey {
        return runBlocking(Dispatchers.IO) {
            try {
                val response = apiService.getSurvey(id = id, email = email)
                response.data?.toSurvey()
                    ?: throw IllegalArgumentException("Survey with id $id not found")
            } catch (e: Exception) {
                throw IllegalArgumentException("Survey with id $id not found")
            }
        }
    }

    fun updateSurvey(survey: Survey): Int {
        return runBlocking(Dispatchers.IO) {
            try {
                val request = SurveyRequest(
                    id = survey.id,
                    surveyorEmail = survey.surveyorEmail ?: throw IllegalArgumentException("Surveyor email is required"),
                    name = survey.name,
                    age = survey.age,
                    address = survey.address,
                    symptoms = survey.symptoms,
                    latitude = survey.latitude,
                    longitude = survey.longitude
                )

                val response = apiService.updateSurvey(request)
                if (response.success) 1 else 0
            } catch (e: Exception) {
                0
            }
        }
    }

    fun deleteSurveyByIdAndEmail(id: Long, email: String): Int {
        return runBlocking(Dispatchers.IO) {
            try {
                val request = DeleteRequest(id = id, surveyorEmail = email)
                val response = apiService.deleteSurvey(request)
                if (response.success) 1 else 0
            } catch (e: Exception) {
                0
            }
        }
    }

    private fun SurveyResponse.toSurvey(): Survey {
        return Survey(
            id = id,
            name = name,
            age = age,
            address = address,
            symptoms = symptoms,
            latitude = latitude,
            longitude = longitude,
            surveyorEmail = surveyorEmail
        )
    }
}