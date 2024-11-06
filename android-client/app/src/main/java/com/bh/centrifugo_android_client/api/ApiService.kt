package com.bh.centrifugo_android_client.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

data class TokenResponse(
    val token: String
)

interface ApiService {
    @GET("token")
    fun getToken(@Query("user_id") userId: String): Call<TokenResponse>
}
