package org.cf0x.rustnithm.Page

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TouchDebugOverlay(
    airCount: Int,
    slideCount: Int,
    totalPoints: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "{ air: $airCount | slide: $slideCount | p: $totalPoints }",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(4.dp)
        )
    }
}