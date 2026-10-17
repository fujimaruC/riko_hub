package id.riko.hub.ui.spaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.theme.ChefButter
import id.riko.hub.ui.theme.ChefCoral
import id.riko.hub.ui.theme.ChefGround
import id.riko.hub.ui.theme.ChefInk
import id.riko.hub.ui.theme.ChefLime
import id.riko.hub.ui.theme.ChefLine
import id.riko.hub.ui.theme.ChefMuted
import id.riko.hub.ui.theme.ChefPeriwinkle
import id.riko.hub.ui.theme.ChefSage
import id.riko.hub.ui.theme.NotoCondensedBlack

private enum class SpaceItem(
    val route: String,
    val num: String,
    val titleTop: String,
    val titleBottom: String,
    val destination: Destination,
    val badge1: String,
    val badge2: String,
    val color: Color,
    val buttonLabel: String,
) {
    ACADEMICS(
        route = "AcademicsSpace",
        num = "01 / AKADEMIK",
        titleTop = "KAMPUS",
        titleBottom = "BERGERAK",
        destination = Destination.AcademicsSpace,
        badge1 = "12 SKS Aktif",
        badge2 = "4 Mata Kuliah",
        color = ChefButter,
        buttonLabel = "MASUK RUANG AKADEMIK",
    ),
    PROJECTS(
        route = "ProjectsSpace",
        num = "02 / PROYEK",
        titleTop = "TARGET",
        titleBottom = "SKRIPSI",
        destination = Destination.ProjectsSpace,
        badge1 = "Sprint 42",
        badge2 = "3 Tahap Selesai",
        color = ChefCoral,
        buttonLabel = "MASUK RUANG PROYEK",
    ),
    PERSONAL(
        route = "PersonalSpace",
        num = "03 / PERSONAL",
        titleTop = "RITME",
        titleBottom = "TENANG",
        destination = Destination.PersonalSpace,
        badge1 = "Jurnal Draf",
        badge2 = "Fokus Istirahat",
        color = ChefSage,
        buttonLabel = "MASUK RUANG PERSONAL",
    ),
    DEVICES(
        route = "DevicesSpace",
        num = "04 / PERANGKAT",
        titleTop = "INTEGRASI",
        titleBottom = "LOKAL",
        destination = Destination.DevicesSpace,
        badge1 = "TLS P2P",
        badge2 = "3 Terhubung",
        color = Color(0xFF9FE3DE),
        buttonLabel = "MASUK RUANG PERANGKAT",
    ),
}

