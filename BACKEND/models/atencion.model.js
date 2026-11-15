const sql = require("mssql");

//Iniciar la atencion de una mesa libre
async function iniciarAtencion(mesaId, mozoId) {
    const transaccion = new sql.Transaction();
    let transaccionActiva = false;

    try {
        await transaccion.begin();
        transaccionActiva = true;

        //Ocupar la mesa solamente si esta libre
        const mesa = await new sql.Request(transaccion)
            .input("mesaId", sql.Int, mesaId)
            .query(`
                UPDATE salon.Mesa
                SET estado = N'OCUPADA'
                OUTPUT INSERTED.mesa_id
                WHERE mesa_id = @mesaId
                  AND activa = 1
                  AND estado = N'LIBRE'
            `);

        //Si no se actualizo, no se puede iniciar
        if (mesa.recordset.length === 0) {
            await transaccion.rollback();
            transaccionActiva = false;
            return null;
        }

        //Registrar la atencion del mozo
        const resultado = await new sql.Request(transaccion)
            .input("mesaId", sql.Int, mesaId)
            .input("mozoId", sql.Int, mozoId)
            .query(`
                INSERT INTO salon.AtencionMesa (
                    mesa_id,
                    mozo_id
                )
                OUTPUT
                    INSERTED.atencion_id AS atencionId,
                    INSERTED.mesa_id AS mesaId,
                    INSERTED.mozo_id AS mozoId,
                    INSERTED.fecha_apertura AS fechaApertura,
                    INSERTED.estado
                VALUES (
                    @mesaId,
                    @mozoId
                )
            `);

        //Confirmar ambos cambios
        await transaccion.commit();
        transaccionActiva = false;

        return resultado.recordset[0];

    } catch (error) {
        if (transaccionActiva) {
            try {
                await transaccion.rollback();
            } catch (rollbackError) {
                console.error("Error al revertir:", rollbackError);
            }
        }

        throw error;
    }
}

module.exports = {
    iniciarAtencion
};