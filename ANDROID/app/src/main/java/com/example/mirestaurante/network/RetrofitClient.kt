package com.example.mirestaurante.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//Configuracion de la conexion con nuestra API REST
object RetrofitClient {
    //Direccion del backend para pruebas con ADB Reverse
    private const val BASE_URL = "http://127.0.0.1:3000/"

    //Crear la conexion con Retrofit
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}