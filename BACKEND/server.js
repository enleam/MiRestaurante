require("dotenv").config();

const express = require("express");
const { conectarBD } = require("./config/db");
const authRoutes = require("./routes/auth.routes");

const app = express();
const PORT = process.env.PORT || 3000;

//Permitr recibir datos en formato JSON
app.use(express.json());

//Ruta para comprobar que la API funciona
app.get("/api/health", (req,res) => {
    res.status(200).json({
        mensaje: "Api de MiRestaurante funcionando correctamente"
    });
});

//Rutas de autenticacion
app.use("/api/auth", authRoutes);

//Iniciar el servidor
async function iniciarServidor() {
    try {
        //Conectar con SQL server
        await conectarBD();
        
        //Iniciar express
        app.listen(PORT, () => {
            console.log(`Servidor ejecutandose en http://localhost:${PORT}`);
        });
    } catch (error) {
        console.error("No se pudo iniciar el servidor:", error.message);
        process.exit(1);
    }
}

iniciarServidor();