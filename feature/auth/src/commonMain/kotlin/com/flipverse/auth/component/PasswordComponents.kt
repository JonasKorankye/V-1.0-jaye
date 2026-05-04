package com.flipverse.auth.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flipverse.auth.AuthState
import com.flipverse.shared.BlackLight
import com.flipverse.shared.FontSize
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.Strings
import com.flipverse.shared.presentation.component.FlipPasswordTextField
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.painterResource


@Composable
fun PasswordSection(
    authState: AuthState,
    onPasswordChange: (String) -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
) {
    FlipPasswordTextField(
        value = authState.password,
        onValueChange = onPasswordChange,
        label = Strings.password_label,
        isVisible = authState.isVisible,
        onVisibilityChange = onVisibilityChange,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ConfirmPasswordSection(
    confirmPasswordState: AuthState,
    onConfirmPasswordChange: (String) -> Unit,
    onVisibilityChange: (Boolean) -> Unit
) {
    FlipPasswordTextField(
        value = confirmPasswordState.confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = Strings.confirm_password_label,
        isVisible = confirmPasswordState.isConfirmVisible,
        onVisibilityChange = onVisibilityChange,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PasswordRequirements(authState: AuthState) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            PasswordRequirement("Upper Case", authState.hasUpperCase)
            PasswordRequirement("Special character", authState.hasSpecialCharacter)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            PasswordRequirement("Number", authState.hasNumber)
            PasswordRequirement("At least 6 characters", authState.hasMinLength)
        }
    }

}

@Composable
fun PasswordRequirement(
    text: String,
    isMet: Boolean,
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(if (isMet) Resources.Icon.Check else Resources.Icon.Close),
            contentDescription = null,
            tint = if (isMet) MaterialTheme.colorScheme.tertiary else Color(0xFF9E9E9E),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isMet) MaterialTheme.colorScheme.tertiary else Color(0xFF9E9E9E)
            )
        )
    }
}

@Composable
fun SubmitButton(
    enabled: Boolean,
    label: String,
    state: AuthState,
    onButtonClicked: () -> Unit,
) {

    var buttonText by rememberSaveable { mutableStateOf(label) }
    val secondaryText = Strings.please_wait

    LaunchedEffect(state.isLoading) {
        buttonText = if (state.isLoading) secondaryText else label
    }
    Button(
        onClick = onButtonClicked,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSecondary,
                shape = RoundedCornerShape(99.dp)
            )
            .height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = Color.LightGray,
        )
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(200),
                ),
//            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = state.isLoading
            ) { loadingState ->
                if (!loadingState) {
                   Unit
                } else {
                    AdaptiveCircularProgressIndicator(
                        modifier =
                            Modifier
                                .size(24.dp),
                        strokeWidth = 2.dp,
                        color = BlackLight
                    )

                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = buttonText,
                color = if(enabled) BlackLight else MaterialTheme.colorScheme.onPrimary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = FontSize.MEDIUM
            )
        }
    }
}
