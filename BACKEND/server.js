require("dotenv").config();

const express = require("express");
const { conectarBD } = require("./config/db");

const authRoutes = require("./routes/auth.routes");
const mesaRoutes = require("./routes/mesa.routes");
const atencionRoutes = require("./routes/atencion.routes");

const app = express();
const PORT = process.env.PORT || 3000;

//Permitir recibir datos en formato JSON
app.use(express.json());

//Ruta para comprobar que la API funciona
app.get("/api/health", (req, res) => {
    res.status(200).json({
        mensaje: "Api de MiRestaurante funcionando correctamente"
    });
});

//Rutas de autenticacion
app.use("/api/auth", authRoutes);

//Rutas de mesas
app.use("/api/mesas", mesaRoutes);

//Rutas de atenciones
app.use("/api/atenciones", atencionRoutes);

// Iniciar el servidor
async function iniciarServidor() {
    try {
        //Conectar con SQL Server
        await conectarBD();

        //Iniciar Express
        app.listen(PORT, () => {
            console.log(`Servidor ejecutandose en http://localhost:${PORT}`);
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