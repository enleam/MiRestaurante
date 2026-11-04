const { iniciarSesion } = require("../services/auth.service");

//Controlador para iniciar sesion
async function login(req, res) {
    try {
        //Obtener los datos enviados por el usuario
        const { correo, password } = req.body;

        //Validar que los campos esten completos
        if (
            typeof correo !== "string" ||
            typeof password !== "string" ||
            !correo.trim() ||
            !password
        ) {
            return res.status(400).json({
                mensaje: "El correo y la contrasena son obligatorios"
            });
        }

        //Validar las credenciales
        const resultado = await iniciarSesion(correo, password);

        //Si las credenciales son incorrectas
        if (!resultado) {
            return res.status(401).json({
                mensaje: "Credenciales incorrectas o usuario inactivo"
            });
        }

        //Inicio de sesion exitoso
        return res.status(200).json({
            mensaje: "Inicio de sesion exitoso",
            token: resultado.token,
            usuario: resultado.usuario
        });
    } catch (error) {
        console.error("Error al iniciar sesion:", error.message);

        return res.status(500).json({
            mensaje: "Error interno del servidor"
        });
    }
}

module.exports = { login };