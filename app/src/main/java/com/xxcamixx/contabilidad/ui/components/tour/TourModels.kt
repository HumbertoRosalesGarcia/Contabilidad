package com.xxcamixx.contabilidad.ui.components.tour

import androidx.compose.ui.graphics.vector.ImageVector

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
    TIENDA_CHECKOUT_BAR
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
