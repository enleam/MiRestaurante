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

//HU04: iniciar la atencion de una mesa libre
//Todo se hace en una transaccion: si algo falla, no queda nada a medias
async function iniciarAtencion(mesaId, mozoId) {
    const transaccion = new sql.Transaction();
    await transaccion.begin();

    try {
        //Leer la mesa y bloquearla hasta terminar la transaccion,
        //para que dos mozos no puedan abrir la misma mesa a la vez
        const resultadoMesa = await new sql.Request(transaccion)
            .input("mesaId", sql.Int, mesaId)
            .query(`
                SELECT
                    mesa_id AS mesaId,
                    numero,
                    capacidad,
                    estado
                FROM salon.Mesa WITH (UPDLOCK, HOLDLOCK)
                WHERE mesa_id = @mesaId
                    AND activa = 1
            `);

        const mesa = resultadoMesa.recordset[0];

        //La mesa no existe o esta inactiva
        if (!mesa) {
            await transaccion.rollback();
            return { resultado: "NO_ENCONTRADA" };
        }

        //Solo se puede iniciar la atencion de una mesa LIBRE
        if (mesa.estado !== "LIBRE") {
            await transaccion.rollback();
            return { resultado: "NO_DISPONIBLE", mesa };
        }

        //Registrar la atencion asociada al mozo
        const resultadoAtencion = await new sql.Request(transaccion)
            .input("mesaId", sql.Int, mesaId)
            .input("mozoId", sql.Int, mozoId)
            .query(`
                INSERT INTO salon.AtencionMesa (mesa_id, mozo_id)
                OUTPUT
                    INSERTED.atencion_id AS atencionId,
                    INSERTED.mesa_id AS mesaId,
                    INSERTED.mozo_id AS mozoId,
                    INSERTED.fecha_apertura AS fechaApertura,
                    INSERTED.estado AS estado
                VALUES (@mesaId, @mozoId)
            `);

        //Cambiar la mesa a OCUPADA
        await new sql.Request(transaccion)
            .input("mesaId", sql.Int, mesaId)
            .query(`
                UPDATE salon.Mesa
                SET estado = N'OCUPADA'
                WHERE mesa_id = @mesaId
            `);

        await transaccion.commit();

        return {
            resultado: "INICIADA",
            atencion: resultadoAtencion.recordset[0],
            mesa: { ...mesa, estado: "OCUPADA" }
        };
    } catch (error) {
        //Deshacer los cambios si la transaccion sigue abierta
        try {
            await transaccion.rollback();
        } catch (errorRollback) {
            //La transaccion ya fue cancelada por SQL Server
        }
        throw error;
    }
}

module.exports = {
    listarMesas,
    obtenerMesaPorId,
    iniciarAtencion
};