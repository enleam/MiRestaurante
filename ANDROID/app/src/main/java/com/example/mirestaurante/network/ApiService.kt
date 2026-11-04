package com.example.mirestaurante.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

//Rutas de nuestra API REST
interface ApiService {
    // Enviar los datos de inicio de sesión
    @POST("api/auth/login")
    suspend fun iniciarSesion(
        @Body datos: LoginRequest
    ): Response<LoginResponse>
}