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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mirestaurante.R
import com.example.mirestaurante.network.PedidoCocina
import com.example.mirestaurante.network.ActualizarEstadoPedidoRequest
import com.example.mirestaurante.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.Locale

//Colores del prototipo
private val Naranja = Color(0xFFDB7C43)
private val Amarillo = Color(0xFFD59A47)
private val Azul = Color(0xFF5D83A7)
private val GrisTexto = Color(0xFF777777)
private val GrisBorde = Color(0xFFE3E3E3)

@Composable
fun PantallaCocina(token: String, onCerrarSesion: () -> Unit) {

    var filtro by rememberSaveable {
        mutableStateOf("Todos")
    }

    var menuAbierto by remember {
        mutableStateOf(false)
    }

    //HU10: Pedidos obtenidos desde el backend
    var pedidos by remember {
        mutableStateOf<List<PedidoCocina>>(emptyList())
    }

    //Estado de carga
    var cargando by remember {
        mutableStateOf((true))
    }

    //Mensaje de error y confirmacion
    var mensajeError by remember {
        mutableStateOf("")
    }

    var mensajeExito by remember {
        mutableStateOf("")
    }

    //Identificar el pedido que se esta actualizando
    var pedidoActualizandoId by remember {
        mutableStateOf<Int?>(null)
    }

    //Permite actualizar la lista
    var actualizacion by remember {
        mutableStateOf(0)
    }

    val scope = rememberCoroutineScope()
    //HU10: Consultar pedidos mediante la API REST
    LaunchedEffect(token, actualizacion) {
        cargando = true
        mensajeError = ""

        try {
            val respuesta = RetrofitClient.api.listarPedidosCocina(
                token = "Bearer $token"
            )
            if (respuesta.isSuccessful){
                val datos = respuesta.body()
                if (datos != null){
                    pedidos = datos.pedidos
                } else {
                    mensajeError = "No se recibieron datos del servidor"
                }
                } else {
                    mensajeError = when (respuesta.code()){
                        401 -> "Tu sesión ha expirado"
                        403 -> "No tienes permiso para consultar pedidos de cocina"
                        else -> "Error al consultar pedidos (${respuesta.code()})"
                    }
                }
            }catch (error: IOException){
                mensajeError = "No se pudo conectar con el servidor"
            } catch (error: CancellationException){
                throw error
            }catch (error: Exception){
                mensajeError = "Ocurrió un error al cargar los pedidos"
            }finally {
                cargando = false
            }
    }
    //HU10: Contadores segun el estado real de los pedidos

    val pendientes = pedidos.count{
        it.estado == "ENVIADO"
    }
    val enPreparacion = pedidos.count{
        it.estado == "EN_PREPARACION"
    }
    val listos = pedidos.count{
        it.estado == "LISTO"
    }

    //Filtrar pedidos
    val pedidosFiltrados = pedidos.filter { pedido ->
        when (filtro) {
            "Pendientes" -> pedido.estado == "ENVIADO"
            "En preparación" -> pedido.estado == "EN_PREPARACION"
            "Listos" -> pedido.estado == "LISTO"
            else -> true
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        containerColor = Color.White
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(18.dp))

            //Encabezado
            Text(
                text = "Mi Restaurante",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Naranja
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = "Panel de cocina",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Gestiona los pedidos recibidos",
                        fontSize = 14.sp,
                        color = GrisTexto
                    )
                }

                //Menu de usuario
                Box {
                    IconButton(
                        onClick = {
                            menuAbierto = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_person
                            ),
                            contentDescription = "Opciones de usuario",
                            tint = Color.DarkGray
                        )
                    }

                    DropdownMenu(
                        expanded = menuAbierto,
                        onDismissRequest = {
                            menuAbierto = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Cerrar sesión")
                            },
                            onClick = {
                                menuAbierto = false
                                onCerrarSesion()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            //Resumen de pedidos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenPedido(
                    cantidad = pendientes,
                    titulo = "Pendientes",
                    color = Amarillo,
                    modifier = Modifier.weight(1f)
                )

                ResumenPedido(
                    cantidad = enPreparacion,
                    titulo = "En preparación",
                    color = Naranja,
                    modifier = Modifier.weight(1f)
                )

                ResumenPedido(
                    cantidad = listos,
                    titulo = "Listos",
                    color = Azul,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            //Filtros de pedidos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Todos",
                    "Pendientes",
                    "En preparación",
                    "Listos"
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
                        shape = RoundedCornerShape(20.dp),
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

            Spacer(modifier = Modifier.height(12.dp))

            //Encabezado de pedidos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pedidos recibidos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                //HU10: Actualizar pedidos desde el backend
                IconButton(
                    onClick = {
                        mensajeExito=""
                        actualizacion++
                    },
                    enabled = !cargando && pedidoActualizandoId == null
                ) {
                        Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = "Actualizar pedidos",
                        tint = Naranja
                    )
                }
            }
            //Mensaje de confirmación
            if (mensajeExito.isNotBlank()){
                Text(
                    text = mensajeExito,
                    color = Color(0xFF2E7D32),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Indicador de carga
            if (cargando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Naranja
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            //Mensaje de error
            if (mensajeError.isNotBlank()){
                Text(
                    text = mensajeError,
                    color = Color.Red,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            //Lista de pedidos
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                items(
                    pedidosFiltrados,
                    key = { it.pedidoId }
                ) { pedido ->

                    TarjetaPedidoCocina(
                        pedido = pedido,
                        actualizando = pedidoActualizandoId == pedido.pedidoId,
                        habilitado = pedidoActualizandoId == null,
                        onCambiarEstado = { nuevoEstado ->

                                scope.launch {
                                    pedidoActualizandoId = pedido.pedidoId
                                    mensajeError = ""
                                    mensajeExito = ""

                                    try {
                                        val respuesta =
                                            RetrofitClient.api.actualizarEstadoPedidoCocina(
                                                pedidoId = pedido.pedidoId,
                                                token = "Bearer $token",
                                                datos = ActualizarEstadoPedidoRequest(
                                                    nuevoEstado = nuevoEstado
                                                )
                                            )
                                        if(respuesta.isSuccessful){
                                            mensajeExito =
                                                respuesta.body()?.mensaje
                                                    ?: "Estado del pedido actualizado correctamente"
                                            //Volver a consultar los pedidos
                                            actualizacion++
                                        }else{

                                            mensajeError = when (respuesta.code()){
                                                400 -> "Los datos enviados no son válidos"
                                                401 -> "Tu sesión ha expirado"
                                                403 -> "No tienes permiso para actualizar pedidos"
                                                404 -> "No se encontró el pedido"
                                                409 -> "El pedido ya cambió de estado o no permite esta operación"
                                                else -> "Error al actualizar pedido (${respuesta.code()})"
                                            }
                                        }
                                    }catch (error: IOException){
                                        mensajeError =
                                            "No se pudo conectar con el servidor"
                                    }catch (error: CancellationException){
                                        throw error
                                    }catch (error: Exception){
                                        mensajeError =
                                            "Ocurrió un error al actualizar el pedido"
                                    }finally {
                                        pedidoActualizandoId = null
                                    }
                                }
                            }
                    )
                }
                //Mensaje cuando no existen pedidos
                if(!cargando &&
                    mensajeError.isBlank() &&
                    pedidosFiltrados.isEmpty()
                    ){

                    item {
                        Text(
                            text = if (pedidos.isEmpty()){
                                "Todavía no hay pedidos recibidos."
                            } else {
                                "No hay pedidos  en esta categoría."
                            },
                            color = GrisTexto,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }

                }
            }
        }
    }
}

//Tarjeta de resumen
@Composable
private fun ResumenPedido(
    cantidad: Int,
    titulo: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GrisBorde)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = cantidad.toString(),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = titulo,
                fontSize = 11.sp,
                color = GrisTexto
            )
        }
    }
}

