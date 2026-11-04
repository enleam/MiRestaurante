//Verificar que el usaurio tenga un rol permitido
function permitirRoles(...rolesPermitidos) {
    return (req, res, next) => {
        //Obtener el usuario autenticado
        const usuario = req.usuario;

        if (!usuario) {
            return res.status(401).json({
                mensaje: "Usuario no autenticado"
            });
        }

        //Verificar si el rol tiene permiso
        if (!rolesPermitidos.includes(usuario.rol)) {
            return res.status(403).json({
                mensaje: "No tienes permiso para acceder"
            });
        }

        //Permitir continuar con la solicitud
        next();
    };
}

module.exports = { permitirRoles };