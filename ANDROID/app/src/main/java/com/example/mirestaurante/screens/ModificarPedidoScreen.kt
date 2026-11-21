
package com.example.mirestaurante.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mirestaurante.network.ModificarPedidoRequest
import com.example.mirestaurante.network.PedidoConsultado
import com.example.mirestaurante.network.Producto
import com.example.mirestaurante.network.ProductoPedidoRequest
import com.example.mirestaurante.network.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToLong

private val NaranjaModificar = Color(0xFFDB7C43)

@Composable
fun PantallaModificarPedido(
    token: String,
    pedido: PedidoConsultado,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Productos disponibles de la carta
    var productosCarta by remember {
        mutableStateOf<List<Producto>>(emptyList())
    }

    // Cantidades actuales del pedido
    val cantidades = remember(pedido.pedidoId) {
        mutableStateMapOf<Int, Int>().apply {
            pedido.productos.forEach { producto ->
                put(producto.productoId, producto.cantidad)
            }
        }
    }

    var cargandoCarta by remember {
        mutableStateOf(true)
    }

    var guardando by remember {
        mutableStateOf(false)
    }

    var mensajeError by remember {
        mutableStateOf("")
    }

    var mensajeCarta by remember {
        mutableStateOf("")
    }

    var mostrarConfirmacion by remember {
        mutableStateOf(false)
    }

    // HU08: Consultar productos para poder agregarlos
    LaunchedEffect(token, pedido.pedidoId) {
        cargandoCarta = true
        mensajeCarta = ""

        try {
            val respuesta = RetrofitClient.api.listarProductos(
                "Bearer $token"
            )

            if (respuesta.isSuccessful) {
                val datos = respuesta.body()

                if (datos != null) {
                    productosCarta = datos.productos
                } else {
                    mensajeCarta = "No se recibieron productos"
                }
            } else {
                mensajeCarta =
                    "No se pudo consultar la carta (${respuesta.code()})"
            }

        } catch (error: IOException) {
            mensajeCarta = "No se pudo conectar con el servidor"

        } catch (error: CancellationException) {
            throw error

        } catch (error: Exception) {
            mensajeCarta = "Error al cargar los productos"

        } finally {
            cargandoCarta = false
        }
    }

    // Obtener el precio original de los productos existentes
    val detallesOriginales = pedido.productos.associateBy {
        it.productoId
    }

    // Obtener los productos de la carta por ID
    val cartaPorId = productosCarta.associateBy {
        it.productoId
    }

    // Calcular el total aproximado antes de guardar
    // Los productos existentes mantienen su precio registrado
    val totalCentavos = cantidades.entries.sumOf { (productoId, cantidad) ->

        val precio = detallesOriginales[productoId]?.precioUnitario
            ?: cartaPorId[productoId]?.precio
            ?: 0.0

        (precio * 100).roundToLong() * cantidad
    }

    // Productos que todavía se pueden agregar
    val productosParaAgregar = productosCarta.filter { producto ->
        producto.disponible &&
                !cantidades.containsKey(producto.productoId)
    }

    // Confirmacion despues de modificar
    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = onGuardado,
            title = {
                Text("Pedido actualizado")
            },
            text = {
                Text("Los cambios se guardaron correctamente.")
            },
            confirmButton = {
                TextButton(onClick = onGuardado) {
                    Text("Aceptar")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.White,

        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = "Total: S/ ${
                            String.format(
                                Locale.US,
                                "%.2f",
                                totalCentavos / 100.0
                            )
                        }",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !guardando &&
                                cantidades.isNotEmpty() &&
                                pedido.modificable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NaranjaModificar
                        ),

                        onClick = {
                            mensajeError = ""

                            val listaProductos = cantidades.entries
                                .filter { it.value > 0 }
                                .map { (productoId, cantidad) ->
                                    ProductoPedidoRequest(
                                        productoId = productoId,
                                        cantidad = cantidad
                                    )
                                }

                            if (listaProductos.isEmpty()) {
                                mensajeError =
                                    "El pedido debe tener al menos un producto"
                            } else {

                                scope.launch {
                                    guardando = true

                                    try {
                                        val respuesta =
                                            RetrofitClient.api.modificarPedido(
                                                pedidoId = pedido.pedidoId,
                                                token = "Bearer $token",
                                                datos = ModificarPedidoRequest(
                                                    productos = listaProductos
                                                )
                                            )

                                        if (respuesta.isSuccessful &&
                                            respuesta.body()?.pedido != null
                                        ) {
                                            mostrarConfirmacion = true

                                        } else {
                                            mensajeError = when (
                                                respuesta.code()
                                            ) {
                                                400 ->
                                                    "Verifica los productos y cantidades"

                                                401 ->
                                                    "Tu sesión ha expirado"

                                                403 ->
                                                    "No tienes permiso para modificar pedidos"

                                                404 ->
                                                    "No se encontró el pedido"

                                                409 ->
                                                    "El pedido ya no permite modificaciones"

                                                else ->
                                                    "No se pudo modificar el pedido (${respuesta.code()})"
                                            }
                                        }

                                    } catch (error: IOException) {
                                        mensajeError =
                                            "No se pudo conectar con el servidor"

                                    } catch (error: CancellationException) {
                                        throw error

                                    } catch (error: Exception) {
                                        mensajeError =
                                            "Ocurrió un error al guardar el pedido"

                                    } finally {
                                        guardando = false
                                    }
                                }
                            }
                        }
                    ) {

                        if (guardando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Guardar cambios")
                        }
                    }
                }
            }
        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 20.dp
            )
        ) {

            item {
                TextButton(
                    onClick = onVolver,
                    enabled = !guardando
                ) {
                    Text(
                        "Volver a pedidos",
                        color = NaranjaModificar
                    )
                }

                Text(
                    text = "Modificar pedido",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Pedido N.º ${pedido.pedidoId}",
                    color = Color.Gray
                )

                Text(
                    text = "Estado: ${pedido.estado}",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Productos del pedido",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Mostrar los productos seleccionados
            items(
                items = cantidades.keys.toList().sorted(),
                key = { "seleccion_$it" }
            ) { productoId ->

                val detalle = detallesOriginales[productoId]
                val productoCarta = cartaPorId[productoId]

                val nombre = detalle?.nombre
                    ?: productoCarta?.nombre
                    ?: "Producto $productoId"

                val cantidad = cantidades[productoId] ?: 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F8F8)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {

                        Text(
                            text = nombre,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            OutlinedButton(
                                onClick = {
                                    if (cantidad <= 1) {
                                        cantidades.remove(productoId)
                                    } else {
                                        cantidades[productoId] = cantidad - 1
                                    }
                                },
                                enabled = !guardando
                            ) {
                                Text("-")
                            }

                            Text(
                                text = cantidad.toString(),
                                modifier = Modifier.padding(
                                    horizontal = 14.dp
                                ),
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedButton(
                                onClick = {
                                    if (cantidad < 9999) {
                                        cantidades[productoId] = cantidad + 1
                                    }
                                },
                                enabled = !guardando &&
                                        cantidad < 9999
                            ) {
                                Text("+")
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            TextButton(
                                onClick = {
                                    cantidades.remove(productoId)
                                },
                                enabled = !guardando
                            ) {
                                Text(
                                    "Quitar",
                                    color = Color.Red
                                )
                            }
                        }
                    }
                }
            }

            item {
                if (cantidades.isEmpty()) {
                    Text(
                        text = "Agrega al menos un producto para guardar.",
                        color = Color.Red
                    )
                }

                if (mensajeError.isNotBlank()) {
                    Text(
                        text = mensajeError,
                        color = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Agregar productos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (cargandoCarta) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (mensajeCarta.isNotBlank()) {
                    Text(
                        text = mensajeCarta,
                        color = Color.Red
                    )
                }
            }

            // Mostrar productos disponibles que aun no estan
            // incluidos en el pedido
            items(
                items = productosParaAgregar,
                key = { "carta_${it.productoId}" }
            ) { producto ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F8F8)
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = producto.nombre,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = String.format(
                                    Locale.US,
                                    "S/ %.2f",
                                    producto.precio
                                ),
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }

                        TextButton(
                            onClick = {
                                cantidades[producto.productoId] = 1
                            },
                            enabled = !guardando
                        ) {
                            Text(
                                "Agregar",
                                color = NaranjaModificar
                            )
                        }
                    }
                }
            }
        }
    }
}
