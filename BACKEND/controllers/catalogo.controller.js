const catalogoService = require("../services/catalogo.service");

// Obtener las categorias activas
async function listarCategorias(req, res) {
    try {
        const categorias = await catalogoService.listarCategorias();

        return res.status(200).json({
            mensaje: "Categorías obtenidas correctamente",
            categorias
        });

    } catch (error) {
        console.error("Error al listar categorías:", error);

        return res.status(500).json({
            mensaje: "Error al obtener las categorías"
        });
    }
}

// Obtener los productos de la carta
async function listarProductos(req, res) {
    try {
        const productos = await catalogoService.listarProductos();

        return res.status(200).json({
            mensaje: "Productos obtenidos correctamente",
            productos
        });

    } catch (error) {
        console.error("Error al listar productos:", error);

        return res.status(500).json({
            mensaje: "Error al obtener los productos"
        });
    }
}

module.exports = {
    listarCategorias,
    listarProductos
};
