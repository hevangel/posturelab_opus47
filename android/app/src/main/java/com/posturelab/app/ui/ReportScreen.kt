package com.posturelab.app.ui

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.posturelab.app.report.PdfExporter
import com.posturelab.app.report.PostureReportText
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(vm: AppViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val front = state.front; val side = state.side
    if (front == null || side == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Posture Report", color = Brand.OnPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = Brand.OnPrimary) }
                },
                actions = {
                    TextButton(onClick = {
                        val name = "posturelab-${state.name.replace(" ", "_")}-${state.date}.pdf"
                        val saved = PdfExporter.export(ctx, name, state)
                        Toast.makeText(ctx, if (saved) "Saved $name" else "Export failed", Toast.LENGTH_LONG).show()
                    }) { Text("Save PDF", color = Brand.OnPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Primary),
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(12.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Letterhead
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(48)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("PostureLab", color = Brand.Primary, fontWeight = FontWeight.Bold)
                    Text("Posture Analysis Platform", fontSize = 11.sp, color = Color.Gray)
                    Text("posturelab.example", fontSize = 11.sp, color = Color.Gray)
                }
            }
            Divider(color = Brand.Primary)
            Text(
                PostureReportText.INTRO,
                fontSize = 11.sp,
            )

            // Two image columns
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImageColumn("Your Posture Viewed from the Front", state.frontAnnotated, front.totalShiftIn, front.totalTiltDeg, Modifier.weight(1f))
                ImageColumn("Your Posture Viewed from the Side", state.sideAnnotated, side.totalShiftIn, side.totalTiltDeg, Modifier.weight(1f))
            }

            // Findings
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Tinted(true) { Text(PostureReportText.headLine(front), fontSize = 11.sp) }
                    Tinted(false) { Text(PostureReportText.shouldersLine(front), fontSize = 11.sp) }
                    Tinted(true) { Text(PostureReportText.ribLine(front), fontSize = 11.sp) }
                    Tinted(false) { Text(PostureReportText.hipsLine(front), fontSize = 11.sp) }
                    Tinted(true) { Text(PostureReportText.qLine(front), fontSize = 11.sp) }
                }
                Column(Modifier.weight(1f)) {
                    Tinted(true) { Text(PostureReportText.headSide(side), fontSize = 11.sp) }
                    Tinted(false) { Text(PostureReportText.headWeightSide(side), fontSize = 11.sp) }
                    Tinted(true) { Text(PostureReportText.shouldersSide(side), fontSize = 11.sp) }
                    Tinted(false) { Text(PostureReportText.hipsSide(side), fontSize = 11.sp) }
                    Tinted(true) { Text(PostureReportText.kneesSide(side), fontSize = 11.sp) }
                    Tinted(false) { Text(PostureReportText.headVsAnkle(side), fontSize = 11.sp) }
                }
            }

            Text(
                "Any measurable deviation from normal posture causes weakening of the spine as well as increased stress on the nervous system which can adversely affect overall health.",
                color = Brand.Primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            )
            Divider()
            Text(
                "Pose detection uses Google ML Kit on-device. This report is generated automatically and is not a medical diagnosis. Report for ${state.name} on ${state.date}.",
                fontSize = 9.sp, color = Color.Gray,
            )
        }
    }
}

@Composable
private fun ImageColumn(
    title: String, bitmap: android.graphics.Bitmap?, totalShift: Float, totalTilt: Float, modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().background(Brand.Band).padding(vertical = 6.dp), Alignment.Center) {
            Text(title.uppercase(), color = Brand.OnPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().height(260.dp).background(Color(0xFFF5F7FA)),
                contentScale = ContentScale.Fit,
            )
        }
        Row(Modifier.fillMaxWidth().background(Color.White)) {
            Column(Modifier.weight(1f).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Shift", color = Brand.Primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text("${"%.2f".format(totalShift)} in", fontSize = 10.sp)
            }
            Column(Modifier.weight(1f).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Tilt", color = Brand.Primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text("${"%.1f".format(totalTilt)}°", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun Tinted(tint: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .background(if (tint) Brand.Row else Color.White)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) { content() }
}
