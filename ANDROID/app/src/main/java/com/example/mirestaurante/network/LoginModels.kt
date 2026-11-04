package com.example.mirestaurante.network

//Datos que enviamos para iniciar sesion
data class LoginRequest(
    val correo: String,
    val password: String
)

//Informacion del trabajador que devuelve el backend
data class Usuario(
    val usuarioId: Int,
    val nombres: String,
    val correo: String,
    val rol: String
)

//Respuesta del servidor al iniciar sesion correctamente
data class LoginResponse(
    val mensaje: String,
    val token: String,
    val usuario: Usuario
)