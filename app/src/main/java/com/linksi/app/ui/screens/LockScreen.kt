package com.linksi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.linksi.app.R
import com.linksi.app.utils.SecurityManager
import com.linksi.app.utils.SECURITY_FAILED_ATTEMPTS
import com.linksi.app.utils.SECURITY_LOCKOUT_UNTIL
import com.linksi.app.utils.dataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    verifyPin: (String) -> Boolean,
    isBiometricEnabled: Boolean,
    onUnlock: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Brute-force protection: after 5 wrong attempts the numpad locks for an
    // escalating delay (30s doubling, capped at 15 minutes). Persisted so
    // force-quitting the app doesn't reset the counter.
    val lockoutPrefs by remember {
        context.dataStore.data.map { prefs ->
            Pair(
                prefs[SECURITY_FAILED_ATTEMPTS] ?: 0,
                prefs[SECURITY_LOCKOUT_UNTIL] ?: 0L
            )
        }
    }.collectAsState(initial = Pair(0, 0L))
    val (failedAttempts, lockoutUntil) = lockoutPrefs
    val now by produceState(System.currentTimeMillis()) {
        while (lockoutUntil > value) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }
    val lockedOut = lockoutUntil > now
    val lockoutSecondsLeft = ((lockoutUntil - now) / 1000L).coerceAtLeast(0)

    fun recordFailure() {
        val attempts = failedAttempts + 1
        val delayMs = if (attempts >= 5) {
            (30_000L shl ((attempts - 5).coerceAtMost(5))).coerceAtMost(15 * 60_000L)
        } else 0L
        scope.launch {
            context.dataStore.edit {
                it[SECURITY_FAILED_ATTEMPTS] = attempts
                if (delayMs > 0) {
                    it[SECURITY_LOCKOUT_UNTIL] = System.currentTimeMillis() + delayMs
                }
            }
        }
    }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            if (verifyPin(enteredPin)) {
                scope.launch {
                    context.dataStore.edit {
                        it[SECURITY_FAILED_ATTEMPTS] = 0
                        it[SECURITY_LOCKOUT_UNTIL] = 0L
                    }
                }
                onUnlock()
            } else {
                error = true
                delay(500)
                enteredPin = ""
                error = false
                recordFailure()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isBiometricEnabled && !lockedOut && SecurityManager.canUseBiometric(context)) {
            SecurityManager.showBiometricPrompt(
                activity = context as FragmentActivity,
                title = context.getString(R.string.unlock),
                subtitle = context.getString(R.string.authenticate),
                onSuccess = onUnlock,
                onError = { }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background gradient element
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Expressive Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.app_locked),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = if (lockedOut) {
                        stringResource(R.string.lockout_message, lockoutSecondsLeft)
                    } else {
                        stringResource(R.string.enter_pin_to_unlock)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (lockedOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Expressive PIN Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isFilled,
                                enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            if (error) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Numpad
            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val numpad = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("biometric", "0", "backspace")
                )

                numpad.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { item ->
                            when (item) {
                                "biometric" -> {
                                    if (isBiometricEnabled && !lockedOut && SecurityManager.canUseBiometric(context)) {
                                        NumpadIconButton(
                                            onClick = {
                                                SecurityManager.showBiometricPrompt(
                                                    activity = context as FragmentActivity,
                                                    title = context.getString(R.string.unlock),
                                                    subtitle = context.getString(R.string.authenticate),
                                                    onSuccess = onUnlock,
                                                    onError = { }
                                                )
                                            },
                                            icon = Icons.Outlined.Fingerprint,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(72.dp))
                                    }
                                }
                                "backspace" -> {
                                    NumpadIconButton(
                                        onClick = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) },
                                        icon = Icons.Outlined.Backspace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                else -> {
                                    NumpadButton(
                                        text = item,
                                        onClick = { if (!lockedOut && enteredPin.length < 4) enteredPin += item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumpadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun NumpadIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        contentColor = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
