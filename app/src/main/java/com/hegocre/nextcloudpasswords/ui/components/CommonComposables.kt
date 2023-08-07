package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OutlinedTextFieldWithCaption(
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    captionText: String = "",
    errorText: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null,
    onDone: () -> Unit = {}
) {
    val isError by remember { derivedStateOf { errorText.isNotBlank() } }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            value = text,
            maxLines = 1,
            singleLine = true,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = keyboardType
            ),
            keyboardActions = KeyboardActions(onDone = {
                onDone()
                keyboardController?.hide()
            }),
            isError = isError,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon
        )

        if (captionText.isNotBlank() || isError) {
            Text(
                text = if (!isError)
                    captionText
                else
                    errorText,
                style = if (!isError)
                    MaterialTheme.typography.labelSmall
                else
                    MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.error
                    ),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Preview
@Composable
fun OutlinedTextFieldPreview() {
    NextcloudPasswordsTheme {
        OutlinedTextFieldWithCaption(
            text = "Hello World",
            onValueChange = {},
            captionText = "Caption here"
        )
    }
}

