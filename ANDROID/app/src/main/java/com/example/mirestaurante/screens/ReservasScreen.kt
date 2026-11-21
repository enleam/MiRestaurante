
package com.example.mirestaurante.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.mirestaurante.network.Reserva
import com.example.mirestaurante.network.RetrofitClient
import kotlinx.coroutines.CancellationException
import java.io.IOException

private val NaranjaReserva = Color(0xFFDB7C43)
private val GrisReserva = Color(0xFF777777)

@Composable
fun PantallaReservas(
    token: String,
    onVolver: () -> Unit
) {

    // Reservas obtenidas desde SQL Server
    var reservas by remember {
        mutableStateOf<List<Reserva>>(emptyList())
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

    // Consultar las reservas mediante Retrofit
    LaunchedEffect(token, numeroActualizacion) {

        cargando = true
        mensajeError = ""

        try {

            val respuesta = RetrofitClient.api.listarReservas(
                "Bearer $token"
            )

            if (respuesta.isSuccessful) {

                val datos = respuesta.body()

                if (datos != null) {
                    reservas = datos.reservas
                } else {
                    mensajeError = "No se recibieron datos"
                }

            } else {

                mensajeError = when (respuesta.code()) {

                    401 -> "Tu sesión ha expirado"

                    403 -> "No tienes permiso para consultar reservas"

                    else -> "Error al consultar reservas (${respuesta.code()})"
                }
            }

        } catch (error: IOException) {

            mensajeError = "No se pudo conectar con el servidor"

        } catch (error: CancellationException) {

            throw error

        } catch (error: Exception) {

            mensajeError = "Ocurrió un error al consultar reservas"

        } finally {

            cargando = false
        }
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

            // Encabezado de la pantalla
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Reservas",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Consulta las reservas registradas",
                        fontSize = 14.sp,
                        color = GrisReserva
                    )
                }

                TextButton(onClick = onVolver) {
                    Text(
                        text = "Volver",
                        color = NaranjaReserva
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actualizar los datos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Reservas (${reservas.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(
                    onClick = {
                        numeroActualizacion++
                    },
                    enabled = !cargando
                ) {
                    Text(
                        text = "Actualizar",
                        color = NaranjaReserva
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Indicador de carga
            if (cargando) {

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NaranjaReserva
                )

            } else if (mensajeError.isNotBlank()) {

                Text(
                    text = mensajeError,
                    color = Color.Red
                )

                TextButton(
                    onClick = {
                        numeroActualizacion++
                    }
                ) {
                    Text("Reintentar")
                }

            } else if (reservas.isEmpty()) {

                Text(
                    text = "No hay reservas registradas",
                    color = GrisReserva
                )

            } else {

                // Mostrar reservas registradas
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {

                    items(
                        reservas,
                        key = { it.reservaId }
                    ) { reserva ->

                        TarjetaReserva(reserva)
                    }
                }
            }
        }
    }
}

// Tarjeta con informacion de cada reserva
@Composable
private fun TarjetaReserva(
    reserva: Reserva
) {

    // Separar la fecha y hora recibidas del backend
    val fechaOriginal = reserva.fechaHora

    val fecha = if (fechaOriginal.length >= 10) {
        val partes = fechaOriginal.substring(0, 10).split("-")

        if (partes.size == 3) {
            "${partes[2]}/${partes[1]}/${partes[0]}"
        } else {
            fechaOriginal
        }
    } else {
        fechaOriginal
    }

    val hora = if (fechaOriginal.length >= 16) {
        fechaOriginal.substring(11, 16)
    } else {
        "--:--"
    }

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

            // Cliente y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = reserva.clienteNombre,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = reserva.estado,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (reserva.estado == "CONFIRMADA") {
                        Color(0xFF36866B)
                    } else {
                        NaranjaReserva
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Mesa: ${reserva.mesaNumero}",
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Fecha: $fecha",
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Hora: $hora",
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Personas: ${reserva.cantidadPersonas}",
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Teléfono: ${reserva.clienteTelefono}",
                fontSize = 15.sp
            )

            // Mostrar observacion cuando exista
            if (!reserva.observacion.isNullOrBlank()) {

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Observación: ${reserva.observacion}",
                    fontSize = 14.sp,
                    color = GrisReserva
                )
            }
        }
    }
}
