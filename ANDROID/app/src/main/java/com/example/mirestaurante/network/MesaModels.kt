package com.example.mirestaurante.network

//datos de una mesa recibidos desde el backend
data class Mesa(
    val mesaId: Int,
    val numero: Int,
    val capacidad: Int,
    val estado: String
)

//respuesta al consultar todas las mesas
data class MesasResponse(
    val mensaje: String,
    val mesas: List<Mesa>
)

//respuesta al consultar una mesa por ID
data class MesaResponse(
    val mensaje: String,
    val mesa: Mesa
)

//HU04: atencion registrada al iniciar el servicio de una mesa
data class Atencion(
    val atencionId: Int,
    val mesaId: Int,
    val mozoId: Int,
    val fechaApertura: String,
    val estado: String
)

//HU04: respuesta al iniciar la atencion de una mesa
data class IniciarAtencionResponse(
    val mensaje: String,
    val atencion: Atencion,
    val mesa: Mesa
)