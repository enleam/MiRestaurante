const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");

const { buscarUsuarioPorCorreo } = require("../models/usuario.model");

//Validar las credenciales del trabajador
async function iniciarSesion(correo, password) {
    //Verificar que los datos sean validos
    if (typeof correo !== "string" || typeof password !== "string") {
        return null;
    }

    const correoLimpio = correo.trim().toLowerCase();

    if (!correoLimpio || !password) {
        return null;
    }

    //Buscar el usuario en la BD
    const usuario = await buscarUsuarioPorCorreo(correoLimpio);

    if (!usuario) {
        return null;
    }

    //Verificar que este activo
    if (usuario.estado !== "ACTIVO") {
        return null;
    }

    //Comparar la contrasena con el hash almacenado
    const passwordCorrecta = await bcrypt.compare(
        password,
        usuario.password_hash
    );

    if(!passwordCorrecta) {
        return null;
    }

    //Verificar que existe la clave secreta
    if (!process.env.JWT_SECRET) {
        throw new Error("Falta configurar JWT_SECRET");
    }

    //Generar el token de autenticacion
    const token = jwt.sign(
        {
            usuarioId: usuario.usuario_id,
            rol: usuario.rol
        },
        process.env.JWT_SECRET,
        { expiresIn: "8h" }
    );

    //Devolver los datos necesarios
    return {
        token,
        usuario: {
            usuarioId: usuario.usuario_id,
            nombres: usuario.nombres,
            correo: usuario.correo,
            rol: usuario.rol
        }
    };
}

module.exports = { iniciarSesion };