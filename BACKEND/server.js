
require("dotenv").config();

const express = require("express");
const { conectarBD } = require("./config/db");

const authRoutes = require("./routes/auth.routes");
const mesaRoutes = require("./routes/mesa.routes");
const atencionRoutes = require("./routes/atencion.routes");
const catalogoRoutes = require("./routes/catalogo.routes");
const reservaRoutes = require("./routes/reserva.routes");

const app = express();
const PORT = process.env.PORT || 3000;

// Permitir recibir datos en formato JSON
app.use(express.json());

// Verificar que la API funciona
app.get("/api/health", (req, res) => {
    res.status(200).json({
        mensaje: "Api de MiRestaurante funcionando correctamente"
    });
});

// Sprint 1: Autenticacion
app.use("/api/auth", authRoutes);

// HU03: Consultar mesas
app.use("/api/mesas", mesaRoutes);

// HU04: Iniciar atencion
app.use("/api/atenciones", atencionRoutes);

// HU05: Consultar carta
app.use("/api/catalogo", catalogoRoutes);

// HU06: Consultar reservas
app.use("/api/reservas", reservaRoutes);

// Iniciar el servidor
async function iniciarServidor() {
    try {
        await conectarBD();

        app.listen(PORT, () => {
            console.log(
                `Servidor ejecutandose en http://localhost:${PORT}`
            );
        });

    } catch (error) {
        console.error(
            "No se pudo iniciar el servidor:",
            error.message
        );

        process.exit(1);
    }
}

iniciarServidor();
