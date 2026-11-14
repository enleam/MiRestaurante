package com.example.mirestaurante.network

import android.R
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

//Rutas de nuestra API REST
interface ApiService {
    // Enviar los datos de inicio de sesión
    @POST("api/auth/login")
    suspend fun iniciarSesion(
        @Body datos: LoginRequest
    ): Response<LoginResponse>

    //Obtener todas las mesas activas
    @GET("api/mesas")
    suspend fun listarMesas(
        @Header("Authorization") token: String
    ): Response<MesasResponse>

    //Obtener una mesa por su ID
    @GET("api/mesas/{mesaId}")
    suspend fun obtenerMesaporId(
        @Path("mesaId") mesaId:Int,
        @Header("Authorization") token: String
    ): Response<MesaResponse>

    //HU04: iniciar la atencion de una mesa libre
    @POST("api/mesas/{mesaId}/atenciones")
    suspend fun iniciarAtencion(
        @Path("mesaId") mesaId: Int,
        @Header("Authorization") token: String
    ): Response<IniciarAtencionResponse>
}
