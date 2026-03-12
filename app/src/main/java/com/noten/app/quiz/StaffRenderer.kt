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
        val lineSpacing = size.height / 8f
        val staffTop = size.height / 2f - 2f * lineSpacing
        val lineColor = Color(0xFF555555)

        // 5 staff lines
        for (i in 0..4) {
            val y = staffTop + i * lineSpacing
            drawLine(lineColor, Offset(40f, y), Offset(size.width - 20f, y), strokeWidth = 1.5f)
        }

        // Treble clef
        val clefLayout = textMeasurer.measure(clefText, clefStyle)
        drawText(clefLayout, topLeft = Offset(45f, staffTop - lineSpacing * 1.1f))

        // Note position
        val bottomLineY = staffTop + 4 * lineSpacing
        val noteY = bottomLineY - staffPosition * (lineSpacing / 2f)
        val noteX = size.width * 0.6f

        // Ledger lines
        drawLedgerLines(noteX, staffTop, bottomLineY, staffPosition, lineSpacing, lineColor)

        // Note head
        drawOval(
            color = noteColor,
            topLeft = Offset(noteX - 12f, noteY - 8f),
            size = Size(24f, 16f)
        )

        // Stem
        val stemUp = staffPosition < 4
        if (stemUp) {
            drawLine(noteColor, Offset(noteX + 12f, noteY), Offset(noteX + 12f, noteY - lineSpacing * 3.5f), strokeWidth = 2f)
        } else {
            drawLine(noteColor, Offset(noteX - 12f, noteY), Offset(noteX - 12f, noteY + lineSpacing * 3.5f), strokeWidth = 2f)
        }
    }
}

private fun DrawScope.drawLedgerLines(
    noteX: Float,
    staffTop: Float,
    bottomLineY: Float,
    staffPosition: Int,
    lineSpacing: Float,
    lineColor: Color
) {
    val ledgerWidth = 35f

    if (staffPosition < 0) {
        // Draw ledger lines below staff at even positions
        var pos = -2
        while (pos >= staffPosition) {
            val y = bottomLineY - pos * (lineSpacing / 2f)
            drawLine(lineColor, Offset(noteX - ledgerWidth, y), Offset(noteX + ledgerWidth, y), strokeWidth = 1.5f)
            pos -= 2
        }
    }

    if (staffPosition > 8) {
        var pos = 10
        while (pos <= staffPosition) {
            val y = bottomLineY - pos * (lineSpacing / 2f)
            drawLine(lineColor, Offset(noteX - ledgerWidth, y), Offset(noteX + ledgerWidth, y), strokeWidth = 1.5f)
            pos += 2
        }
    }
}
