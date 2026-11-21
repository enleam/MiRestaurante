
const reservaService = require("../services/reserva.service");

// Consultar todas las reservas registradas
async function listarReservas(req, res) {
    try {
        const reservas = await reservaService.listarReservas();

        return res.status(200).json({
            mensaje: "Reservas obtenidas correctamente",
            reservas
        });

    } catch (error) {
        console.error("Error al listar reservas:", error);

        return res.status(500).json({
            mensaje: "Error al obtener las reservas"
        });
    }
}

module.exports = {
    listarReservas
};
