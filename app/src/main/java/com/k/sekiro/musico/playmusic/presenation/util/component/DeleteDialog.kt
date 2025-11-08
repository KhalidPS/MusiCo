package com.k.sekiro.musico.playmusic.presenation.util.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.k.sekiro.musico.ui.theme.Blue
import com.k.sekiro.musico.ui.theme.Red


@Composable
fun DeleteDialog(
    modifier: Modifier = Modifier,
    title: String = "Delete Audios",
    description: String = "Are you sure that you want delete these audios ?",
    onDismissRequest: () -> Unit,
    onConfirm : () -> Unit,
    onIgnore : () -> Unit,
    additionalContent:@Composable ColumnScope.() -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = .85f))
                .padding(12.dp)

        ){
            Text(
                title,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                description,
                color = Color.White,
                textAlign = TextAlign.Center
                )

            additionalContent()

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Confirm",
                    color = Blue,
                    fontSize = 18.sp

                )
            }
            HorizontalDivider()

            TextButton(
                onClick = onIgnore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Ignore",
                    color = Red,
                    fontSize = 18.sp
                )
            }
        }
    }
}


@Preview
@Composable
private fun DeleteDialogPrev() {
    DeleteDialog(onDismissRequest = {}, onIgnore = {}, onConfirm = {})
}