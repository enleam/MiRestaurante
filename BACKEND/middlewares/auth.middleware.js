const jwt = require("jsonwebtoken");

//Verificar que el usuario tenga un token valido
function verificarToken(req, res, next) {
    //Obtener el token del encabezado Authotization
    const autorizacion = req.headers.authorization;

    if (!autorizacion) {
        return res.status(401).json({
            mensaje: "No se proporciono un token"
        });
    }

    //El formato debe ser: Bearer TOKEN
    const partes = autorizacion.split(" ");

    if (partes.length !== 2 || partes[0] !== "Bearer" || !partes[1]) {
        return res.status(401).json({
            mensaje: "Formato de token incorrecto"
        });
    }
    
    const token = partes[1];

    try {
        //Comprobar que el token sea valido
        const datos = jwt.verify(token, process.env.JWT_SECRET, {
            algorithms: ["HS256"]
        });

        //Guardar los datos del usuario
        req.usuario = datos;

        //Continuar con la siguiente funcion
        next();
    } catch (error) {
        return res.status(401).json({
            mensaje: "Token invalido o expirado"
        });
    }
}

module.exports = { verificarToken };