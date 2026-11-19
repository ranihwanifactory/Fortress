package com.example.ui.tank

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.AppLanguage
import com.example.game.model.TankType
import com.example.ui.theme.DeepNavyOutline
import com.example.ui.theme.HudSurfaceDark
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted

/**
 * 7종 포탄별 전용 탱크를 미리보고 선택·장착할 수 있는 격납고(탱크 선택) 화면.
 */
@Composable
fun TankSelectorOverlay(
    currentTank: TankType,
    language: AppLanguage,
    onSelectTank: (TankType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKo = language == AppLanguage.KO
    var previewTank by remember(currentTank) { mutableStateOf(currentTank) }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE007101E))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp)
            .testTag("tank_selector_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(HudSurfaceDark)
                .border(3.dp, PrimaryGold, RoundedCornerShape(24.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 상단 바
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (isKo) "포탄별 전용 탱크 격납고 (7종)" else "Shell-Themed Tank Hangar (7)",
                            style = MaterialTheme.typography.titleLarge,
                            color = PrimaryGold
                        )
                        Text(
                            text = if (isKo) "원하는 탱크를 선택하면 전장·프로필·명예의 전당에 모두 적용됩니다!"
                            else "Equip your tank for battles, profile, and the Hall of Fame!",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF90E0EF)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF23395B))
                        .testTag("close_tank_selector_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = if (isKo) "닫기" else "Close",
                        tint = Color.White
                    )
                }
            }

            // 본문: 좌측 대형 스포트라이트 쇼케이스 + 우측 7종 탱크 그리드
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 좌측: 선택된 탱크 쇼케이스 & 스펙 카드
                Column(
                    modifier = Modifier
                        .weight(0.92f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF142742),
                                    Color(0xFF0C192C)
                                )
                            )
                        )
                        .border(2.dp, previewTank.trimColor, RoundedCornerShape(18.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 시그니처 포탄 배지
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(previewTank.signatureWeapon.shellColor.copy(alpha = 0.22f))
                            .border(1.5.dp, previewTank.signatureWeapon.shellColor, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(previewTank.signatureWeapon.shellColor)
                        )
                        Text(
                            text = if (isKo) "전용 포탄 · ${previewTank.signatureWeapon.label(language)}"
                            else "Signature · ${previewTank.signatureWeapon.label(language)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    TankAvatar(
                        tankType = previewTank,
                        size = 112.dp,
                        showPedestal = true,
                        highlighted = true
                    )

                    Text(
                        text = previewTank.label(language),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = previewTank.title(language),
                        style = MaterialTheme.typography.labelMedium,
                        color = previewTank.trimColor,
                        textAlign = TextAlign.Center
                    )

                    // 전용 특수 능력 배지
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A3150))
                            .border(1.dp, previewTank.accentColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = PrimaryGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = previewTank.perk(language),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 3종 기본 스탯 요약
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TankSpecChip(
                            label = "HP",
                            value = "${100 + previewTank.bonusHp}",
                            accent = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        TankSpecChip(
                            label = if (isKo) "연료" else "FUEL",
                            value = "${(100 + previewTank.bonusFuel.toInt())}%",
                            accent = Color(0xFF4CC9F0),
                            modifier = Modifier.weight(1f)
                        )
                        TankSpecChip(
                            label = if (isKo) "주포" else "SHELL",
                            value = previewTank.signatureWeapon.label(language),
                            accent = previewTank.signatureWeapon.shellColor,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    val isAlreadyEquipped = previewTank == currentTank
                    Button(
                        onClick = {
                            onSelectTank(previewTank)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAlreadyEquipped) SuccessGreen else PrimaryGold
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(2.dp, DeepNavyOutline, RoundedCornerShape(14.dp))
                            .testTag("equip_tank_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = DeepNavyOutline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isAlreadyEquipped && isKo -> "장착 중! 확인"
                                isAlreadyEquipped -> "Equipped · Done"
                                isKo -> "${previewTank.label(language)} 출격 장착!"
                                else -> "Equip ${previewTank.label(language)}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyOutline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 우측: 7종 포탄별 탱크 선택 그리드
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 112.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TankType.entries, key = { it.name }) { tank ->
                        val isSelected = tank == previewTank
                        val isEquipped = tank == currentTank
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color(0xFF24426B) else Color(0xFF13243C)
                                )
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = when {
                                        isSelected -> PrimaryGold
                                        isEquipped -> SuccessGreen
                                        else -> Color(0xFF2B486E)
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    previewTank = tank
                                    onSelectTank(tank)
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("select_tank_${tank.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                TankAvatar(
                                    tankType = tank,
                                    size = 64.dp,
                                    highlighted = isSelected,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                if (isEquipped) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(SuccessGreen)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (isKo) "장착" else "ON",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavyOutline
                                        )
                                    }
                                }
                            }
                            Text(
                                text = tank.label(language),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tank.signatureWeapon.label(language),
                                style = MaterialTheme.typography.labelSmall,
                                color = tank.signatureWeapon.shellColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TankSpecChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF162A45))
            .padding(vertical = 5.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
