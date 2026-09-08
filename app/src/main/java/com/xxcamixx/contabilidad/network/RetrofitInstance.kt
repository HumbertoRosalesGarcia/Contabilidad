package com.xxcamixx.contabilidad.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api: ServerApi by lazy { Retrofit.Builder().baseUrl("http://158.247.123.136:3000/").addConverterFactory(GsonConverterFactory.create()).build().create(ServerApi::class.java) }
}
