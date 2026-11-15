package com.example.mirestaurante.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // Inicio de sesion (Sprint 1)
    @POST("api/auth/login")
    suspend fun iniciarSesion(
        @Body datos: LoginRequest
    ): Response<LoginResponse>

    // Obtener todas las mesas activas (HU03)
    @GET("api/mesas")
    suspend fun listarMesas(
        @Header("Authorization") token: String
    ): Response<MesasResponse>

    // Obtener una mesa por su ID (HU03)
    @GET("api/mesas/{mesaId}")
    suspend fun obtenerMesaPorId(
        @Path("mesaId") mesaId: Int,
        @Header("Authorization") token: String
    ): Response<MesaResponse>

    // Iniciar la atencion de una mesa (HU04)
    @POST("api/atenciones")
    suspend fun iniciarAtencion(
        @Header("Authorization") token: String,
        @Body datos: IniciarAtencionRequest
    ): Response<AtencionResponse>

    // Obtener las categorias activas (HU05)
    @GET("api/catalogo/categorias")
    suspend fun listarCategorias(
        @Header("Authorization") token: String
    ): Response<CategoriasResponse>

    // Obtener los productos de la carta (HU05)
    @GET("api/catalogo/productos")
    suspend fun listarProductos(
        @Header("Authorization") token: String
    ): Response<ProductosResponse>
}