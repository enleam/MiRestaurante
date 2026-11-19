package com.example.mirestaurante.network

//Producto que el mozo agrega al pedido
data class ProductoPedidoRequest(
    val productoId: Int,
    val cantidad: Int
)

//Datos que Android envia al backend
data class RegistrarPedidoRequest(
    val atencionId: Int,
    val productos: List<ProductoPedidoRequest>
)

//Informacion de cada producto registrado
data class DetallePedidoResponse(
    val detalleId: Int,
    val productoId: Int,
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

//Informacion del pedido registrado
data class PedidoRegistrado(
    val pedidoId: Int,
    val atencionId: Int,
    val estado: String,
    val fechaRegistro: String,
    val productos: List<DetallePedidoResponse>,
    val total: Double
)

//Respuesta que devuelve el backend
data class RegistrarPedidoResponse(
    val mensaje: String,
    val pedido: PedidoRegistrado
)