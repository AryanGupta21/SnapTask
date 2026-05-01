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
import java.util.concurrent.TimeUnit

interface OpenClawApi {
    @POST("process")
    suspend fun process(@Body request: SnapTaskRequest): SnapTaskResponse
}

class OpenClawClient(private val api: OpenClawApi) {

    suspend fun process(rawText: String, entities: List<ExtractedEntity>): SnapTaskResponse =
        api.process(SnapTaskRequest(rawText = rawText, entities = entities))

    companion object {
        // Emulator: use 10.0.2.2 (host machine alias). Real device: use MacBook's LAN IP.
        private const val BASE_URL = "http://10.0.2.2:3000/"

        fun create(): OpenClawClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val http = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
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
