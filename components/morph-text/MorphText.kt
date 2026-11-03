import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.pow

private fun Float.powF(n: Float): Float = toDouble().pow(n.toDouble()).toFloat()

data class MorphTextConfig(
    val blurBase: Float = 8f,
    val blurMaxDp: Float = 40f,
    val thresholdGain: Float = 18f,
    val thresholdBias: Float = -7f,
    val morphSequence: Float = 0.5f,
)

private data class MorphState(
    val textIndex: Int,
    val morphFraction: Float,
    val inCooldown: Boolean,
    val cooldownRemaining: Float,
)

@Composable
private fun rememberMorphState(
    textCount: Int,
    morphTime: Float,
    cooldownTime: Float,
): MorphState {
    var state by remember(textCount) {
        mutableStateOf(
            MorphState(
                textIndex = textCount - 1,
                morphFraction = 0f,
                inCooldown = true,
                cooldownRemaining = cooldownTime,
            )
        )
    }
    LaunchedEffect(textCount, morphTime, cooldownTime) {
        var lastMs = withFrameMillis { it }
        while (true) {
            val frameMs = withFrameMillis { it }
            val dt = (frameMs - lastMs) / 1000f
            lastMs = frameMs
            state = state.run {
                if (inCooldown) {
                    val remaining = cooldownRemaining - dt
                    if (remaining <= 0f) {
                        copy(
                            textIndex = (textIndex + 1) % textCount,
                            morphFraction = 0f,
                            inCooldown = false,
                            cooldownRemaining = 0f,
                        )
                    } else {
                        copy(cooldownRemaining = remaining)
                    }
                } else {
                    val next = (morphFraction + dt / morphTime).coerceAtMost(1f)
                    if (next >= 1f) {
                        copy(morphFraction = 1f, inCooldown = true, cooldownRemaining = cooldownTime)
                    } else {
                        copy(morphFraction = next)
                    }
                }
            }
        }
    }
    return state
}

@Composable
fun MorphText(
    texts: List<String>,
    modifier: Modifier = Modifier,
    morphTime: Float = 1f,
    cooldownTime: Float = 0.25f,
    config: MorphTextConfig = MorphTextConfig(),
    style: TextStyle = LocalTextStyle.current,
) {
    require(texts.isNotEmpty()) { "MorphText requires at least one string" }
    val state = rememberMorphState(texts.size, morphTime, cooldownTime)
    val f = if (state.inCooldown) 1f else state.morphFraction
    val text1 = texts[state.textIndex]
    val text2 = texts[(state.textIndex + 1) % texts.size]
    val thresholdFilter = remember(config.thresholdGain, config.thresholdBias) {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, config.thresholdGain, config.thresholdBias,
                )
            )
        )
    }
    val p = (config.morphSequence * 0.5f).coerceIn(0f, 0.499f)
    val denom = (1f - p).coerceAtLeast(0.001f)
    val t1 = ((1f - f) / denom).coerceIn(0f, 1f)
    val t2 = ((f - p) / denom).coerceIn(0f, 1f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.graphicsLayer { colorFilter = thresholdFilter; clip = false },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(config.blurMaxDp.dp + 8.dp),
        ) {
            Text(text = text1, style = style, modifier = Modifier.graphicsLayer {
                alpha = t1.powF(0.4f)
                val blurPx = (config.blurBase / t1.coerceAtLeast(0.001f) - config.blurBase)
                    .coerceIn(0f, config.blurMaxDp).dp.toPx()
                if (blurPx > 0f) renderEffect = BlurEffect(blurPx, blurPx, TileMode.Decal)
                clip = false
            })
            Text(text = text2, style = style, modifier = Modifier.graphicsLayer {
                alpha = t2.powF(0.4f)
                val blurPx = (config.blurBase / t2.coerceAtLeast(0.001f) - config.blurBase)
                    .coerceIn(0f, config.blurMaxDp).dp.toPx()
                if (blurPx > 0f) renderEffect = BlurEffect(blurPx, blurPx, TileMode.Decal)
                clip = false
            })
        }
    }
}
