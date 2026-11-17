
package com.example.mirestaurante.network

// Representa una categoria del restaurante
data class Categoria(
    val categoriaId: Int,
    val nombre: String
)

// Respuesta del backend con las categorias
data class CategoriasResponse(
    val mensaje: String,
    val categorias: List<Categoria>
)

// Representa un producto de la carta
data class Producto(
    val productoId: Int,
    val categoriaId: Int,
    val nombre: String,
    val descripcion: String?,
    val precio: Double,
    val disponible: Boolean,
    val categoria: String
)

// Respuesta del backend con los productos
data class ProductosResponse(
    val mensaje: String,
    val productos: List<Producto>
)
