
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
import com.example.mirestaurante.network.Categoria
import com.example.mirestaurante.network.Producto
import com.example.mirestaurante.network.RetrofitClient
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.Locale

private val NaranjaCarta = Color(0xFFDB7C43)
private val VerdeCarta = Color(0xFF36866B)
private val GrisCarta = Color(0xFF777777)

@Composable
fun PantallaCarta(
    token: String,
    onVolver: () -> Unit
) {

    // Datos recibidos desde el backend
    var categorias by remember {
        mutableStateOf<List<Categoria>>(emptyList())
    }

    var productos by remember {
        mutableStateOf<List<Producto>>(emptyList())
    }

    // Filtro de categorias
    var categoriaSeleccionada by rememberSaveable {
        mutableStateOf<Int?>(null)
    }

    // Estados de carga
    var cargando by remember {
        mutableStateOf(true)
    }

    var mensajeError by remember {
        mutableStateOf("")
    }

    var numeroActualizacion by remember {
        mutableIntStateOf(0)
    }

    // Consultar la carta desde SQL Server
    LaunchedEffect(token, numeroActualizacion) {

        cargando = true
        mensajeError = ""

        try {
            val respuestaCategorias =
                RetrofitClient.api.listarCategorias(
                    "Bearer $token"
                )

            val respuestaProductos =
                RetrofitClient.api.listarProductos(
                    "Bearer $token"
                )

            if (
                respuestaCategorias.isSuccessful &&
                respuestaProductos.isSuccessful
            ) {

                val datosCategorias =
                    respuestaCategorias.body()

                val datosProductos =
                    respuestaProductos.body()

                if (
                    datosCategorias != null &&
                    datosProductos != null
                ) {
                    categorias = datosCategorias.categorias
                    productos = datosProductos.productos
                } else {
                    mensajeError =
                        "No se recibieron datos del catálogo"
                }

            } else {

                val codigo = if (
                    !respuestaCategorias.isSuccessful
                ) {
                    respuestaCategorias.code()
                } else {
                    respuestaProductos.code()
                }

                mensajeError = when (codigo) {
                    401 -> "Tu sesión ha expirado"
                    403 -> "No tienes permiso para consultar la carta"
                    else -> "Error al consultar la carta ($codigo)"
                }
            }

        } catch (error: IOException) {

            mensajeError =
                "No se pudo conectar con el servidor"

        } catch (error: CancellationException) {

            throw error

        } catch (error: Exception) {

            mensajeError =
                "Ocurrió un error al cargar la carta"

        } finally {
            cargando = false
        }
    }

    // Filtrar productos
    val productosFiltrados = productos.filter { producto ->
        categoriaSeleccionada == null ||
                producto.categoriaId == categoriaSeleccionada
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

            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Carta del restaurante",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Consulta nuestros productos",
                        fontSize = 14.sp,
                        color = GrisCarta
                    )
                }

                TextButton(onClick = onVolver) {
                    Text(
                        text = "Volver",
                        color = NaranjaCarta
                    )
                }
            }

            // Boton actualizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(
                    onClick = { numeroActualizacion++ },
                    enabled = !cargando
                ) {
                    Text(
                        text = "Actualizar",
                        color = NaranjaCarta
                    )
                }
            }

            if (cargando) {

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NaranjaCarta
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

                Text(
                    text = "Categorías",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filtros de categorias
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    FilterChip(
                        selected = categoriaSeleccionada == null,
                        onClick = {
                            categoriaSeleccionada = null
                        },
                        label = { Text("Todas") }
                    )

                    categorias.forEach { categoria ->

                        FilterChip(
                            selected =
                                categoriaSeleccionada ==
                                        categoria.categoriaId,
                            onClick = {
                                categoriaSeleccionada =
                                    categoria.categoriaId
                            },
                            label = {
                                Text(categoria.nombre)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Productos (${productosFiltrados.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Lista de productos
                if (productosFiltrados.isEmpty()) {

                    Text(
                        text = "No hay productos en esta categoría",
                        color = GrisCarta
                    )

                } else {

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp),
                        contentPadding =
                            PaddingValues(bottom = 16.dp)
                    ) {

                        items(
                            productosFiltrados,
                            key = { it.productoId }
                        ) { producto ->

                            TarjetaProductoCarta(producto)
                        }
                    }
                }
            }
        }
    }
}

// Mostrar los datos de cada producto
@Composable
private fun TarjetaProductoCarta(
    producto: Producto
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            Color(0xFFE3E3E3)
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text = producto.nombre,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = String.format(
                        Locale.US,
                        "S/ %.2f",
                        producto.precio
                    ),
                    fontWeight = FontWeight.Bold,
                    color = NaranjaCarta
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = producto.categoria,
                fontSize = 12.sp,
                color = GrisCarta
            )

            if (!producto.descripcion.isNullOrBlank()) {

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = producto.descripcion,
                    fontSize = 14.sp,
                    color = GrisCarta
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (producto.disponible) {
                    "Disponible"
                } else {
                    "No disponible"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (producto.disponible) {
                    VerdeCarta
                } else {
                    Color.Red
                }
            )
        }
    }
}
