
package com.example.mirestaurante.network

data class Reserva(
    val reservaId: Int,
    val clienteNombre: String,
    val clienteTelefono: String,
    val mesaId: Int,
    val mesaNumero: Int,
    val fechaHora: String,
    val cantidadPersonas: Int,
    val estado: String,
    val observacion: String?
)

data class ReservasResponse(
    val mensaje: String,
    val reservas: List<Reserva>
)
