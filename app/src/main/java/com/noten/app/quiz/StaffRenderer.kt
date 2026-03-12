package com.noten.app.quiz

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

@Composable
fun StaffNotation(
    staffPosition: Int,
    noteColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val clefText = "\uD834\uDD1E"
    val clefStyle = TextStyle(fontSize = 54.sp, color = Color(0xFF9E9E9E))

    Canvas(modifier = modifier) {
        val lineColor = Color(0xFF555555)

        // Calculate how much vertical space we need:
        // Staff itself = 4 lineSpacing (5 lines)
        // Below staff: if staffPosition < 0, we need extra space
        // Above staff: if staffPosition > 8, we need extra space
        val staffLines = 4 // spaces between 5 lines
        val belowExtra = if (staffPosition < -1) (-staffPosition - 1) / 2f + 1f else 0f
        val aboveExtra = if (staffPosition > 9) (staffPosition - 9) / 2f + 1f else 0f
        // Total vertical units needed (in lineSpacing units)
        val totalUnits = staffLines + belowExtra + aboveExtra + 2f // +2 for stem room

        val lineSpacing = size.height / totalUnits
        // Position staff so everything fits: more space below for low notes
        val staffTop = (aboveExtra + 1f) * lineSpacing // 1 unit margin above

        // 5 staff lines
        for (i in 0..4) {
            val y = staffTop + i * lineSpacing
            drawLine(lineColor, Offset(40f, y), Offset(size.width - 20f, y), strokeWidth = 1.5f)
        }

        // Treble clef
        val clefLayout = textMeasurer.measure(clefText, clefStyle)
        drawText(clefLayout, topLeft = Offset(45f, staffTop - lineSpacing * 0.9f))

        // Note position: staffPosition 0 = bottom line (line 5)
        val bottomLineY = staffTop + 4 * lineSpacing
        val noteY = bottomLineY - staffPosition * (lineSpacing / 2f)
        val noteX = size.width * 0.6f

        // Ledger lines
        drawLedgerLines(noteX, bottomLineY, staffPosition, lineSpacing, lineColor)

        // Note head (scale with lineSpacing)
        val noteRadiusX = lineSpacing * 0.45f
        val noteRadiusY = lineSpacing * 0.35f
        drawOval(
            color = noteColor,
            topLeft = Offset(noteX - noteRadiusX, noteY - noteRadiusY),
            size = Size(noteRadiusX * 2, noteRadiusY * 2)
        )

        // Stem
        val stemLength = lineSpacing * 3.5f
        val stemUp = staffPosition < 4
        if (stemUp) {
            drawLine(noteColor, Offset(noteX + noteRadiusX, noteY), Offset(noteX + noteRadiusX, noteY - stemLength), strokeWidth = 2f)
        } else {
            drawLine(noteColor, Offset(noteX - noteRadiusX, noteY), Offset(noteX - noteRadiusX, noteY + stemLength), strokeWidth = 2f)
        }
    }
}

private fun DrawScope.drawLedgerLines(
    noteX: Float,
    bottomLineY: Float,
    staffPosition: Int,
    lineSpacing: Float,
    lineColor: Color
) {
    val ledgerWidth = lineSpacing * 1.2f

    // Ledger lines below staff (staffPosition < 0 means below bottom line)
    if (staffPosition < 0) {
        var pos = -2
        while (pos >= staffPosition) {
            val y = bottomLineY - pos * (lineSpacing / 2f)
            drawLine(lineColor, Offset(noteX - ledgerWidth, y), Offset(noteX + ledgerWidth, y), strokeWidth = 1.5f)
            pos -= 2
        }
        // If note sits on a ledger line position (even negative staffPosition)
        if (staffPosition % 2 == 0) {
            val y = bottomLineY - staffPosition * (lineSpacing / 2f)
            drawLine(lineColor, Offset(noteX - ledgerWidth, y), Offset(noteX + ledgerWidth, y), strokeWidth = 1.5f)
        }
    }

    // Ledger lines above staff (staffPosition > 8 means above top line)
    if (staffPosition > 8) {
        var pos = 10
        while (pos <= staffPosition) {
            val y = bottomLineY - pos * (lineSpacing / 2f)
            drawLine(lineColor, Offset(noteX - ledgerWidth, y), Offset(noteX + ledgerWidth, y), strokeWidth = 1.5f)
            pos += 2
        }
        if (staffPosition % 2 == 0) {
            val y = bottomLineY - staffPosition * (lineSpacing / 2f)
            drawLine(lineColor, Offset(noteX - ledgerWidth, y), Offset(noteX + ledgerWidth, y), strokeWidth = 1.5f)
        }
    }
}
