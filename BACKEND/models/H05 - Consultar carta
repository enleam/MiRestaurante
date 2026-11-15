const sql = require("mssql");

// Obtener las categorias activas
async function listarCategorias() {
    const resultado = await sql.query(`
        SELECT
            categoria_id AS categoriaId,
            nombre
        FROM catalogo.Categoria
        WHERE activa = 1
        ORDER BY nombre
    `);

    return resultado.recordset;
}

// Obtener los productos de la carta
async function listarProductos() {
    const resultado = await sql.query(`
        SELECT
            p.producto_id AS productoId,
            p.categoria_id AS categoriaId,
            p.nombre,
            p.descripcion,
            p.precio,
            p.disponible,
            c.nombre AS categoria
        FROM catalogo.Producto AS p
        INNER JOIN catalogo.Categoria AS c
            ON p.categoria_id = c.categoria_id
        WHERE c.activa = 1
        ORDER BY c.nombre, p.nombre
    `);

    return resultado.recordset;
}

module.exports = {
    listarCategorias,
    listarProductos
};
