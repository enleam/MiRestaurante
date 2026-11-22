package com.example.mirestaurante.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.PATCH

interface ApiService {
    //Sprint 1: Inicio de sesion
    @POST("api/auth/login")
    suspend fun iniciarSesion(
        @Body datos: LoginRequest
    ): Response<LoginResponse>

    //HU03: Consultar todas las mesas activas
    @GET("api/mesas")
    suspend fun listarMesas(
        @Header("Authorization") token: String
    ): Response<MesasResponse>

    //HU03: Consultar una mesa por su ID
    @GET("api/mesas/{mesaId}")
    suspend fun obtenerMesaPorId(
        @Path("mesaId") mesaId: Int,
        @Header("Authorization") token: String
    ): Response<MesaResponse>

    //HU04: Iniciar atencion de una mesa
    @POST("api/atenciones")
    suspend fun iniciarAtencion(
        @Header("Authorization") token: String,
        @Body datos: IniciarAtencionRequest
    ): Response<AtencionResponse>

    //HU05: Consultar categorias
    @GET("api/catalogo/categorias")
    suspend fun listarCategorias(
        @Header("Authorization") token: String
    ): Response<CategoriasResponse>

    //HU05: Consultar productos
    @GET("api/catalogo/productos")
    suspend fun listarProductos(
        @Header("Authorization") token: String
    ): Response<ProductosResponse>

    //HU06: Consultar reservas registradas
    @GET("api/reservas")
    suspend fun listarReservas(
        @Header("Authorization") token: String
    ): Response<ReservasResponse>

    //HU07: Registrar un pedido con productos y cantidades
    @POST("api/pedidos")
    suspend fun registrarPedido(
        @Header("Authorization") token: String,
        @Body datos: RegistrarPedidoRequest
    ): Response<RegistrarPedidoResponse>

    //HU07: Consultar la atencion abierta de una mesa
    @GET("api/atenciones/mesa/{mesaId}/abierta")
    suspend fun obtenerAtencionAbierta(
        @Path("mesaId") mesaId: Int,
        @Header("Authorization") token: String
    ): Response<AtencionAbiertaResponse>

    @GET("api/pedidos/atencion/{atencionId}")
    suspend fun listarPedidosPorAtencion(
        @Path("atencionId") atencionId: Int,
        @Header("Authorization") token: String
    ): Response<ConsultarPedidosResponse>

    // Modificar productos y cantidades de un pedido
    @PUT("api/pedidos/{pedidoId}")
    suspend fun modificarPedido(
        @Path("pedidoId") pedidoId: Int,
        @Header("Authorization") token: String,
        @Body datos: ModificarPedidoRequest
    ): Response<ModificarPedidoResponse>

    //HU09: Enviar pedido a cocina
    @PATCH ("api/pedidos/{pedidoId}/enviar")
    suspend fun enviarPedidoACocina(
        @Path("pedidoId") pedidoId: Int,
        @Header("Authorization") token: String
    ): Response<EnviarPedidoResponse>

    //Consultar pedidos enviados a cocina
    @GET("api/pedidos/cocina")
    suspend fun listarPedidosCocina(
        @Header("Authorization") token: String
    ): Response<ConsultarPedidosCocinaResponse>

    @PATCH("api/pedidos/{pedidoId}/estado")
    suspend fun actualizarEstadoPedidoCocina(
        @Path("pedidoId") pedidoId: Int,
        @Header("Authorization") token: String,
        @Body datos: ActualizarEstadoPedidoRequest
    ): Response<ActualizarEstadoPedidoResponse>
}