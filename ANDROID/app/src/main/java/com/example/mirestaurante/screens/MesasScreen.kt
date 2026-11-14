package com.example.mirestaurante.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mirestaurante.R
import com.example.mirestaurante.network.Mesa
import com.example.mirestaurante.network.RetrofitClient
import kotlinx.coroutines.CancellationException
import java.io.IOException

//Colores del prototipo
private val Naranja = Color(0xFFDB7C43)
private val Verde = Color(0xFF36866B)
private val Azul = Color(0xFF5D83A7)
private val GrisBorde = Color(0xFFE3E3E3)
private val GrisTexto = Color(0xFF777777)

@Composable
fun PantallaMesas(token: String, onCerrarSesion: () -> Unit) {

    var filtro by rememberSaveable {
        mutableStateOf("Todas")
    }

    // Controlar el menu del usuario
    var menuUsuarioAbierto by remember {
        mutableStateOf(false)
    }

    // Mesas obtenidas desde SQL Server a traves de la API
    var mesas by remember { mutableStateOf<List<Mesa>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf("") }
    var numeroActualizacion by remember { mutableStateOf(0) }

    // Consultar al entrar en la pantalla y al presionar Actualizar
    LaunchedEffect(token, numeroActualizacion) {
        cargando = true
        mensajeError = ""

        try {
            val respuesta = RetrofitClient.api.listarMesas("Bearer $token")

            if (respuesta.isSuccessful) {
                val datos = respuesta.body()
                if (datos != null) {
                    mesas = datos.mesas
                } else {
                    mensajeError = "El servidor no devolvio datos de mesas"
                }
            } else {
                mensajeError = when (respuesta.code()) {
                    401 -> "La sesion ha expirado. Inicia sesion nuevamente"
                    403 -> "No tienes permiso para consultar las mesas"
                    else -> "No se pudieron obtener las mesas (${respuesta.code()})"
                }
            }
        } catch (error: IOException) {
            mensajeError = "No se pudo conectar con el servidor"
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            mensajeError = "Ocurrio un error al cargar las mesas"
        } finally {
            cargando = false
        }
    }

    //Aplicar el filtro seleccionado
    val mesasFiltradas = mesas.filter { mesa ->
        when (filtro) {
            "Libres" -> mesa.estado == "LIBRE"
            "Ocupadas" -> mesa.estado == "OCUPADA"
            "Reservadas" -> mesa.estado == "RESERVADA"
            else -> true
        }
    }

    Scaffold(
        containerColor = Color.White,

        //Menu inferior
        bottomBar = {
            MenuInferiorMesas()
        }

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

                Column {
                    Text(
                        text = "Gestión de mesas",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Consulta el estado de las mesas",
                        fontSize = 14.sp,
                        color = GrisTexto
                    )
                }

                Box {
                    //Boton de usuario
                    IconButton(
                        onClick = {
                            menuUsuarioAbierto = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_person),
                            contentDescription = "Opciones de usuario",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    //Menu desplegable
                    DropdownMenu(
                        expanded = menuUsuarioAbierto,
                        onDismissRequest = {
                            menuUsuarioAbierto = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Cerrar sesión")
                            },
                            onClick = {
                                menuUsuarioAbierto = false
                                onCerrarSesion()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Permite volver a consultar los estados de las mesas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { numeroActualizacion++ },
                    enabled = !cargando
                ) {
                    Text("Actualizar", color = Naranja)
                }
            }

            // Indicador de consulta y errores de conexion
            if (cargando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Naranja
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (mensajeError.isNotBlank()) {
                Text(text = mensajeError, color = Color(0xFFB3261E))
                TextButton(onClick = { numeroActualizacion++ }) {
                    Text("Reintentar", color = Naranja)
                }
            }

            //Contadores de mesas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                val resumen = listOf(
                    Triple(
                        "Libres",
                        mesas.count { it.estado == "LIBRE" },
                        Verde
                    ),
                    Triple(
                        "Ocupadas",
                        mesas.count { it.estado == "OCUPADA" },
                        Naranja
                    ),
                    Triple(
                        "Reservadas",
                        mesas.count { it.estado == "RESERVADA" },
                        Azul
                    )
                )

                resumen.forEach { item ->

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(82.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, GrisBorde)
                    ) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Text(
                                text = item.second.toString(),
                                color = item.third,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.first,
                                color = GrisTexto,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            //Filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "Todas",
                    "Libres",
                    "Ocupadas",
                    "Reservadas"
                ).forEach { opcion ->

                    FilterChip(
                        selected = filtro == opcion,
                        onClick = {
                            filtro = opcion
                        },
                        label = {
                            Text(
                                text = opcion,
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = Color.DarkGray,
                            selectedContainerColor = Naranja,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Si la lista viene vacia, mostrar un mensaje
            if (!cargando && mensajeError.isBlank() && mesasFiltradas.isEmpty()) {
                Text(
                    text = "No hay mesas para el filtro seleccionado",
                    color = GrisTexto,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            //Cuadricula de mesas
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {

                items(
                    mesasFiltradas,
                    key = { it.mesaId }
                ) { mesa ->

                    TarjetaMesa(mesa)
                }
            }
        }
    }
}

//Tarjeta individual de mesa
@Composable
private fun TarjetaMesa(mesa: Mesa) {

    val colorEstado = when (mesa.estado) {
        "LIBRE" -> Verde
        "OCUPADA" -> Naranja
        "RESERVADA" -> Azul
        else -> GrisTexto
    }

    val textoEstado = when (mesa.estado) {
        "LIBRE" -> "Libre"
        "OCUPADA" -> "Ocupada"
        "RESERVADA" -> "Reservada"
        else -> mesa.estado
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(146.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GrisBorde)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Icon(
                painter = painterResource(R.drawable.ic_mesa),
                contentDescription = null,
                tint = colorEstado,
                modifier = Modifier.size(27.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Mesa ${mesa.numero.toString().padStart(2, '0')}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            colorEstado,
                            RoundedCornerShape(50)
                        )
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = textoEstado,
                    color = colorEstado,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Capacidad: ${mesa.capacidad} personas",
                fontSize = 11.sp,
                color = GrisTexto
            )
        }
    }
}

//Menu de navegacion inferior
@Composable
private fun MenuInferiorMesas() {

    val opciones = listOf(
        Triple("Mesas", R.drawable.ic_menu_mesas, true),
        Triple("Pedidos", R.drawable.ic_pedidos, false),
        Triple("Reservas", R.drawable.ic_reservas, false)
    )

    Column(
        modifier = Modifier.background(Color.White)
    ) {

        HorizontalDivider(
            color = GrisBorde,
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            opciones.forEach { opcion ->

                val color = if (opcion.third) {
                    Naranja
                } else {
                    GrisTexto
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Icon(
                        painter = painterResource(opcion.second),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = opcion.first,
                        fontSize = 12.sp,
                        color = color,
                        fontWeight = if (opcion.third) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            }
        }
    }
}
