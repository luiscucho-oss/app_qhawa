package com.cucho.qhawa.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.R

/**
 * Logo oficial de Qhawa.
 */
@Composable
fun QhawaLogo(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(76.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.qhawa_brand_mark),
                contentDescription = null,
                modifier = Modifier.size(39.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Ilustración del gato Qhawa (el monitor). Se usa en bienvenida y en las pantallas de acceso.
 * PROVISIONAL: dibujo simple. Cuando tengas la imagen real (res/drawable/qhawa_mascot.png),
 * cambia R.drawable.qhawa_mascot_placeholder por R.drawable.qhawa_mascot.
 * Hay que darle un tamaño con el modifier (por ejemplo, fillMaxWidth().height(220.dp)).
 */
@Composable
fun QhawaMascot(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.qhawa_mascot_placeholder),
        contentDescription = stringResource(R.string.mascot_description),
        modifier = modifier.clip(MaterialTheme.shapes.extraLarge),
        contentScale = ContentScale.Crop,
    )
}
