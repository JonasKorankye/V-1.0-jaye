package com.flipverse.shared.domain

import androidx.compose.ui.graphics.painter.Painter

data class FabItem(val text: String, val icon: Painter, var onClick: () -> Unit)
