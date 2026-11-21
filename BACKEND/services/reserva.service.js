
const reservaModel = require("../models/reserva.model");

// Obtener todas las reservas registradas
async function listarReservas() {

    const reservas = await reservaModel.listarReservas();

    return reservas;
}

module.exports = {
    listarReservas
};
