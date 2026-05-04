package com.flipverse.shared.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.flipverse.shared.WorkSansFont


@Composable
fun FlipPrivacyPolicyLabel(
    onTermsOfUseClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "By signing up, you agree to FlipVerse's ",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontFamily = WorkSansFont()
            ),
            color = MaterialTheme.colorScheme.onSecondary,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Terms of Use",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                fontFamily = WorkSansFont(),
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable { onTermsOfUseClick() }
        )
        Text(
            text = " and ",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontFamily = WorkSansFont()
            ),
            color = MaterialTheme.colorScheme.onSecondary,
        )
        Text(
            text = "Privacy Policy.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                fontFamily = WorkSansFont(),
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable { onPrivacyPolicyClick() }
        )
    }


}