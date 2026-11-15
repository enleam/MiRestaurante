const catalogoModel = require("../models/catalogo.model");

// Obtener todas las categorias activas
async function listarCategorias() {
    const categorias = await catalogoModel.listarCategorias();

    return categorias;
}

// Obtener todos los productos de la carta
async function listarProductos() {
    const productos = await catalogoModel.listarProductos();

    return productos;
}

module.exports = {
    listarCategorias,
    listarProductos
};