@Composable
fun SpacesScreen(onNavigate: (Destination) -> Unit) {
    var activeSpace by remember { mutableStateOf(SpaceItem.ACADEMICS) }

    Column(
        Modifier
            .fillMaxSize()
            .testTag("screen-Spaces")
            .background(ChefPeriwinkle)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header Bar (Matches full-app-v1.html line 560)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "KATEGORI RUANG",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    text = "CATEGORIES",
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = Color.White,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(40.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "⌕",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChefGround,
                    )
                }
            }
        }

        // Stepped Deck Container
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy((-14).dp),
        ) {
            // Background Peeking Tabs for non-active spaces in reverse order
            val inactiveSpaces = SpaceItem.entries.filter { it != activeSpace }.reversed()
            inactiveSpaces.forEach { space ->
                val tabColor = when (space) {
                    SpaceItem.DEVICES -> ChefCoral
                    SpaceItem.PERSONAL -> ChefGround
                    SpaceItem.PROJECTS -> ChefSage
                    SpaceItem.ACADEMICS -> ChefButter
                }
                val textColor = if (space == SpaceItem.PERSONAL) Color.White else ChefGround

                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = tabColor,
                    border = BorderStroke(2.dp, ChefGround),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("space-portal-${space.route}")
                        .clickable(role = Role.Button) { onNavigate(space.destination) },
                ) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = space.num,
                            fontFamily = NotoCondensedBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = textColor,
                        )
                        Text(
                            text = "↗",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = textColor,
                        )
                    }
                }
            }

            // Active Front Hero Card (Mustard Yellow or selected color)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = activeSpace.color,
                border = BorderStroke(2.5.dp, ChefGround),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("space-portal-${activeSpace.route}")
                    .clickable(role = Role.Button) { onNavigate(activeSpace.destination) },
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header inside hero card: Kicker + Big Title + Round Arrow
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column {
                            Text(
                                text = activeSpace.num,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                color = ChefGround.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${activeSpace.titleTop}\n${activeSpace.titleBottom}",
                                fontFamily = NotoCondensedBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                lineHeight = 34.sp,
                                color = ChefGround,
                            )
                        }

                        // Black Circle Arrow Button
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ChefGround)
                                .clickable { onNavigate(activeSpace.destination) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "↗",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }

                    // Two Badge Pills
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryBadgePill(dotColor = ChefCoral, label = activeSpace.badge1)
                        CategoryBadgePill(dotColor = ChefPeriwinkle, label = activeSpace.badge2)
                    }

                    // Whimsical Van/Dome Illustration from Reference Image 4
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(width = 200.dp, height = 120.dp)) {
                            val w = size.width
                            val h = size.height

                            // Trees on top
                            drawCircle(color = Color(0xFFE8EAE0), radius = 24.dp.toPx(), center = Offset(w * 0.3f, h * 0.32f))
                            drawCircle(color = ChefGround, radius = 24.dp.toPx(), center = Offset(w * 0.3f, h * 0.32f), style = Stroke(width = 2.dp.toPx()))

                            drawCircle(color = Color(0xFFE8EAE0), radius = 22.dp.toPx(), center = Offset(w * 0.72f, h * 0.31f))
                            drawCircle(color = ChefGround, radius = 22.dp.toPx(), center = Offset(w * 0.72f, h * 0.31f), style = Stroke(width = 2.dp.toPx()))

                            drawCircle(color = ChefSage, radius = 28.dp.toPx(), center = Offset(w * 0.5f, h * 0.22f))
                            drawCircle(color = ChefGround, radius = 28.dp.toPx(), center = Offset(w * 0.5f, h * 0.22f), style = Stroke(width = 2.dp.toPx()))

                            // Red dome vehicle body
                            val domePath = Path().apply {
                                moveTo(w * 0.16f, h * 0.82f)
                                cubicTo(w * 0.16f, h * 0.42f, w * 0.84f, h * 0.42f, w * 0.84f, h * 0.82f)
                                close()
                            }
                            drawPath(path = domePath, color = ChefCoral)
                            drawPath(path = domePath, color = ChefGround, style = Stroke(width = 2.5.dp.toPx()))

                            // White cabin window
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(w * 0.32f, h * 0.52f),
                                size = Size(w * 0.36f, h * 0.28f),
                                cornerRadius = CornerRadius(10.dp.toPx()),
                            )
                            drawRoundRect(
                                color = ChefGround,
                                topLeft = Offset(w * 0.32f, h * 0.52f),
                                size = Size(w * 0.36f, h * 0.28f),
                                cornerRadius = CornerRadius(10.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx()),
                            )

                            // Character with headphones
                            drawCircle(color = ChefButter, radius = 9.dp.toPx(), center = Offset(w * 0.46f, h * 0.63f))
                            drawCircle(color = ChefGround, radius = 9.dp.toPx(), center = Offset(w * 0.46f, h * 0.63f), style = Stroke(width = 1.5.dp.toPx()))
                            drawRoundRect(
                                color = ChefSage,
                                topLeft = Offset(w * 0.42f, h * 0.68f),
                                size = Size(14.dp.toPx(), 12.dp.toPx()),
                                cornerRadius = CornerRadius(3.dp.toPx()),
                            )

                            // Wheels & chassis
                            drawRoundRect(
                                color = Color(0xFFFDFCFA),
                                topLeft = Offset(w * 0.14f, h * 0.82f),
                                size = Size(w * 0.72f, 13.dp.toPx()),
                                cornerRadius = CornerRadius(6.dp.toPx()),
                            )
                            drawRoundRect(
                                color = ChefGround,
                                topLeft = Offset(w * 0.14f, h * 0.82f),
                                size = Size(w * 0.72f, 13.dp.toPx()),
                                cornerRadius = CornerRadius(6.dp.toPx()),
                                style = Stroke(width = 2.5.dp.toPx()),
                            )
                            drawCircle(color = ChefGround, radius = 4.dp.toPx(), center = Offset(w * 0.5f, h * 0.87f))
                        }
                    }

                    // Bottom Enter Space Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ChefGround,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onNavigate(activeSpace.destination) },
                    ) {
                        Row(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = activeSpace.buttonLabel,
                                fontFamily = NotoCondensedBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.White,
                            )
                            Text(
                                text = "↗",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // Quick Space Switcher Tabs (Bottom)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpaceItem.entries.forEach { space ->
                val isSelected = space == activeSpace
                Surface(
                    onClick = { activeSpace = space },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ChefGround else Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                ) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = space.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBadgePill(dotColor: Color, label: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, ChefGround.copy(alpha = 0.15f)),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ChefGround,
            )
        }
    }
}
