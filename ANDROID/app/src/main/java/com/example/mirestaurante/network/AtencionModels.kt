package com.example.mirestaurante.network

//Datos enviados para iniciar una atencion
data class IniciarAtencionRequest(
    val mesaId: Int
)

//Datos de la atencion creada
data class Atencion(
    val atencionId: Int,
    val mesaId: Int,
    val mozoId: Int,
    val fechaApertura: String,
    val estado: String
)

//Respuesta del backend al iniciar una atencion
data class AtencionResponse(
    val mensaje: String,
    val atencion: Atencion
)