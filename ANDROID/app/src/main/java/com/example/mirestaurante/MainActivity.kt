package com.example.mirestaurante

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.mirestaurante.network.LoginRequest
import com.example.mirestaurante.network.RetrofitClient
import kotlinx.coroutines.launch
import java.io.IOException
import com.example.mirestaurante.network.LoginResponse
import com.example.mirestaurante.screens.PantallaMesas
import com.example.mirestaurante.screens.PantallaCocina

// Colores de nuestro prototipo
private val Naranja = Color(0xFFDB7C43)
private val GrisTexto = Color(0xFF777777)
private val GrisBorde = Color(0xFFE2E2E2)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Iconos oscuros en la barra de estado
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        setContent {
            MaterialTheme {

                //Guardar la sesion mientras la aplicacion está abierta
                var sesion by remember {
                    mutableStateOf<LoginResponse?>(null)
                }

                //Mostrar la pantalla segun el rol
                when (sesion?.usuario?.rol) {

                    "MOZO" -> {
                        PantallaMesas(
                            onCerrarSesion = {
                                sesion = null
                            }
                        )
                    }

                    "COCINA" -> {
                        PantallaCocina(
                            onCerrarSesion = {
                                sesion = null
                            }
                        )
                    }

                    else -> {
                        PantallaLogin(
                            onLoginExitoso = { resultado ->
                                sesion = resultado
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaLogin(onLoginExitoso: (LoginResponse) -> Unit) {
    //Datos ingresados por el trabajador
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var mostrarContrasena by remember { mutableStateOf(false) }

    //Estado del inicio de sesion
    var cargando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    //Permite ejecutar solicitudes sin bloquear la interfaz
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFC))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        //Logotipo
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(
                    color = Color(0xFFF9ECE5),
                    shape = RoundedCornerShape(22.dp)
                )
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_restaurant),
                contentDescription = "Logotipo de Mi Restaurante",
                tint = Naranja,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Mi Restaurante",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Sistema de gestión del restaurante",
            fontSize = 16.sp,
            color = GrisTexto,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(36.dp))

        //Encabezado del formulario
        Text(
            text = "Iniciar sesión",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Ingresa tus credenciales para continuar",
            fontSize = 16.sp,
            color = GrisTexto
        )

        Spacer(modifier = Modifier.height(18.dp))

        //Campo usuario
        Text(
            text = "Usuario",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                mensaje = ""
            },
            placeholder = {
                Text(
                    "Ingresa tu usuario",
                    color = GrisTexto
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = null,
                    tint = GrisTexto,
                    modifier = Modifier.size(24.dp)
                )
            },
            singleLine = true,
            enabled = !cargando,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Naranja,
                unfocusedBorderColor = GrisBorde,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        //Campo contrasena
        Text(
            text = "Contraseña",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = {
                contrasena = it
                mensaje = ""
            },
            placeholder = {
                Text(
                    "Ingresa tu contraseña",
                    color = GrisTexto
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = GrisTexto,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        mostrarContrasena = !mostrarContrasena
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (mostrarContrasena) {
                                R.drawable.ic_visibility_off
                            } else {
                                R.drawable.ic_visibility
                            }
                        ),
                        contentDescription = if (mostrarContrasena) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        },
                        tint = GrisTexto
                    )
                }
            },
            visualTransformation = if (mostrarContrasena) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            enabled = !cargando,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Naranja,
                unfocusedBorderColor = GrisBorde,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        //Boton para iniciar sesion
        Button(
            onClick = {

                if (correo.isBlank() || contrasena.isBlank()) {
                    mensaje = "Ingresa tu usuario y contraseña"
                } else {

                    cargando = true
                    mensaje = ""

                    scope.launch {
                        try {
                            //Enviar credenciales al backend
                            val datos = LoginRequest(
                                correo = correo.trim(),
                                password = contrasena
                            )

                            val respuesta = RetrofitClient.api
                                .iniciarSesion(datos)

                            if (respuesta.isSuccessful) {

                                val resultado = respuesta.body()

                                if (resultado != null) {

                                    if (resultado.usuario.rol == "MOZO" ||
                                        resultado.usuario.rol == "COCINA") {

                                        contrasena = ""

                                        //Avisar que el inicio de sesion fue exitoso
                                        onLoginExitoso(resultado)

                                    } else {
                                        mensaje = "No tienes acceso a esta aplicación"
                                    }

                                } else {
                                    mensaje = "No se recibió una respuesta válida"
                                }

                            } else if (respuesta.code() == 400) {
                                mensaje = "Verifica los datos ingresados"

                            } else if (respuesta.code() == 401) {
                                mensaje = "Usuario o contraseña incorrectos"

                            } else {
                                mensaje = "Error del servidor. Intenta nuevamente"
                            }

                        } catch (error: IOException) {
                            mensaje = "No se pudo conectar con el servidor"

                        } catch (error: Exception) {
                            mensaje = "Ocurrió un error inesperado"

                        } finally {
                            cargando = false
                        }
                    }
                }
            },
            enabled = !cargando,
            colors = ButtonDefaults.buttonColors(
                containerColor = Naranja
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {

            if (cargando) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Iniciar sesión",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        //Mensaje de resultado
        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = mensaje,
                fontSize = 14.sp,
                color = GrisTexto,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Acceso exclusivo para personal autorizado",
            fontSize = 13.sp,
            color = GrisTexto,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}