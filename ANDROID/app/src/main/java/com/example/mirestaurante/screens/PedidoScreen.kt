package com.example.mirestaurante.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mirestaurante.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToLong

private val NaranjaPedido = Color(0xFFDB7C43)
private val VerdePedido = Color(0xFF36866B)
private val GrisPedido = Color(0xFF777777)

@Composable
fun PantallaRegistrarPedido(
    token: String,
    atencionId: Int,
    onVolver: () -> Unit
) {
    //Datos de la carta
    var categorias by remember {
        mutableStateOf<List<Categoria>>(emptyList())
    }

    var productos by remember {
        mutableStateOf<List<Producto>>(emptyList())
    }

    //Categoria seleccionada
    var categoriaSeleccionada by rememberSaveable {
        mutableStateOf<Int?>(null)
    }

    //Cantidad seleccionada por cada producto
    var cantidades by remember {
        mutableStateOf<Map<Int, Int>>(emptyMap())
    }

    //Estados de la pantalla
    var cargando by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }
    var mensajeRegistroError by remember { mutableStateOf("") }

    var pedidoRegistrado by remember {
        mutableStateOf<PedidoRegistrado?>(null)
    }

    var numeroActualizacion by remember {
        mutableIntStateOf(0)
    }

    val scope = rememberCoroutineScope()

    //Consultar los productos desde el backend
    LaunchedEffect(token, numeroActualizacion) {
        cargando = true
        mensajeError = ""

        try {
            val respuestaCategorias =
                RetrofitClient.api.listarCategorias("Bearer $token")

            val respuestaProductos =
                RetrofitClient.api.listarProductos("Bearer $token")

            if (respuestaCategorias.isSuccessful &&
                respuestaProductos.isSuccessful) {

                val datosCategorias = respuestaCategorias.body()
                val datosProductos = respuestaProductos.body()

                if (datosCategorias != null && datosProductos != null) {
                    categorias = datosCategorias.categorias
                    productos = datosProductos.productos
                } else {
                    mensajeError = "No se recibieron datos de la carta"
                }
            } else {
                mensajeError = "Error al consultar los productos"
            }
        } catch (error: IOException) {
            mensajeError = "No se pudo conectar con el servidor"

        } catch (error: CancellationException) {
            throw error

        } catch (error: Exception) {
            mensajeError = "Ocurrio un error al cargar los productos"

        } finally {
            cargando = false
        }
    }

    //Filtrar productos por categoria
    val productosFiltrados = productos.filter {
        categoriaSeleccionada == null ||
                it.categoriaId == categoriaSeleccionada
    }

    //Obtener los productos seleccionados
    val seleccionados = productos.filter {
        it.disponible && (cantidades[it.productoId] ?: 0) > 0
    }

    //Calcular total usando centavos
    val totalCentavos = seleccionados.sumOf { producto ->
        val precioCentavos = (producto.precio * 100).roundToLong()
        precioCentavos * (cantidades[producto.productoId] ?: 0)
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            //Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Registrar pedido",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Atencion N.º $atencionId",
                        fontSize = 14.sp,
                        color = GrisPedido
                    )
                }

                TextButton(
                    onClick = onVolver,
                    enabled = !enviando
                ) {
                    Text("Volver", color = NaranjaPedido)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            //Mostrar confirmacion del pedido
            val pedidoConfirmado = pedidoRegistrado

            if (pedidoConfirmado != null) {
                Text(
                    text = "Pedido registrado correctamente",
                    color = VerdePedido,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Pedido N.º ${pedidoConfirmado.pedidoId}")
                Text("Estado: ${pedidoConfirmado.estado}")

                Text(
                    text = String.format(
                        Locale.US,
                        "Total: S/ %.2f",
                        pedidoConfirmado.total
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        pedidoRegistrado = null
                        cantidades = emptyMap()
                        mensajeRegistroError = ""
                        numeroActualizacion++
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NaranjaPedido
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registrar otro pedido")
                }

                TextButton(
                    onClick = onVolver,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver a mesas")
                }

            } else if (cargando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NaranjaPedido
                )
            } else if (mensajeError.isNotBlank()) {
                Text(
                    text = mensajeError,
                    color = Color.Red
                )

                TextButton(
                    onClick = { numeroActualizacion++ }
                ) {
                    Text("Reintentar")
                }
            } else {
                //Filtros de categorias
                Text(
                    text = "Categorias",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoriaSeleccionada == null,
                        onClick = { categoriaSeleccionada = null },
                        label = { Text("Todas") }
                    )

                    categorias.forEach { categoria ->
                        FilterChip(
                            selected = categoriaSeleccionada ==
                                    categoria.categoriaId,
                            onClick = {
                                categoriaSeleccionada = categoria.categoriaId
                            },
                            label = { Text(categoria.nombre) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Selecciona los productos",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                //Lista de productos
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(
                        productosFiltrados,
                        key = { it.productoId }
                    ) { producto ->
                        val cantidad = cantidades[producto.productoId] ?: 0

                        TarjetaProductoPedido(
                            producto = producto,
                            cantidad = cantidad,
                            onAumentar = {
                                if (cantidad < Int.MAX_VALUE) {
                                    cantidades = cantidades +
                                            (producto.productoId to cantidad + 1)
                                }
                            },
                            onDisminuir = {
                                if (cantidad > 0) {
                                    cantidades = cantidades +
                                            (producto.productoId to cantidad - 1)
                                }
                            }
                        )
                    }
                }

                HorizontalDivider()

                Spacer(modifier = Modifier.height(12.dp))

                //Resumen del pedido
                Text(
                    text = "Productos seleccionados: ${seleccionados.size}",
                    fontSize = 14.sp
                )

                Text(
                    text = String.format(
                        Locale.US,
                        "Total: S/ %.2f",
                        totalCentavos / 100.0
                    ),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NaranjaPedido
                )

                if (mensajeRegistroError.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = mensajeRegistroError,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                //Guardar el pedido
                Button(
                    onClick = {
                        scope.launch {
                            enviando = true
                            mensajeRegistroError = ""

                            try {
                                val datos = RegistrarPedidoRequest(
                                    atencionId = atencionId,
                                    productos = seleccionados.map {
                                        ProductoPedidoRequest(
                                            productoId = it.productoId,
                                            cantidad = cantidades[it.productoId] ?: 0
                                        )
                                    }
                                )

                                val respuesta =
                                    RetrofitClient.api.registrarPedido(
                                        "Bearer $token",
                                        datos
                                    )

                                if (respuesta.isSuccessful) {
                                    val resultado = respuesta.body()

                                    if (resultado != null) {
                                        pedidoRegistrado = resultado.pedido
                                    } else {
                                        mensajeRegistroError =
                                            "No se recibio la confirmacion"
                                    }

                                } else {
                                    mensajeRegistroError =
                                        when (respuesta.code()) {
                                            400 -> "Verifica los productos seleccionados"
                                            401 -> "Tu sesion ha expirado"
                                            403 -> "No tienes permiso para registrar pedidos"
                                            409 -> "La atencion no esta disponible"
                                            else -> "Error al registrar el pedido"
                                        }
                                }

                            } catch (error: IOException) {
                                mensajeRegistroError =
                                    "No se pudo conectar con el servidor"

                            } catch (error: CancellationException) {
                                throw error

                            } catch (error: Exception) {
                                mensajeRegistroError =
                                    "Ocurrio un error al registrar el pedido"

                            } finally {
                                enviando = false
                            }
                        }
                    },
                    enabled = !enviando &&
                            seleccionados.isNotEmpty() &&
                            atencionId > 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NaranjaPedido
                    )
                ) {
                    Text(
                        if (enviando) "Registrando..."
                        else "Registrar pedido"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

//Tarjeta individual de cada producto
@Composable
private fun TarjetaProductoPedido(
    producto: Producto,
    cantidad: Int,
    onAumentar: () -> Unit,
    onDisminuir: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE3E3E3))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = producto.nombre,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = String.format(
                        Locale.US,
                        "S/ %.2f",
                        producto.precio
                    ),
                    color = NaranjaPedido,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (producto.disponible) {
                    "Disponible"
                } else {
                    "No disponible"
                },
                color = if (producto.disponible) {
                    VerdePedido
                } else {
                    Color.Red
                },
                fontSize = 13.sp
            )

            //Control de cantidades
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDisminuir,
                    enabled = cantidad > 0
                ) {
                    Text("−", fontSize = 22.sp)
                }
                Text(
                    text = "$cantidad",
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onAumentar,
                    enabled = producto.disponible
                ) {
                    Text("+", fontSize = 22.sp)
                }
            }

            if (cantidad > 0) {
                Text(
                    text = String.format(
                        Locale.US,
                        "Subtotal: S/ %.2f",
                        producto.precio * cantidad
                    ),
                    color = VerdePedido,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}