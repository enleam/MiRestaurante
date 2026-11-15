const sql = require("mssql");

//obtener mesas activas
async function listarMesas() {
    const resultado = await sql.query(`
        SELECT
            mesa_id AS mesaId,
            numero,
            capacidad,
            estado
        FROM salon.Mesa
        WHERE activa = 1
        ORDER BY numero
    `);

    return resultado.recordset;
}

//obtener mesa por id
async function obtenerMesaPorId(mesaId) {
    const resultado = await new sql.Request()
        .input("mesaId", sql.Int, mesaId)
        .query(`
            SELECT
                mesa_id AS mesaId,
                numero,
                capacidad,
                estado
            FROM salon.Mesa
            WHERE mesa_id = @mesaId
                AND activa = 1
        `);
    return resultado.recordset[0] || null;
}

module.exports = {
    listarMesas,
    obtenerMesaPorId
};