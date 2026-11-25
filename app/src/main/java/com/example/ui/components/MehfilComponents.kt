package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.LoginMode
import com.example.data.Story
import com.example.data.UserAccount
import com.example.ui.theme.MehfilChampagne
import com.example.ui.theme.MehfilCoral
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGlassBg
import com.example.ui.theme.MehfilGlassStroke
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilMint
import com.example.ui.theme.MehfilPurple
import com.example.ui.theme.MehfilPurpleLight
import com.example.ui.theme.MehfilRichGold
import com.example.ui.theme.MehfilRoyalPurple
import kotlinx.coroutines.delay

fun parseHexColor(hex: String, fallback: Color = MehfilPurple): Color {
    return try {
        val clean = hex.removePrefix("#")
        Color(("FF$clean").toLong(16))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun MehfilLogoBadge(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = MehfilGold, spotColor = MehfilGold)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(MehfilRoyalPurple, Color(0xFF14063B))
                )
            )
            .border(
                width = (size.value * 0.045f).dp.coerceAtLeast(1.5.dp),
                brush = Brush.linearGradient(listOf(MehfilChampagne, MehfilGold, MehfilRichGold)),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_mehfil_foreground),
            contentDescription = "Mehfil golden microphone luxury logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
fun MehfilGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MehfilGlassBg,
    highlightGold: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val borderBrush = if (highlightGold) {
        Brush.linearGradient(listOf(MehfilChampagne.copy(alpha = 0.72f), MehfilRichGold.copy(alpha = 0.45f)))
    } else {
        Brush.linearGradient(listOf(MehfilGlassStroke, MehfilPurpleLight.copy(alpha = 0.28f)))
    }

    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderBrush),
        shadowElevation = 6.dp,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                )
            ),
            content = content
        )
    }
}

@Composable
fun AnimatedMusicEqualizer(
    isPlaying: Boolean,
    barColor: Color = MehfilGold,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq_anim")
    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(370, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3"
    )
    val bar4 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(470, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq4"
    )
    val heights = if (isPlaying) listOf(bar1, bar2, bar3, bar4) else listOf(0.28f, 0.42f, 0.32f, 0.25f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((18f * fraction).dp.coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun MehfilAvatar(
    name: String,
    accentHex: String = "#6E2CF2",
    size: Dp = 52.dp,
    online: Boolean = false,
    hasStoryRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val baseColor = remember(accentHex) { parseHexColor(accentHex, MehfilPurple) }
    val initials = remember(name) {
        name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "M" }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val ringModifier = if (hasStoryRing) {
            Modifier
                .fillMaxSize()
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(listOf(MehfilChampagne, MehfilGold, MehfilPurple)),
                    shape = CircleShape
                )
                .padding(3.5.dp)
        } else {
            Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MehfilGold.copy(alpha = 0.35f),
                    shape = CircleShape
                )
        }

        Box(
            modifier = ringModifier
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(baseColor, MehfilRoyalPurple)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = MehfilChampagne,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.34f).sp
            )
        }

        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.28f).dp.coerceIn(11.dp, 16.dp))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MehfilMint)
                )
            }
        }
    }
}

@Composable
fun StoryViewerDialog(
    story: Story,
    onDismiss: () -> Unit
) {
    var progress by remember(story.id) { mutableFloatStateOf(0f) }
    val bgColor = remember(story.accentHex) { parseHexColor(story.accentHex, MehfilPurple) }

    LaunchedEffect(story.id) {
        val steps = 60
        repeat(steps) {
            delay(85L)
            progress = (it + 1) / steps.toFloat()
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(bgColor, MehfilDeepPurple, MehfilMidnightPlum)
                    )
                )
                .clickable(onClick = onDismiss)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MehfilGold,
                    trackColor = Color.White.copy(alpha = 0.22f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MehfilAvatar(
                            name = story.ownerName,
                            accentHex = story.accentHex,
                            size = 42.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = story.ownerName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Mehfil Story",
                                color = MehfilGold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_story_button")) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close story",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = story.moodEmoji, fontSize = 68.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = story.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = MehfilGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${story.viewCount} views • Tap to close",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsSheet(
    account: UserAccount?,
    onSave: (displayName: String, bio: String, online: Boolean, lowDataMode: Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var name by remember(account) { mutableStateOf(account?.displayName ?: "Mehfil Guest") }
    var bio by remember(account) { mutableStateOf(account?.bio ?: "Yahan har baat ek Mehfil hai") }
    var online by remember(account) { mutableStateOf(account?.online ?: true) }
    var lowDataMode by remember(account) { mutableStateOf(account?.lowDataMode ?: false) }
    var copied by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MehfilDeepPurple,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MehfilAvatar(
                    name = name,
                    size = 60.dp,
                    online = online,
                    hasStoryRing = true
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val modeLabel = if (LoginMode.fromRaw(account?.loginMode) == LoginMode.PHONE) {
                        "Phone: ${account?.phoneNumber ?: "Verified"}"
                    } else {
                        "Guest Session"
                    }
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val mehfilId = account?.mehfilId ?: "mehfil_guest"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable {
                                clipboard.setText(AnnotatedString(mehfilId))
                                copied = true
                            }
                    ) {
                        Text(
                            text = if (copied) "Copied $mehfilId" else "Mehfil ID: $mehfilId",
                            style = MaterialTheme.typography.labelMedium,
                            color = MehfilGold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy Mehfil ID",
                            tint = MehfilGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 60) name = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 160) bio = it },
                label = { Text("Status Bio") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("profile_bio_input")
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show Online Status", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Let contacts & rooms see when you're online",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = online, onCheckedChange = { online = it })
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.DataSaverOn,
                        contentDescription = null,
                        tint = MehfilGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Low-Data Call Mode", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Optimize video calls for slower networks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = lowDataMode, onCheckedChange = { lowDataMode = it })
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    onSave(name, bio, online, lowDataMode)
                    onDismiss()
                },
                enabled = name.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MehfilGold,
                    contentColor = MehfilMidnightPlum
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button")
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MehfilCoral),
                border = BorderStroke(1.dp, MehfilCoral.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch Account / Sign Out")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
