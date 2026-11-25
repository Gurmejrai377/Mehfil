package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.GoogleSignInButton
import com.example.ui.components.MehfilGlassCard
import com.example.ui.components.MehfilLogoBadge
import com.example.ui.theme.MehfilCoral
import com.example.ui.theme.MehfilGlassBgStrong
import com.example.ui.theme.MehfilIceWhite
import com.example.ui.theme.MehfilNeonCyan
import com.example.ui.theme.MehfilNeonGreen
import com.example.ui.theme.MehfilNeonPink
import com.example.ui.theme.MehfilObsidian
import com.example.ui.theme.MehfilSlateNight
import com.example.ui.theme.MehfilSoftMuted

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MehfilObsidian, MehfilSlateNight, MehfilObsidian)
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_party_stage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.62f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MehfilObsidian.copy(alpha = 0.28f),
                            MehfilObsidian.copy(alpha = 0.56f),
                            MehfilObsidian.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(10.dp))
                MehfilLogoBadge(size = 86.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mehfil",
                    color = MehfilIceWhite,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Welcome Back!",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Login to continue • Connect, Chat & Party Live",
                    color = MehfilSoftMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            MehfilGlassCard(
                highlightGold = true,
                shape = RoundedCornerShape(28.dp),
                containerColor = MehfilGlassBgStrong,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MehfilGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        containerColor = Color.White.copy(alpha = 0.04f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            LoginHighlightRow(
                                icon = Icons.Rounded.Videocam,
                                tint = MehfilNeonGreen,
                                text = "Live 1-on-1 Audio & HD Video Calls"
                            )
                            LoginHighlightRow(
                                icon = Icons.Rounded.Groups,
                                tint = MehfilNeonCyan,
                                text = "8-Mic Party Rooms, DJ Synth & Gifts"
                            )
                            LoginHighlightRow(
                                icon = Icons.Rounded.GraphicEq,
                                tint = MehfilNeonPink,
                                text = "Instant Cloud Chat, Voice Notes & Stories"
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MehfilCoral.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MehfilCoral.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MehfilCoral,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    GoogleSignInButton(
                        onAuthSuccess = {
                            errorMessage = null
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            errorMessage = msg
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FeatureGlowPill(icon = Icons.Rounded.Videocam, color = MehfilNeonGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        FeatureGlowPill(icon = Icons.Rounded.Groups, color = MehfilNeonCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        FeatureGlowPill(icon = Icons.Rounded.GraphicEq, color = MehfilNeonPink)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Yahan har baat ek Mehfil hai",
                color = MehfilNeonCyan,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureGlowPill(
    icon: ImageVector,
    color: Color
) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LoginHighlightRow(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}
