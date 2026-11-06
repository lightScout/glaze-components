import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** How the dots move. */
enum class GooeyDotsStyle(internal val cycleSeconds: Float) {
    /** A pulse travels along a row of five dots, pulling each neighbour as it passes. */
    BeadChain(1.25f),

    /** Five dots circle, merging into a ring and spreading back out. */
    Orbit(3.2f),

    /** One blob splits into three, then pulls back together. */
    SplitJoin(1.67f),
}

/** One dot, in units of the dot radius, relative to the centre of the loader. */
internal data class GooeyDot(val x: Float, val y: Float, val radius: Float)

// The area the dots stay inside, in units of the dot radius.
internal const val GOOEY_HALF_WIDTH = 8f
internal const val GOOEY_HALF_HEIGHT = 4f

/** Where every dot is at [progress] through a cycle, from 0 to 1. A cycle ends where it began. */
internal fun gooeyDotLayout(style: GooeyDotsStyle, progress: Float): List<GooeyDot> = when (style) {
    GooeyDotsStyle.BeadChain -> List(5) { i ->
        val phase = (progress - i * 0.13f + 1f) % 1f
        val pull = sin(min(phase / 0.4f, 1f) * PI.toFloat())
        val gap = 2.5f
        GooeyDot(x = (i - 2) * gap + pull * gap * 0.45f, y = -pull * 1.5f, radius = 1f + pull * 0.15f)
    }
    GooeyDotsStyle.Orbit -> List(5) { i ->
        val turn = 2f * PI.toFloat()
        val angle = progress * turn + i * turn / 5f
        val spread = 1.2f + 2f * (sin(progress * turn) + 1f) / 2f
        GooeyDot(x = spread * cos(angle), y = spread * sin(angle), radius = 0.8f)
    }
    GooeyDotsStyle.SplitJoin -> {
        val half = if (progress < 0.5f) easeInOut(progress * 2f) else 1f - easeInOut((progress - 0.5f) * 2f)
        List(3) { i ->
            val side = i - 1
            GooeyDot(
                x = side * half * 3.4f,
                y = -abs(side) * sin(half * PI.toFloat()) * 0.7f,
                radius = if (side == 0) 1.5f - half * 0.5f else 0.5f + half * 0.5f,
            )
        }
    }
}

private fun easeInOut(x: Float): Float = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f

/**
 * A loader whose dots melt into each other. The dots are blurred, then a threshold filter snaps the
 * soft edges back to sharp, so nearby dots share one outline.
 *
 * Blur needs Android 12 or later. On older Android versions the dots still move but don't merge.
 *
 * @param dotRadius The size of a dot. The loader takes up 16 of these across and 8 down, plus [goo].
 * @param goo       The blur radius. Higher values make dots merge from further apart.
 * @param speed     Multiplies the speed of the animation.
 */
@Composable
fun GooeyDots(
    modifier: Modifier = Modifier,
    style: GooeyDotsStyle = GooeyDotsStyle.BeadChain,
    color: Color = Color(0xFFE8430A),
    dotRadius: Dp = 13.dp,
    goo: Dp = 6.dp,
    speed: Float = 1f,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val currentStyle by rememberUpdatedState(style)
    val currentSpeed by rememberUpdatedState(speed)
    LaunchedEffect(Unit) {
        var last = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                progress = (progress + (now - last) / 1000f * currentSpeed / currentStyle.cycleSeconds) % 1f
                last = now
            }
        }
    }

    // Filters alpha only: A' = 24A - 10. Colour passes through unchanged.
    val threshold = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 24f, -2550f,
                ),
            ),
        )
    }

    // Room around the dots, or the blur is cut off at the edge of the layer.
    val margin = goo * 2
    Canvas(
        modifier = modifier
            .size(dotRadius * GOOEY_HALF_WIDTH * 2 + margin * 2, dotRadius * GOOEY_HALF_HEIGHT * 2 + margin * 2)
            .semantics { contentDescription = "Loading" }
            .graphicsLayer { colorFilter = threshold }
            .graphicsLayer {
                val blur = goo.toPx()
                if (blur > 0f) renderEffect = BlurEffect(blur, blur, TileMode.Decal)
            },
    ) {
        val unit = dotRadius.toPx()
        gooeyDotLayout(style, progress).forEach { dot ->
            drawCircle(
                color = color,
                radius = dot.radius * unit,
                center = Offset(center.x + dot.x * unit, center.y + dot.y * unit),
            )
        }
    }
}
