package com.sju18001.petmanagement.restapi.fcm

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FcmRetrofitBuilder {
    companion object {
        private const val BASE_URL = "https://fcm.googleapis.com"
        private const val FCM_KEY = "AAAAkUo9Nwo:APA91bEQgeWJjwVsJPE2LJOkg9S4YFZG2GGBFfQUh221WKthLB3aetVjimJmhmXmXJp8E6Ug3E4yTbjMIpk75h-e23672WvG7hTpYfc_aqxvgzksFQ2_dc6oawtjQZCscVF0-hQlGVmK"

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