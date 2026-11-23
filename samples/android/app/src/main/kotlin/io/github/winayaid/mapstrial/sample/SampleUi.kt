package io.github.winayaid.mapstrial.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared look for the sample screens. Plain Compose foundation; no design-system dependency. */
internal object SampleColors {
    val Accent = Color(0xFF00796B)
    val Text = Color(0xFF202124)
    val Secondary = Color(0xFF5F6368)
    val Panel = Color(0xEBFFFFFF)
}

/**
 * A full-screen map with an overlay panel at the top. The panel leaves room on the right for the
 * map's compass (top-right) and stays clear of the system bars.
 */
@Composable
internal fun MapScreen(
    map: @Composable BoxScope.() -> Unit,
    panel: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        map()
        Column(
            Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(start = 12.dp, top = 12.dp, end = COMPASS_CLEARANCE)
                .fillMaxWidth()
                .background(SampleColors.Panel, RoundedCornerShape(8.dp))
                .padding(12.dp),
            content = panel,
        )
    }
}

@Composable
internal fun StatusText(
    text: String,
    secondary: Boolean = false,
) {
    BasicText(
        text,
        style =
            TextStyle(
                fontSize = if (secondary) 13.sp else 15.sp,
                color = if (secondary) SampleColors.Secondary else SampleColors.Text,
            ),
    )
}

@Composable
internal fun ButtonRow(content: @Composable () -> Unit) {
    FlowRow(
        Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
internal fun SampleButton(
    label: String,
    onClick: () -> Unit,
) {
    BasicText(
        label,
        style = TextStyle(fontSize = 14.sp, color = Color.White),
        modifier =
            Modifier
                .background(SampleColors.Accent, RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private val COMPASS_CLEARANCE = 64.dp
