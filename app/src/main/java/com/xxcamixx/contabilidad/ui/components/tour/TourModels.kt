package com.xxcamixx.contabilidad.ui.components.tour

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TourZone(val prefKey: String, val displayName: String) {
    PERSONAL("personal", "Modo Personal"),
    PEDIDOS("pedidos", "Modo Pedidos"),
    TIENDA("tienda", "Modo Tienda"),
    CART_PEDIDOS("cart_pedidos", "Carrito de Ventas"),
    INVENTORY("inventory", "Inventario"),
    CHECKOUT_TIENDA("checkout_tienda", "Cobro y Despacho"),
    CALENDAR("calendar", "Calendario y Deudas"),
    CIERRES("cierres", "Cierres de Caja")
}

enum class TourTarget {
    // Modo Personal
    PROFILE,
    CALENDAR,
    MENU,
    DASHBOARD,
    FAB_ADD,
    BOTTOM_NAV,

    // Modo Pedidos
    PEDIDOS_METRICS,
    PEDIDOS_ADD_BTN,
    PEDIDOS_TABS,
    PEDIDOS_CART_FAB,

    // Modo Tienda
    TIENDA_METRICS,
    TIENDA_INVENTORY_BTN,
    TIENDA_SCANNER_BTN,
    TIENDA_CHECKOUT_BAR,

    // Carrito de Pedidos
    CART_SCAN_BTN,
    CART_SEARCH_BTN,
    CART_CUSTOMER_FIELD,
    CART_TOTAL_PAY,

    // Pantalla Inventario
    INV_SEARCH_BAR,
    INV_SCAN_BTN,
    INV_CATEGORIES_ROW,
    INV_ADD_BTN,

    // Checkout / Cobro en Tienda
    CHECKOUT_PRODUCTS_LIST,
    CHECKOUT_PAYMENT_METHODS,
    CHECKOUT_FIAR_BTN,
    CHECKOUT_CONFIRM_BTN,

    // Calendario y Deudores
    CALENDAR_PICKER,
    CALENDAR_DEUDAS_LIST,
    CALENDAR_REMINDERS_BTN,

    // Cierres de Caja
    CIERRES_AUTO_SCHEDULE,
    CIERRES_MANUAL_BTN,
    CIERRES_HISTORY_LIST
}

enum class TooltipPosition {
    AUTO,
    ABOVE,
    BELOW,
    CENTER
}

data class TourStep(
    val target: TourTarget,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val position: TooltipPosition = TooltipPosition.AUTO
)

