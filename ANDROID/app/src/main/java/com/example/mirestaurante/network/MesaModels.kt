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