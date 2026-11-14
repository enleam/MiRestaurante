# API REST - Mi Restaurante

Documentación de los endpoints del backend (RNF-19).

Todas las rutas protegidas requieren el encabezado:

```
Authorization: Bearer <token>
```

El token se obtiene en `POST /api/auth/login`.

---

## Iniciar la atención de una mesa (HU04)

Abre una atención en una mesa libre, la asocia al mozo que inicia sesión y cambia la mesa a `OCUPADA`.

| Campo | Valor |
| --- | --- |
| Método | `POST` |
| Ruta | `/api/mesas/{mesaId}/atenciones` |
| Rol permitido | `MOZO` |
| Parámetro de ruta | `mesaId` (entero mayor que 0) |
| Cuerpo | No requiere. El mozo se toma del token. |

### Reglas

- Solo se puede iniciar la atención de una mesa activa con estado `LIBRE`.
- Se registra una fila en `salon.AtencionMesa` con estado `ABIERTA` y se actualiza `salon.Mesa` a `OCUPADA` en una sola transacción.
- Si dos mozos intentan abrir la misma mesa al mismo tiempo, solo uno lo consigue. La mesa se bloquea durante la transacción y el índice `UX_Atencion_MesaAbierta` impide dos atenciones abiertas en la misma mesa (RN-01).

### Respuesta exitosa: `201 Created`

```json
{
  "mensaje": "Atención iniciada correctamente",
  "atencion": {
    "atencionId": 2,
    "mesaId": 3,
    "mozoId": 1,
    "fechaApertura": "2026-11-14T05:00:00.000Z",
    "estado": "ABIERTA"
  },
  "mesa": {
    "mesaId": 3,
    "numero": 3,
    "capacidad": 4,
    "estado": "OCUPADA"
  }
}
```

`fechaApertura` se guarda en UTC.

### Errores

| Código | Mensaje | Cuándo ocurre |
| --- | --- | --- |
| 400 | El ID de la mesa no es válido | `mesaId` no es un entero positivo. |
| 401 | No se proporciono un token / Token invalido o expirado | Falta el token o ya venció. |
| 403 | No tienes permiso para acceder | El usuario no tiene rol `MOZO`. |
| 404 | Mesa no encontrada | La mesa no existe o está inactiva. |
| 409 | La mesa no está disponible | La mesa ya está `OCUPADA`. |
| 409 | La mesa ya tiene una atención abierta | Otra atención abierta ya existe para esa mesa. |
| 500 | Error interno al iniciar la atención | Error inesperado. No se guarda ningún cambio. |

### Ejemplo

```bash
curl -X POST http://localhost:3000/api/mesas/3/atenciones \
  -H "Authorization: Bearer <token>"
```