//Tarjeta de un pedido
@Composable
private fun TarjetaPedidoCocina(
    pedido: PedidoCocina,
    actualizando: Boolean,
    habilitado: Boolean,
    onCambiarEstado: (String) -> Unit
) {

    val colorEstado = when (pedido.estado) {
        "ENVIADO" -> Amarillo
        "EN_PREPARACION" -> Naranja
        "LISTO" -> Azul
        else -> GrisTexto
    }

    val nombreEstado = when (pedido.estado){
        "ENVIADO" -> "Pendiente"
        "EN_PREPARACION" -> "En preparación"
        "LISTO" -> "Listo"
        else -> pedido.estado
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GrisBorde)
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            //Numero y estado del pedido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Pedido #${pedido.pedidoId}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Text(
                    text = nombreEstado,
                    fontSize = 12.sp,
                    color = colorEstado,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Mesa ${pedido.mesaNumero}",
                fontSize = 13.sp,
                color = GrisTexto
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = GrisBorde,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Productos reales del pedido
            pedido.productos.forEach { producto ->

                Text(
                    text = "${producto.cantidad} × ${producto.nombre}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
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
                    color = GrisTexto
                )

                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                color = GrisBorde,
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            //Total del pedido
            Text(
                text = "Total: S/ ${
                    String.format(
                        Locale.US,
                        "%.2f",
                        pedido.total
                    )
                }",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            //Acciones segun el estado
            when (pedido.estado) {

                "ENVIADO" -> {
                    Button(
                        onClick = {
                            onCambiarEstado("EN_PREPARACION")
                        },
                        enabled = habilitado,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Naranja
                        )
                    ) {
                        Text(
                            text = if (actualizando)
                                "Actualizando..."
                            else
                                "Iniciar preparación",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                "EN_PREPARACION" -> {
                    OutlinedButton(
                        onClick = {onCambiarEstado("LISTO")},
                        enabled = habilitado,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, Naranja),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Naranja
                        )
                    ) {
                        Text(
                            text = if (actualizando)
                                "Actualizando..."
                            else
                                "Marcar como listo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                "LISTO" -> {
                    Text(
                        text = "✓ Listo para recoger",
                        fontSize = 13.sp,
                        color = Azul,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}