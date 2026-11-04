const { sql, conectarBD } = require("../config/db")

//Buscar un usuario por su correo
async function buscarUsuarioPorCorreo(correo) {
    const pool = await conectarBD();

    const resultado = await pool.request()
        .input("correo", sql.NVarChar(150), correo)
        .query(`
            SELECT 
                u.usuario_id,
                u.nombres,
                u.correo,
                u.password_hash,
                u.estado,
                r.nombre AS rol
            FROM seguridad.Usuario u
            INNER JOIN seguridad.Rol r
                ON u.rol_id = r.rol_id
            WHERE u.correo = @correo
        `);
    
    return resultado.recordset[0] || null;
}

module.exports = { buscarUsuarioPorCorreo };