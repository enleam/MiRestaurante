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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mirestaurante.R

//Colores del prototipo
private val Naranja = Color(0xFFDB7C43)
private val Amarillo = Color(0xFFD59A47)
private val Azul = Color(0xFF5D83A7)
private val GrisTexto = Color(0xFF777777)
private val GrisBorde = Color(0xFFE3E3E3)

//Modelo sencillo de pedido para el prototipo
private data class PedidoEjemplo(
    val numero: String,
    val mesa: String,
    val productos: List<String>,
    val estado: String
)

//Datos de ejemplo para mostrar la interfaz
private fun obtenerPedidosEjemplo(): List<PedidoEjemplo> {
    return listOf(
        PedidoEjemplo(
            "005",
            "Mesa 04",
            listOf("2 × Lomo saltado", "1 × Ají de gallina"),
            "Pendiente"
        ),
        PedidoEjemplo(
            "006",
            "Mesa 07",
            listOf("1 × Arroz con pollo", "2 × Chicha morada"),
            "Pendiente"
        ),
        PedidoEjemplo(
            "007",
            "Mesa 04",
            listOf("1 × Lomo saltado", "1 × Gaseosa"),
            "En preparación"
        ),
        PedidoEjemplo(
            "008",
            "Mesa 07",
            listOf("2 × Ají de gallina"),
            "Listo"
        )
    )
}

@Composable
fun PantallaCocina(onCerrarSesion: () -> Unit) {

    var filtro by rememberSaveable {
        mutableStateOf("Todos")
    }

    var menuAbierto by remember {
        mutableStateOf(false)
    }

    //Los cambios son unicamente locales por ahora
    var pedidos by remember {
        mutableStateOf(obtenerPedidosEjemplo())
    }

    //Cantidades segun el estado
    val pendientes = pedidos.count {
        it.estado == "Pendiente"
    }

    val enPreparacion = pedidos.count {
        it.estado == "En preparación"
    }

    val listos = pedidos.count {
        it.estado == "Listo"
    }

    //Filtrar pedidos
    val pedidosFiltrados = pedidos.filter { pedido ->
        when (filtro) {
            "Pendientes" -> pedido.estado == "Pendiente"
            "En preparación" -> pedido.estado == "En preparación"
            "Listos" -> pedido.estado == "Listo"
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

                //Icono referencial de actualización
                //Se conectará con la API en el Sprint 4
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                    tint = Naranja,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            //Lista de pedidos
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                items(
                    pedidosFiltrados,
                    key = { it.numero }
                ) { pedido ->

                    TarjetaPedidoCocina(
                        pedido = pedido,
                        onCambiarEstado = {

                            //Simulacion local de cambio de estado
                            pedidos = pedidos.map { actual ->

                                if (actual.numero == pedido.numero) {

                                    val nuevoEstado = when (actual.estado) {
                                        "Pendiente" -> "En preparación"
                                        "En preparación" -> "Listo"
                                        else -> actual.estado
                                    }

                                    actual.copy(estado = nuevoEstado)

                                } else {
                                    actual
                                }
                            }
                        }
                    )
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
    pedido: PedidoEjemplo,
    onCambiarEstado: () -> Unit
) {

    val colorEstado = when (pedido.estado) {
        "Pendiente" -> Amarillo
        "En preparación" -> Naranja
        else -> Azul
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
                    text = "Pedido #${pedido.numero}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Text(
                    text = pedido.estado,
                    fontSize = 12.sp,
                    color = colorEstado,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = pedido.mesa,
                fontSize = 13.sp,
                color = GrisTexto
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = GrisBorde,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Productos del pedido
            pedido.productos.forEach { producto ->

                Text(
                    text = producto,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            //Acciones segun el estado
            when (pedido.estado) {

                "Pendiente" -> {
                    Button(
                        onClick = onCambiarEstado,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Naranja
                        )
                    ) {
                        Text(
                            text = "Iniciar preparación",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                "En preparación" -> {
                    OutlinedButton(
                        onClick = onCambiarEstado,
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
                            text = "Marcar como listo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                "Listo" -> {
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