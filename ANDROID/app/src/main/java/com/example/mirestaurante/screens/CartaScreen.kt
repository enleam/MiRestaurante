package com.example.mirestaurante.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

// Colores del prototipo
private val Naranja = Color(0xFFDB7C43)
private val Verde = Color(0xFF36866B)
private val GrisTexto = Color(0xFF777777)
private val GrisBorde = Color(0xFFE3E3E3)

@Composable
fun PantallaCarta(
    token: String,
    onVolver: () -> Unit
) {

    // Datos obtenidos desde SQL Server
    var categorias by remember {
        mutableStateOf<List<Categoria>>(emptyList())
    }

    var productos by remember {
        mutableStateOf<List<Producto>>(emptyList())
    }

    // Categoria seleccionada
    // null significa mostrar todas
    var categoriaSeleccionada by rememberSaveable {
        mutableStateOf<Int?>(null)
    }

    // Estados de la pantalla
    var cargando by remember {
        mutableStateOf(true)
    }

    var mensajeError by remember {
        mutableStateOf("")
    }

    var numeroActualizacion by remember {
        mutableIntStateOf(0)
    }

    // Consultar categorias y productos
    LaunchedEffect(token, numeroActualizacion) {

        cargando = true
        mensajeError = ""

        try {

            // Obtener categorias
            val respuestaCategorias =
                RetrofitClient.api.listarCategorias(
                    "Bearer $token"
                )

            // Obtener productos
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

                    401 ->
                        "Tu sesión ha expirado. Inicia sesión nuevamente"

                    403 ->
                        "No tienes permiso para consultar la carta"

                    else ->
                        "Error al consultar la carta ($codigo)"
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

    // Filtrar productos segun la categoria
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
                .background(Color.White)
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
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Consulta nuestros productos",
                        fontSize = 14.sp,
                        color = GrisTexto
                    )
                }

                TextButton(
                    onClick = onVolver
                ) {
                    Text(
                        text = "Volver",
                        color = Naranja
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Boton para actualizar el catalogo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(
                    onClick = {
                        numeroActualizacion++
                    },
                    enabled = !cargando
                ) {

                    Text(
                        text = "Actualizar",
                        color = Naranja
                    )
                }
            }

            // Indicador de carga
            if (cargando) {

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Naranja
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Mensaje de error
            if (mensajeError.isNotBlank()) {

                Text(
                    text = mensajeError,
                    color = Color(0xFFB3261E),
                    fontSize = 14.sp
                )

                TextButton(
                    onClick = {
                        numeroActualizacion++
                    },
                    enabled = !cargando
                ) {

                    Text(
                        text = "Reintentar",
                        color = Naranja
                    )
                }
            }

            // Mostrar informacion si existen datos
            if (!cargando && mensajeError.isBlank()) {

                Text(
                    text = "Categorías",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filtros por categoria
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    // Filtro Todas
                    FilterChip(
                        selected = categoriaSeleccionada == null,
                        onClick = {
                            categoriaSeleccionada = null
                        },
                        label = {
                            Text("Todas")
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Naranja,
                                selectedLabelColor = Color.White
                            )
                    )

                    // Categorias reales del backend
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
                            },

                            shape = RoundedCornerShape(24.dp),

                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Naranja,
                                    selectedLabelColor = Color.White
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cantidad de productos encontrados
                Text(
                    text = "Productos (${productosFiltrados.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (productosFiltrados.isEmpty()) {

                    Text(
                        text = "No hay productos en esta categoría",
                        color = GrisTexto,
                        fontSize = 14.sp
                    )

                } else {

                    // Lista de productos
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

                            TarjetaProducto(producto)
                        }
                    }
                }
            }
        }
    }
}

// Tarjeta individual de producto
@Composable
private fun TarjetaProducto(
    producto: Producto
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            GrisBorde
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            // Nombre y precio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Text(
                    text = producto.nombre,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = String.format(
                        Locale.US,
                        "S/ %.2f",
                        producto.precio
                    ),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Naranja
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Categoria
            Text(
                text = producto.categoria,
                fontSize = 12.sp,
                color = GrisTexto
            )

            // Descripcion del producto
            if (!producto.descripcion.isNullOrBlank()) {

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = producto.descripcion,
                    fontSize = 14.sp,
                    color = GrisTexto
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Disponibilidad
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (producto.disponible) {
                    Color(0xFFE8F5EE)
                } else {
                    Color(0xFFFFEEEE)
                }
            ) {

                Text(
                    text = if (producto.disponible) {
                        "Disponible"
                    } else {
                        "No disponible"
                    },
                    color = if (producto.disponible) {
                        Verde
                    } else {
                        Color(0xFFB3261E)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                )
            }
        }
    }
}