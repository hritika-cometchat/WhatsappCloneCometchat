package com.example.testapplication

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://${AppConstants.APP_ID}.api-in.cometchat.io/"
//    private const val BASE_URL = "https://${AppConstants.APP_ID}.call-${AppConstants.REGION}.cometchat.io/"


    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("accept", "application/json")
            .addHeader("content-type", "application/json")
            .addHeader("apiKey", AppConstants.API_KEY)
            .build()
        chain.proceed(request)
    }
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun getInstance(): Retrofit {
        val client = OkHttpClient()
        val clientBuilder: OkHttpClient.Builder = client.newBuilder()
                .addInterceptor(headerInterceptor)
                .addInterceptor(loggingInterceptor)

        return Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(clientBuilder.build())
            .build()
    }
}