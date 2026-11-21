
package com.example.mirestaurante.network

// HU07: Registrar pedidos

data class ProductoPedidoRequest(
    val productoId: Int,
    val cantidad: Int
)

data class RegistrarPedidoRequest(
    val atencionId: Int,
    val productos: List<ProductoPedidoRequest>
)

data class DetallePedidoResponse(
    val detalleId: Int,
    val productoId: Int,
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

data class PedidoRegistrado(
    val pedidoId: Int,
    val atencionId: Int,
    val estado: String,
    val fechaRegistro: String,
    val productos: List<DetallePedidoResponse>,
    val total: Double
)

data class RegistrarPedidoResponse(
    val mensaje: String,
    val pedido: PedidoRegistrado
)

// HU08: Consultar pedidos

data class PedidoConsultado(
    val pedidoId: Int,
    val atencionId: Int,
    val estado: String,
    val fechaRegistro: String,
    val modificable: Boolean,
    val productos: List<DetallePedidoResponse>,
    val total: Double
)

data class ConsultarPedidosResponse(
    val mensaje: String,
    val pedidos: List<PedidoConsultado>
)

// HU08: Modificar pedidos

data class ModificarPedidoRequest(
    val productos: List<ProductoPedidoRequest>
)

data class PedidoModificado(
    val pedidoId: Int,
    val atencionId: Int,
    val estado: String,
    val productos: List<DetallePedidoResponse>,
    val total: Double
)

data class ModificarPedidoResponse(
    val mensaje: String,
    val pedido: PedidoModificado
)
