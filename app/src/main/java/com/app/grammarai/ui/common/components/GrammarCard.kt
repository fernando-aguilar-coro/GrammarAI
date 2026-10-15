package com.app.grammarai.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.grammarai.ui.common.animation.tactilePress
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard

@Composable
fun GrammarCard(
    modifier: Modifier = Modifier,
    containerColor: Color = DarkSurfaceCard,
    borderColor: Color = BorderSubtle,
    shapeRadius: Dp = 16.dp,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(shapeRadius)
    val cardModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(containerColor)
        .border(1.dp, borderColor, shape)
        .then(
            if (onClick != null) Modifier.tactilePress(onClick = onClick) else Modifier
        )
        .padding(padding)

    Box(
        modifier = cardModifier,
        content = content
    )
}
