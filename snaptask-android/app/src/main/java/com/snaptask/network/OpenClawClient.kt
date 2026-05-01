package com.snaptask.network

import com.snaptask.network.models.ExtractedEntity
import com.snaptask.network.models.SnapTaskRequest
import com.snaptask.network.models.SnapTaskResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenClawApi {
    @POST("process")
    suspend fun process(@Body request: SnapTaskRequest): SnapTaskResponse
}

class OpenClawClient(private val api: OpenClawApi) {

    suspend fun process(rawText: String, entities: List<ExtractedEntity>): SnapTaskResponse =
        api.process(SnapTaskRequest(rawText = rawText, entities = entities))

    companion object {
        // Replace with your MacBook's local IP: ifconfig | grep inet
        private const val BASE_URL = "http://192.168.1.100:3000/"

        fun create(): OpenClawClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val http = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return OpenClawClient(retrofit.create(OpenClawApi::class.java))
        }
    }
}
