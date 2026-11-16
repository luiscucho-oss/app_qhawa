package com.cucho.qhawa.feature.auth.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.R
import com.cucho.qhawa.core.designsystem.component.QhawaLogo
import com.cucho.qhawa.core.designsystem.component.QhawaPrimaryButton
import com.cucho.qhawa.core.designsystem.component.QhawaSecondaryButton
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.core.designsystem.theme.qhawaScreenBackground
import kotlinx.coroutines.launch

private const val WelcomePageCount = 5

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { WelcomePageCount })
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val isLastPage = pagerState.currentPage == WelcomePageCount - 1

    Scaffold(
        modifier = modifier.qhawaScreenBackground(),
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QhawaLogo(modifier = Modifier.size(52.dp))
                if (!isLastPage) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(WelcomePageCount - 1) }
                    }) {
                        Text(stringResource(R.string.welcome_skip))
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                pageSpacing = 20.dp,
                verticalAlignment = Alignment.Top,
            ) { page ->
                WelcomeSlide(page = page)
            }

            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(WelcomePageCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 22.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) colors.primary
                                else colors.outlineVariant,
                            ),
                    )
                }
            }

            if (isLastPage) {
                QhawaPrimaryButton(
                    text = stringResource(R.string.login_button),
                    onClick = onLoginClick,
                )
                Spacer(Modifier.height(10.dp))
                QhawaSecondaryButton(
                    text = stringResource(R.string.welcome_create_account),
                    onClick = onCreateAccountClick,
                )
                Spacer(Modifier.height(8.dp))
            } else {
                QhawaPrimaryButton(
                    text = stringResource(R.string.welcome_next),
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WelcomeSlide(page: Int) {
    val title = stringResource(
        when (page) {
            0 -> R.string.welcome_slide_1_title
            1 -> R.string.welcome_slide_2_title
            2 -> R.string.welcome_slide_3_title
            3 -> R.string.welcome_slide_4_title
            else -> R.string.welcome_slide_5_title
        },
    )
    val description = stringResource(
        when (page) {
            0 -> R.string.welcome_slide_1_description
            1 -> R.string.welcome_slide_2_description
            2 -> R.string.welcome_slide_3_description
            3 -> R.string.welcome_slide_4_description
            else -> R.string.welcome_slide_5_description
        },
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        // La ilustración usa el espacio disponible; el texto queda agrupado debajo.
        WelcomeSlideVisual(page = page, modifier = Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WelcomeSlideVisual(page: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = colors.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        when (page) {
            0 -> ReadingsIllustration()
            1 -> HistoryIllustration()
            2 -> HabitsIllustration()
            3 -> AdviceIllustration()
            else -> NavigationIllustration()
        }
    }
}

@Composable
private fun ReadingsIllustration() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer, colors.surface)),
        ),
    ) {
        Image(
            painter = painterResource(R.drawable.qhawa_mascot_idle),
            contentDescription = stringResource(R.string.welcome_visual_mascot),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.86f),
        )
        Row(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ReadingTag(Icons.Filled.Air, stringResource(R.string.welcome_metric_pm), Modifier.weight(1f))
            ReadingTag(Icons.Filled.Thermostat, stringResource(R.string.welcome_metric_temperature), Modifier.weight(1f))
            ReadingTag(Icons.Filled.WaterDrop, stringResource(R.string.welcome_metric_humidity), Modifier.weight(1f))
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ) {
            Text(
                text = stringResource(R.string.welcome_visual_readings_hint),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ReadingTag(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun HistoryIllustration() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(colors.secondaryContainer, colors.surface)),
        ).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    Icons.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp).size(22.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(stringResource(R.string.welcome_history_example), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.welcome_metric_pm), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Text(stringResource(R.string.welcome_history_period), modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp))
                .background(colors.surface.copy(alpha = 0.86f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(shape = CircleShape, color = colors.primaryContainer.copy(alpha = 0.72f)) {
                Icon(
                    Icons.Filled.ShowChart,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.padding(18.dp).size(132.dp),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("08:00", "12:00", "16:00", "20:00").forEach { time ->
                Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HabitsIllustration() {
    val gold = Color(0xFFFFD956)
    val ink = Color(0xFF163930)
    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF006C59), Color(0xFF004D46))),
        ).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = gold) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.padding(7.dp).size(20.dp),
                )
            }
            Text(
                stringResource(R.string.welcome_habit_example_label),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).background(
                Brush.radialGradient(listOf(Color(0xFF5DC9A1), Color.Transparent)),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.qhawa_mascot_happy),
                contentDescription = stringResource(R.string.welcome_visual_mascot),
                modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(),
            )
        }
        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF0D0), contentColor = ink) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF006C59), modifier = Modifier.size(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        stringResource(R.string.welcome_habit_example_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(R.string.welcome_habit_example_description), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AdviceIllustration() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(colors.primaryContainer, colors.surface, colors.tertiaryContainer)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 5.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = colors.tertiaryContainer) {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = colors.primary, modifier = Modifier.padding(8.dp).size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.welcome_advice_label), style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.primaryContainer) {
                        Icon(Icons.Filled.Air, contentDescription = null, tint = colors.primary, modifier = Modifier.padding(12.dp).size(32.dp))
                    }
                    Column {
                        Text(stringResource(R.string.welcome_advice_example), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.welcome_advice_action_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = colors.secondaryContainer) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.welcome_advice_done_hint), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationIllustration() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(colors.secondaryContainer, colors.primaryContainer)),
        ).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.welcome_navigation_question), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = colors.surface, tonalElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavigationItem(Icons.Filled.Home, stringResource(R.string.nav_home), selected = true)
                NavigationItem(Icons.Filled.Timeline, stringResource(R.string.nav_history), selected = false)
                NavigationItem(Icons.Filled.LocalFireDepartment, stringResource(R.string.nav_habits), selected = false)
                NavigationItem(Icons.Filled.Lightbulb, stringResource(R.string.nav_advice), selected = false)
            }
        }
    }
}

@Composable
private fun NavigationItem(icon: ImageVector, label: String, selected: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(9.dp).size(21.dp),
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    QhawaTheme {
        WelcomeScreen(onLoginClick = {}, onCreateAccountClick = {})
    }
}