object TourCatalog {
    fun getStepsForZone(zone: TourZone): List<TourStep> {
        return when (zone) {
            TourZone.PERSONAL -> listOf(
                TourStep(
                    target = TourTarget.PROFILE,
                    title = "Tu Perfil y Cuenta",
                    description = "Toca tu nombre para personalizar tu foto, cambiar tu nombre, consultar la tasa de cambio del día y verificar el tiempo activo de tu cuenta.",
                    icon = Icons.Filled.Person
                ),
                TourStep(
                    target = TourTarget.CALENDAR,
                    title = "Calendario y Deudores",
                    description = "Gestiona tus fechas importantes, programa recordatorios de pago y administra tu lista de personas y clientes fiados con facilidad.",
                    icon = Icons.Filled.DateRange
                ),
                TourStep(
                    target = TourTarget.MENU,
                    title = "Menú y Cierres de Caja",
                    description = "Aquí encuentras los Cierres automáticos y manuales de caja, las Opciones generales, configuración de sonidos y copias de seguridad en la nube.",
                    icon = Icons.Filled.MoreVert
                ),
                TourStep(
                    target = TourTarget.DASHBOARD,
                    title = "Resumen Financiero",
                    description = "Monitorea tu balance total, tus ingresos y tus egresos con el detalle separado de dinero en efectivo y transferencias digitales.",
                    icon = Icons.Filled.AccountBalanceWallet
                ),
                TourStep(
                    target = TourTarget.FAB_ADD,
                    title = "Registrar Movimiento",
                    description = "Usa este botón flotante cada vez que quieras registrar un nuevo ingreso o gasto personal en pocos segundos.",
                    icon = Icons.Filled.Add
                ),
                TourStep(
                    target = TourTarget.BOTTOM_NAV,
                    title = "Modos de Trabajo",
                    description = "Alterna cómodamente entre tus finanzas Personales, la gestión de Pedidos a clientes y el inventario de ventas de tu Tienda.",
                    icon = Icons.Filled.Layers
                )
            )

            TourZone.PEDIDOS -> listOf(
                TourStep(
                    target = TourTarget.PEDIDOS_METRICS,
                    title = "Panel de Pedidos 📦",
                    description = "Monitorea en tiempo real el dinero invertido, las ganancias netas y el total de ventas acumuladas en todos tus pedidos.",
                    icon = Icons.Filled.AccountBalanceWallet
                ),
                TourStep(
                    target = TourTarget.PEDIDOS_ADD_BTN,
                    title = "Crear Nuevo Pedido",
                    description = "Crea un nuevo pedido con el nombre de tu cliente o proveedor y organiza los productos que vas a despachar.",
                    icon = Icons.Filled.Add
                ),
                TourStep(
                    target = TourTarget.PEDIDOS_TABS,
                    title = "Estantes y Ventas",
                    description = "Alterna fácilmente entre los productos organizados por estante y el historial de ventas despachadas.",
                    icon = Icons.Filled.Layers
                ),
                TourStep(
                    target = TourTarget.PEDIDOS_CART_FAB,
                    title = "Carrito de Pedidos 🛒",
                    description = "Accede a tu carrito en cualquier momento para agregar cantidades, despachar pedidos y registrar pagos al instante.",
                    icon = Icons.Filled.ShoppingCart
                )
            )

            TourZone.TIENDA -> listOf(
                TourStep(
                    target = TourTarget.TIENDA_METRICS,
                    title = "Resumen de tu Tienda 🏪",
                    description = "Visualiza el valor monetario total de tu inventario en tiempo real y la ganancia neta obtenida por tus ventas.",
                    icon = Icons.Filled.AccountBalanceWallet
                ),
                TourStep(
                    target = TourTarget.TIENDA_INVENTORY_BTN,
                    title = "Inventario de Productos",
                    description = "Accede al catálogo para ver, editar y organizar todos tus artículos, precios y alertas de existencias.",
                    icon = Icons.Filled.ShoppingCart
                ),
                TourStep(
                    target = TourTarget.TIENDA_SCANNER_BTN,
                    title = "Escáner Visual IA 📸",
                    description = "¡Reconocimiento inteligente! Enfoca la cámara a tus productos y la IA los sumará automáticamente al carrito sin tocar la pantalla.",
                    icon = Icons.Filled.CameraAlt
                ),
                TourStep(
                    target = TourTarget.TIENDA_CHECKOUT_BAR,
                    title = "Historial y Cobros Rápidos",
                    description = "Consulta transacciones realizadas, clientes fiados y despacha pedidos con cálculo de vuelto inmediato.",
                    icon = Icons.Filled.Check
                )
            )

            TourZone.CART_PEDIDOS -> listOf(
                TourStep(
                    target = TourTarget.CART_SCAN_BTN,
                    title = "Escanear con Cámara IA 📸",
                    description = "Toca aquí para abrir la cámara en vivo y comparar visualmente los productos guardados para sumarlos al carrito.",
                    icon = Icons.Filled.CameraAlt
                ),
                TourStep(
                    target = TourTarget.CART_SEARCH_BTN,
                    title = "Buscar Manualmente 🔍",
                    description = "Abre el buscador rápido para ver stock disponible, subtotales en tiempo real y sumar unidades con botones + y -.",
                    icon = Icons.Filled.Search
                ),
                TourStep(
                    target = TourTarget.CART_CUSTOMER_FIELD,
                    title = "Nombre del Cliente",
                    description = "Puedes registrar opcionalmente el nombre del cliente para asignarle el pedido o factura.",
                    icon = Icons.Filled.Person
                ),
                TourStep(
                    target = TourTarget.CART_TOTAL_PAY,
                    title = "Total y Opciones de Pago",
                    description = "Revisa el monto total a pagar y pulsa Siguiente para elegir si la venta es en Efectivo, Digital, Dividida o Fiada.",
                    icon = Icons.Filled.ShoppingCart
                )
            )

            TourZone.INVENTORY -> listOf(
                TourStep(
                    target = TourTarget.INV_SEARCH_BAR,
                    title = "Buscador de Inventario",
                    description = "Escribe cualquier palabra o código para filtrar al instante los productos registrados en tu tienda.",
                    icon = Icons.Filled.Search
                ),
                TourStep(
                    target = TourTarget.INV_SCAN_BTN,
                    title = "Escaneo con Cámara IA",
                    description = "Abre el escáner inteligente directamente desde el inventario para identificar productos en segundos.",
                    icon = Icons.Filled.CameraAlt
                ),
                TourStep(
                    target = TourTarget.INV_CATEGORIES_ROW,
                    title = "Filtrar por Categorías",
                    description = "Toca cada etiqueta para clasificar tus productos por departamentos o tipos de mercancía.",
                    icon = Icons.Filled.Layers
                ),
                TourStep(
                    target = TourTarget.INV_ADD_BTN,
                    title = "Añadir Nuevo Producto",
                    description = "Toca este botón para registrar un nuevo producto con su foto, precio de costo, precio de venta y existencias.",
                    icon = Icons.Filled.Add
                )
            )

            TourZone.CHECKOUT_TIENDA -> listOf(
                TourStep(
                    target = TourTarget.CHECKOUT_PRODUCTS_LIST,
                    title = "Productos por Cobrar",
                    description = "Lista detallada de los artículos agregados al carrito con cantidades, precios unitarios y subtotales.",
                    icon = Icons.Filled.ShoppingCart
                ),
                TourStep(
                    target = TourTarget.CHECKOUT_PAYMENT_METHODS,
                    title = "Método de Pago y Vuelto",
                    description = "Ingresa el dinero recibido en Efectivo o Digital. El sistema calcula automáticamente el vuelto exacto.",
                    icon = Icons.Filled.AccountBalanceWallet
                ),
                TourStep(
                    target = TourTarget.CHECKOUT_FIAR_BTN,
                    title = "Registrar como Fiado 🗓️",
                    description = "Si el cliente pagará después, puedes guardar la deuda directamente con fecha límite y recordatorio.",
                    icon = Icons.Filled.DateRange
                ),
                TourStep(
                    target = TourTarget.CHECKOUT_CONFIRM_BTN,
                    title = "Finalizar la Venta",
                    description = "Confirma el cobro para descontar las existencias del inventario y registrar la ganancia en tus métricas.",
                    icon = Icons.Filled.Check
                )
            )

            TourZone.CALENDAR -> listOf(
                TourStep(
                    target = TourTarget.CALENDAR_PICKER,
                    title = "Calendario de Pagos",
                    description = "Visualiza en el calendario las fechas límites de deudas y cobros pendientes.",
                    icon = Icons.Filled.DateRange
                ),
                TourStep(
                    target = TourTarget.CALENDAR_DEUDAS_LIST,
                    title = "Lista de Deudores y Fiadores",
                    description = "Consulta cuánto te debe cada persona, registra abonos parciales o marca deudas como saldadas.",
                    icon = Icons.Filled.Person
                ),
                TourStep(
                    target = TourTarget.CALENDAR_REMINDERS_BTN,
                    title = "Recordatorios y Alarmas",
                    description = "Programa alertas personalizadas para que la aplicación te avise antes de que venza una cuenta.",
                    icon = Icons.Filled.Notifications
                )
            )

            TourZone.CIERRES -> listOf(
                TourStep(
                    target = TourTarget.CIERRES_AUTO_SCHEDULE,
                    title = "Cierre de Caja Automático",
                    description = "Configura la hora exacta en la que el sistema realizará el corte contable diario sin intervención manual.",
                    icon = Icons.Filled.Schedule
                ),
                TourStep(
                    target = TourTarget.CIERRES_MANUAL_BTN,
                    title = "Cierre Manual Inmediato",
                    description = "Genera un corte de caja al instante para consolidar ventas, ingresos y ganancias hasta este momento.",
                    icon = Icons.Filled.Check
                ),
                TourStep(
                    target = TourTarget.CIERRES_HISTORY_LIST,
                    title = "Historial de Cierres",
                    description = "Revisa los balances y resúmenes archivados de días y periodos anteriores.",
                    icon = Icons.Filled.Layers
                )
            )
        }
    }
}
