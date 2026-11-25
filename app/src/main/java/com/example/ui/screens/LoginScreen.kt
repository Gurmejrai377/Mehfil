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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.MehfilGlassCard
import com.example.ui.components.MehfilLogoBadge
import com.example.ui.theme.MehfilChampagne
import com.example.ui.theme.MehfilCoral
import com.example.ui.theme.MehfilDeepPurple
import com.example.ui.theme.MehfilGold
import com.example.ui.theme.MehfilMidnightPlum
import com.example.ui.theme.MehfilMint
import com.example.ui.theme.MehfilRoyalPurple

@Composable
fun LoginScreen(
    onGuestLogin: (displayName: String) -> Unit,
    onPhoneOtpLogin: (phoneNumber: String, displayName: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Guest Login, 1 = Phone OTP Login
    var guestName by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    var phoneNumber by remember { mutableStateOf("") }
    var phoneUserName by remember { mutableStateOf("") }
    var otpStepActive by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MehfilMidnightPlum, MehfilDeepPurple, MehfilMidnightPlum)
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_party_stage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.3f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    MehfilLogoBadge(size = 56.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mehfil",
                            color = MehfilChampagne,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Yahan har baat ek Mehfil hai",
                            color = MehfilGold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                MehfilGlassCard(
                    highlightGold = true,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_mehfil_hero),
                            contentDescription = "Mehfil royal purple and gold lounge",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, MehfilMidnightPlum.copy(alpha = 0.85f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Party Rooms, Music DJ & HD Video",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enter instantly without verification",
                                color = MehfilGold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            MehfilGlassCard(
                highlightGold = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MehfilRoyalPurple.copy(alpha = 0.72f),
                        contentColor = MehfilGold,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                            },
                            text = { Text("Guest Login") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_guest_login")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                            },
                            text = { Text("Phone OTP") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_phone_otp_login")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MehfilCoral.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MehfilCoral.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MehfilCoral,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        Text(
                            text = "Jump straight into Mehfil Party Rooms as a Guest — no verification needed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = guestName,
                            onValueChange = { if (it.length <= 60) guestName = it },
                            label = { Text("Your Name (Optional)") },
                            placeholder = { Text("Mehfil Guest") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("guest_name_input")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onGuestLogin(guestName) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MehfilGold,
                                contentColor = MehfilMidnightPlum
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("guest_login_button")
                        ) {
                            Text(
                                text = "Continue as Guest",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        if (!otpStepActive) {
                            Text(
                                text = "Enter your mobile number to get instant access without SMS verification.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = countryCode,
                                    onValueChange = { if (it.length <= 5) countryCode = it },
                                    label = { Text("Code") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.width(86.dp)
                                )
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { input ->
                                        val cleaned = input.filter { it.isDigit() }
                                        if (cleaned.length <= 15) phoneNumber = cleaned
                                    },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("9876543210") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phone_number_input")
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = phoneUserName,
                                onValueChange = { if (it.length <= 60) phoneUserName = it },
                                label = { Text("Display Name (Optional)") },
                                placeholder = { Text("Mehfil Member") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (phoneNumber.length < 6) {
                                        errorMessage = "Please enter at least 6 digits for your phone number."
                                    } else {
                                        errorMessage = null
                                        otpCode = "1234"
                                        otpStepActive = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MehfilGold,
                                    contentColor = MehfilMidnightPlum
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("request_otp_button")
                            ) {
                                Text(
                                    text = "Continue with Phone OTP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                color = MehfilMint.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = MehfilMint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Instant OTP pre-filled for $countryCode $phoneNumber (no SMS needed).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    if (digits.length <= 6) otpCode = digits
                                },
                                label = { Text("Enter OTP (Any 4 digits)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_code_input")
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (otpCode.length < 4) {
                                        errorMessage = "Enter any 4-digit OTP or tap Enter Directly."
                                    } else {
                                        onPhoneOtpLogin("$countryCode$phoneNumber", phoneUserName)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MehfilGold,
                                    contentColor = MehfilMidnightPlum
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("verify_otp_button")
                            ) {
                                Text(
                                    text = "Verify & Enter Mehfil",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { otpStepActive = false }) {
                                    Text("Change Number", color = MehfilChampagne)
                                }
                                OutlinedButton(
                                    onClick = { onPhoneOtpLogin("$countryCode$phoneNumber", phoneUserName) },
                                    border = BorderStroke(1.dp, MehfilGold.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("skip_otp_button")
                                ) {
                                    Text("Enter Directly", color = MehfilGold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Yahan har baat ek Mehfil hai",
                color = MehfilGold.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
