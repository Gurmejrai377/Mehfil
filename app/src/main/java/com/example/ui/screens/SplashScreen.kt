package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.MehfilLogoBadge
import com.example.ui.theme.MehfilChampagne
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilRoyalPurple
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val logoScale = remember { Animatable(0.65f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
        )
        delay(900L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MehfilMidnightPlum, MehfilDeepPurple, MehfilRoyalPurple)
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_party_stage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.45f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha.value)
            ) {
                Surface(
                    shape = CircleShape,
                    shadowElevation = 24.dp,
                    color = Color.Transparent,
                    modifier = Modifier.scale(logoScale.value)
                ) {
                    MehfilLogoBadge(size = 128.dp)
                }

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = "Mehfil",
                    color = MehfilChampagne,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 42.sp,
                        letterSpacing = 0.8.sp
                    ),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Yahan har baat ek Mehfil hai",
                    color = MehfilGold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha.value)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(150.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MehfilGold,
                    trackColor = Color.White.copy(alpha = 0.18f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Party Rooms • Music Stage • HD Video",
                    color = MehfilChampagne.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
