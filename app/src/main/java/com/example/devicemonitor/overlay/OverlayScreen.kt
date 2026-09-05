package com.example.devicemonitor.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row

@Composable
fun OverlayReadoutView(
    fps: Int,
    batteryTemp: Float?,
    batteryPercent: Int,
    appName: String,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$fps fps",
            color = Color.White,
            fontSize = 10.sp
        )
        Text(text = "  •  ", color = Color(0x88FFFFFF))
        Text(
            text = if (batteryTemp != null) "%.1f°C".format(batteryTemp) else "—",
            color = Color.White,
            fontSize = 10.sp
        )
        Text(text = "  •  ", color = Color(0x88FFFFFF))
        Text(
            text = "$batteryPercent%",
            color = Color.White,
            fontSize = 10.sp
        )
        Text(text = "  •  ", color = Color(0x88FFFFFF))
        Text(
            text = appName,
            color = Color.White,
            fontSize = 10.sp
        )
    }
}