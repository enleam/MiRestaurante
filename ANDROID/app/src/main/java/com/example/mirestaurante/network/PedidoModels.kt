
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

//HU09: Informar del pedido enviado a cocina

data class PedidoEnviado(
    val pedidoId: Int,
    val atencionId: Int,
    val estado: String,
    val fechaRegistro: String
)

//HU09: Respuesta del backend al enviar un pedido

data class EnviarPedidoResponse(
    val mensaje: String,
    val pedido: PedidoEnviado
)

//HU10: Pedido consultado desde la pantalla de cocina

data class PedidoCocina(
    val pedidoId: Int,
    val atencionId: Int,
    val estado: String,
    val mesaNumero: Int,
    val fechaRegistro: String,
    val productos: List<DetallePedidoResponse>,
    val total: Double
)
//Respuesta de consulta de pedidos de cocina
data class ConsultarPedidosCocinaResponse(
    val mensaje: String,
    val pedidos: List<PedidoCocina>
)

//Solicitud para actualizar el estado del pedido
data class ActualizarEstadoPedidoRequest(
    val nuevoEstado: String,
)

//Información del pedido actualizado por cocina
data class PedidoEstadoCocina(
    val pedidoId: Int,
    val atencionId: Int,
    val estado: String,
    val fechaRegistro: String,
    val mesaNumero: Int
)
//Respuesta de actualizacion del estado

data class ActualizarEstadoPedidoResponse(
    val mensaje: String,
    val pedido: PedidoEstadoCocina
)