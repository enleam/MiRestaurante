
package com.example.mirestaurante.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mirestaurante.network.PedidoConsultado
import com.example.mirestaurante.network.RetrofitClient
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.Locale

private val NaranjaPedidos = Color(0xFFDB7C43)

@Composable
fun PantallaPedidosAtencion(
    token: String,
    atencionId: Int,
    onVolver: () -> Unit
) {
    var pedidos by remember {
        mutableStateOf<List<PedidoConsultado>>(emptyList())
    }

    var cargando by remember {
        mutableStateOf(true)
    }

    var mensajeError by remember {
        mutableStateOf("")
    }

    var actualizacion by remember {
        mutableIntStateOf(0)
    }

    // Pedido seleccionado para modificar
    var pedidoSeleccionado by remember {
        mutableStateOf<PedidoConsultado?>(null)
    }

    // Consultar pedidos desde el backend
    LaunchedEffect(token, atencionId, actualizacion) {
        cargando = true
        mensajeError = ""

        try {
            val respuesta =
                RetrofitClient.api.listarPedidosPorAtencion(
                    atencionId = atencionId,
                    token = "Bearer $token"
                )

            if (respuesta.isSuccessful) {
                val datos = respuesta.body()

                if (datos != null) {
                    pedidos = datos.pedidos
                } else {
                    mensajeError = "No se recibieron datos"
                }
            } else {
                mensajeError = when (respuesta.code()) {
                    401 -> "Tu sesión ha expirado"
                    403 -> "No tienes permiso para consultar pedidos"
                    404 -> "No se encontró la atención"
                    409 -> "La atención ya está cerrada"
                    else -> "Error al consultar pedidos (${respuesta.code()})"
                }
            }

        } catch (error: IOException) {
            mensajeError = "No se pudo conectar con el servidor"

        } catch (error: CancellationException) {
            throw error

        } catch (error: Exception) {
            mensajeError = "Ocurrió un error al cargar los pedidos"

        } finally {
            cargando = false
        }
    }

    // HU08: Abrir la pantalla de modificación
    val pedidoEditar = pedidoSeleccionado

    if (pedidoEditar != null) {
        PantallaModificarPedido(
            token = token,
            pedido = pedidoEditar,
            onVolver = {
                pedidoSeleccionado = null
            },
            onGuardado = {
                pedidoSeleccionado = null
                actualizacion++
            }
        )
        return
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(18.dp)
        ) {

            TextButton(onClick = onVolver) {
                Text(
                    text = "Volver a mesas",
                    color = NaranjaPedidos
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Pedidos de la atención",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Atención N.º $atencionId",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { actualizacion++ },
                enabled = !cargando,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NaranjaPedidos
                )
            ) {
                Text("Actualizar pedidos")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (cargando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (mensajeError.isNotBlank()) {
                Text(
                    text = mensajeError,
                    color = Color.Red
                )
            }

            if (!cargando &&
                mensajeError.isBlank() &&
                pedidos.isEmpty()
            ) {
                Text(
                    text = "Esta atención todavía no tiene pedidos.",
                    color = Color.Gray
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    bottom = 20.dp
                )
            ) {
                items(
                    items = pedidos,
                    key = { it.pedidoId }
                ) { pedido ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8F8F8)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "Pedido N.º ${pedido.pedidoId}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Estado: ${pedido.estado}",
                                color = Color.DarkGray
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            pedido.productos.forEach { producto ->
                                Text(
                                    text = "${producto.cantidad} x ${producto.nombre}"
                                )

                                Text(
                                    text = "Subtotal: S/ ${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            producto.subtotal
                                        )
                                    }",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Spacer(
                                    modifier = Modifier.height(5.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    vertical = 10.dp
                                )
                            )

                            Text(
                                text = "Total: S/ ${
                                    String.format(
                                        Locale.US,
                                        "%.2f",
                                        pedido.total
                                    )
                                }",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Button(
                                onClick = {
                                    pedidoSeleccionado = pedido
                                },
                                enabled = pedido.modificable,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NaranjaPedidos
                                )
                            ) {
                                Text(
                                    if (pedido.modificable)
                                        "Modificar pedido"
                                    else
                                        "No modificable"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
