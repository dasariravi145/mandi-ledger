package com.dasariravi145.agrolynch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.ui.theme.*

@Composable
fun MandiLedgerLogo(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(320.dp)
            .padding(8.dp)
            .shadow(16.dp, RoundedCornerShape(64.dp))
            .clip(RoundedCornerShape(64.dp)),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 110.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF064E3B),
                                Color(0xFF059669)
                            )
                        )
                    )
            ) {
                ShopContent(modifier = Modifier.fillMaxSize())
            }

            ShopAwning(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.TopCenter)
            )

            MandiLedgerBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun ShopAwning(modifier: Modifier = Modifier) {
    val darkGreen = Color(0xFF065F46)
    val lightGreen = Color(0xFF34D399)
    
    Row(modifier = modifier) {
        repeat(7) { index ->
            val color = if ((index % 2) == 0) darkGreen else lightGreen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(color)
                    .padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun MandiLedgerBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF10B981),
                        Color(0xFF047857)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.mandi_ledger),
            color = Color.White,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 44.sp,
                letterSpacing = (-1).sp
            )
        )
    }
}

@Composable
fun ShopContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier
                .size(110.dp, 140.dp)
                .align(Alignment.CenterEnd)
                .offset(y = (-10).dp, x = 10.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            Column {
                Text("₹", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                repeat(5) {
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(LightGray))
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .size(100.dp, 140.dp)
                .align(Alignment.CenterStart)
                .offset(x = 20.dp, y = 10.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .background(Color(0xFF065F46), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("₹ 245,500", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                }
                Spacer(modifier = Modifier.height(8.dp))
                repeat(4) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        repeat(3) {
                            Box(modifier = Modifier.size(18.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .size(110.dp, 110.dp)
                .align(Alignment.BottomEnd)
                .offset(y = (-10).dp, x = (-10).dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width * 0.1f, size.height)
                    quadraticTo(0f, size.height * 0.5f, size.width * 0.5f, size.height * 0.1f)
                    quadraticTo(size.width, size.height * 0.5f, size.width * 0.9f, size.height)
                    close()
                }
                drawPath(path, Color(0xFFD97706))
            }
            Row(modifier = Modifier.align(Alignment.TopCenter).offset(y = 10.dp)) {
                repeat(3) {
                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFFCD34D), CircleShape))
                }
            }
        }

        Icon(
            painter = painterResource(R.drawable.ic_shield_check),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp),
            tint = Color.Unspecified
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 20.dp, y = (-10).dp)
        ) {
            repeat(2) {
                Row {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(24.dp, 12.dp)
                                .background(Color(0xFFFBBF24), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 10.dp, y = (-20).dp)
        ) {
            repeat(4) {
                Box(modifier = Modifier.size(28.dp).background(AccentPurple, CircleShape))
                Spacer(modifier = Modifier.width((-10).dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MandiLedgerLogoPreview() {
    MandiLedgerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1F2937)),
            contentAlignment = Alignment.Center
        ) {
            MandiLedgerLogo()
        }
    }
}
