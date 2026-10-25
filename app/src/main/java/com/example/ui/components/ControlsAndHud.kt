package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.hypot

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onDirectionChange: (Float, Float) -> Unit
) {
    val maxRadius = 60.dp
    var handleOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(Color(0x66000000))
            .border(2.dp, Brush.radialGradient(listOf(JungleGreen.copy(alpha = 0.6f), Color(0x33FFFFFF))), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val drag = offset - center
                        val dist = hypot(drag.x, drag.y)
                        val maxPx = size.width / 2f
                        val clamped = if (dist > maxPx) drag * (maxPx / dist) else drag
                        handleOffset = clamped
                        onDirectionChange(clamped.x / maxPx, clamped.y / maxPx)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = handleOffset + dragAmount
                        val maxPx = size.width / 2f
                        val dist = hypot(next.x, next.y)
                        val clamped = if (dist > maxPx) next * (maxPx / dist) else next
                        handleOffset = clamped
                        onDirectionChange(clamped.x / maxPx, clamped.y / maxPx)
                    },
                    onDragEnd = {
                        handleOffset = Offset.Zero
                        onDirectionChange(0f, 0f)
                    },
                    onDragCancel = {
                        handleOffset = Offset.Zero
                        onDirectionChange(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner thumb knob
        Box(
            modifier = Modifier
                .offset(x = handleOffset.x.dp / 2.5f, y = handleOffset.y.dp / 2.5f)
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(JungleGreen, DeepForest)
                    )
                )
                .border(2.dp, GoldenReward, CircleShape)
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isCooldown: Boolean = false,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = !isCooldown,
        modifier = modifier
            .size(58.dp)
            .clip(CircleShape)
            .border(2.dp, accentColor, CircleShape)
            .testTag("action_${label.lowercase()}"),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = SurfaceDark.copy(alpha = 0.85f),
            contentColor = accentColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isCooldown) Color.Gray else accentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCooldown) Color.Gray else TextPrimary
            )
        }
    }
}

@Composable
fun SurvivalVitalBar(
    icon: ImageVector,
    current: Float,
    max: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val pct = (current / max).coerceIn(0f, 1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0x99000000))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
            .padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x44FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun InteractivePromptBadge(
    title: String,
    actionText: String,
    modifier: Modifier = Modifier,
    onInteract: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, GoldenReward, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = GoldenReward,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                Text(text = actionText, color = GoldenReward, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onInteract,
                colors = ButtonDefaults.buttonColors(containerColor = JungleGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("interact_prompt_button")
            ) {
                Text("Interact", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrehistoricDark)
            }
        }
    }
}
