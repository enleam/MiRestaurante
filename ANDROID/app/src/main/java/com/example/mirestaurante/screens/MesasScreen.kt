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

//Colores del prototipo
private val Naranja = Color(0xFFDB7C43)
private val Verde = Color(0xFF36866B)
private val Azul = Color(0xFF5D83A7)
private val GrisBorde = Color(0xFFE3E3E3)
private val GrisTexto = Color(0xFF777777)

//Datos para mostrar las mesas
private data class MesaDemo(
    val numero: Int,
    val estado: String
)

@Composable
fun PantallaMesas(onCerrarSesion: () -> Unit) {

    var filtro by rememberSaveable {
        mutableStateOf("Todas")
    }

    //Controlar si el menu de usuario esta abierto
        var menuUsuarioAbierto by remember {
            mutableStateOf(false)
        }

    //Mesas de ejemplo segun nuestro prototipo
    val mesas = remember {
        listOf(
            MesaDemo(1, "Libre"),
            MesaDemo(2, "Ocupada"),
            MesaDemo(3, "Reservada"),
            MesaDemo(4, "Ocupada"),
            MesaDemo(5, "Libre"),
            MesaDemo(6, "Reservada"),
            MesaDemo(7, "Ocupada"),
            MesaDemo(8, "Libre")
        )
    }

    //Aplicar el filtro seleccionado
    val mesasFiltradas = mesas.filter { mesa ->
        when (filtro) {
            "Libres" -> mesa.estado == "Libre"
            "Ocupadas" -> mesa.estado == "Ocupada"
            "Reservadas" -> mesa.estado == "Reservada"
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

            Spacer(modifier = Modifier.height(18.dp))

            //Contadores de mesas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                val resumen = listOf(
                    Triple(
                        "Libres",
                        mesas.count { it.estado == "Libre" },
                        Verde
                    ),
                    Triple(
                        "Ocupadas",
                        mesas.count { it.estado == "Ocupada" },
                        Naranja
                    ),
                    Triple(
                        "Reservadas",
                        mesas.count { it.estado == "Reservada" },
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
                    key = { it.numero }
                ) { mesa ->

                    TarjetaMesa(mesa)
                }
            }
        }
    }
}

//Tarjeta individual de mesa
@Composable
private fun TarjetaMesa(mesa: MesaDemo) {

    val colorEstado = when (mesa.estado) {
        "Libre" -> Verde
        "Ocupada" -> Naranja
        else -> Azul
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp),
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
                    text = mesa.estado,
                    color = colorEstado,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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