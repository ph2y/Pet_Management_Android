package com.sju18001.petmanagement.restapi.fcm

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FcmRetrofitBuilder {
    companion object {
        private const val BASE_URL = "https://fcm.googleapis.com"
        private const val FCM_KEY = "AAAAJIaMoyg:APA91bE_ck5C5RQbNX12lS0c6p7aUhbmDHs99Xc8c4s5O-s7PepwhCxxsII1t2muMm-ixjDjwC1DLhHVbjJx_jbQCXptVRwbtbn_IJrbfDV7Mq1VRI9a9jAS_MUvpcWxjirLroz7CCqE"

        private val retrofit by lazy {
            // 인터셉터 초기화
            val networkInterceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "key=$FCM_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build()
                val response = chain.proceed(newRequest)

                response.newBuilder().build()
            }

            // For Logging
            val hlt = HttpLoggingInterceptor()
            hlt.level = HttpLoggingInterceptor.Level.BODY

            val client = OkHttpClient.Builder()
                .addNetworkInterceptor(hlt)
                .addNetworkInterceptor(networkInterceptor)
                .build()

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        val api: FcmApi by lazy {
            retrofit.create(FcmApi::class.java)
        }
    }
}