package com.example.ui.online

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.onGoogleSignInClicked
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.HudSurfaceDark
import com.example.ui.theme.JuaFontFamily
import com.example.ui.theme.PlayerBlue
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.SuccessGreen

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingAuto by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAuto = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAuto = false
            },
            scope = scope
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 카툰 하늘 + 언덕 + 성곽 배경
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E96FC), Color(0xFF68D8FF), Color(0xFFD9F6FF))
                ),
                size = size
            )
            // 구름
            listOf(0.15f to 0.16f, 0.52f to 0.12f, 0.80f to 0.20f).forEach { (fx, fy) ->
                val cx = w * fx
                val cy = h * fy
                drawCircle(Color.White.copy(alpha = 0.88f), 36f, Offset(cx, cy))
                drawCircle(Color.White.copy(alpha = 0.88f), 46f, Offset(cx + 38f, cy - 12f))
                drawCircle(Color.White.copy(alpha = 0.88f), 32f, Offset(cx + 76f, cy + 4f))
            }
            // 성탑 실루엣
            val towerX = w * 0.82f
            val towerTop = h * 0.25f
            val towerColor = Color(0xFF9C8E75).copy(alpha = 0.5f)
            drawRect(towerColor, Offset(towerX, towerTop), Size(w * 0.15f, h * 0.65f))
            for (b in 0..2) {
                drawRect(towerColor, Offset(towerX + b * (w * 0.052f), towerTop - 24f), Size(w * 0.034f, 24f))
            }
            // 언덕
            val hill = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.72f)
                quadraticTo(w * 0.30f, h * 0.58f, w * 0.58f, h * 0.76f)
                quadraticTo(w * 0.82f, h * 0.86f, w, h * 0.68f)
                lineTo(w, h)
                close()
            }
            drawPath(hill, color = Color(0xFF52B788))
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 좌측: 로고 + 엠블럼
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Text(
                            text = "캐논 대전",
                            style = TextStyle(fontFamily = JuaFontFamily, fontSize = 46.sp, color = Color(0xFF4A1500)),
                            modifier = Modifier.offset(x = 3.dp, y = 4.dp)
                        )
                        Text(
                            text = "캐논 대전",
                            style = TextStyle(fontFamily = JuaFontFamily, fontSize = 46.sp, color = Color.White),
                            modifier = Modifier.offset(x = (-1.5).dp, y = (-1.5).dp)
                        )
                        Text(
                            text = "캐논 대전",
                            style = TextStyle(
                                fontFamily = JuaFontFamily,
                                fontSize = 46.sp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFE600), Color(0xFFFF7B00), Color(0xFFE63946))
                                )
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .rotate(-6f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF38F9D7), Color(0xFF00B45A))))
                            .border(3.dp, DeepNavyOutline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "V2",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = DeepNavyOutline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.cannon_app_icon),
                    contentDescription = "캐논 대전 대표 탱크",
                    modifier = Modifier
                        .size(106.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(3.5.dp, Color.White, RoundedCornerShape(26.dp))
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xDD132238))
                        .border(1.5.dp, PrimaryGold, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("auth_creator_badge")
                ) {
                    Text(
                        text = "제작 · 구지초도형캠프",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold
                    )
                }
            }

            // 우측: 구글 로그인 카드
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .widthIn(max = 430.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(HudSurfaceDark)
                    .border(3.dp, Color.White, RoundedCornerShape(26.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "온라인 전장에 합류하세요!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryGold,
                    textAlign = TextAlign.Center
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AuthFeatureRow(Icons.Rounded.Groups, "실시간 대기실 생성 & 초대 코드로 친구와 1:1 대전", Color(0xFF4CC9F0))
                    AuthFeatureRow(Icons.Rounded.EmojiEvents, "승패 자동 기록 & 실시간 글로벌 랭킹 경쟁", PrimaryGold)
                    AuthFeatureRow(Icons.Rounded.TrendingUp, "전투 경험치 획득으로 지휘관 레벨업 & 티어 승급", SuccessGreen)
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DangerRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33E63946))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading && !isCheckingAuto,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DeepNavyOutline
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(2.5.dp, PlayerBlue, RoundedCornerShape(16.dp))
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading || isCheckingAuto) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PlayerBlue,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isCheckingAuto) "계정 확인 중…" else "Google 로그인 중…",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepNavyOutline
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Google 로그인",
                            tint = PlayerBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google로 로그인",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepNavyOutline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(icon: ImageVector, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}
