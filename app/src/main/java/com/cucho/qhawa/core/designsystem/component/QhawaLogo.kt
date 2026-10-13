package com.cucho.qhawa.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.R

/**
 * Logo de Qhawa: hoja verde + nombre.
 * PROVISIONAL: cuando tengas el logo real de Stitch (res/drawable/qhawa_logo.png),
 * reemplaza el contenido de esta función por un Image(painterResource(R.drawable.qhawa_logo), ...).
 */
@Composable
fun QhawaLogo(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_leaf),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = textStyle,
            color = MaterialTheme.colorScheme.primary,
        )
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
