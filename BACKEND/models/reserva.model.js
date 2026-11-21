
const sql = require("mssql");

// Consultar las reservas registradas
async function listarReservas() {

    const resultado = await sql.query(`
        SELECT
            r.reserva_id AS reservaId,
            c.nombre AS clienteNombre,
            c.telefono AS clienteTelefono,
            m.mesa_id AS mesaId,
            m.numero AS mesaNumero,
            r.fecha_hora AS fechaHora,
            r.cantidad_personas AS cantidadPersonas,
            r.estado,
            r.observacion
        FROM clientes.Reserva AS r
        INNER JOIN clientes.Cliente AS c
            ON r.cliente_id = c.cliente_id
        INNER JOIN salon.Mesa AS m
            ON r.mesa_id = m.mesa_id
        ORDER BY r.fecha_hora ASC, m.numero ASC
    `);

    return resultado.recordset;
}

module.exports = {
    listarReservas
};
