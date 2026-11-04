const sql = require("mssql");

//configuracion de la conexion a sql server
const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER,
    database: process.env.DB_DATABASE,
    port: Number(process.env.DB_PORT),

    options: {
        encrypt: true,
        trustServerCertificate: true
    }
};

//funcion para conectar con la base de datos
async function conectarBD() {
    try {
        const pool = await sql.connect(dbConfig);
        console.log("Conexion exitosa a sql server")
        return pool;
    } catch(error) {
        console.error("Error al conectar con sql server:", error.message);
        throw error;
    }
}

module.exports = { sql, conectarBD };