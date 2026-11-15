package com.example.mirestaurante.network

// Datos de una categoria del restaurante
data class Categoria(
    val categoriaId: Int,
    val nombre: String
)

// Respuesta al consultar las categorias
data class CategoriasResponse(
    val mensaje: String,
    val categorias: List<Categoria>
)

// Datos de un producto de la carta
data class Producto(
    val productoId: Int,
    val categoriaId: Int,
    val nombre: String,
    val descripcion: String?,
    val precio: Double,
    val disponible: Boolean,
    val categoria: String
)

// Respuesta al consultar los productos
data class ProductosResponse(
    val mensaje: String,
    val productos: List<Producto>
)