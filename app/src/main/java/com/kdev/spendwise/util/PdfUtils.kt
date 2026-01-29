package com.kdev.spendwise.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kdev.spendwise.data.Expense
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.math.abs

object PdfUtils {

    fun exportTransactionsToPdf(
        context: Context,
        transactions: List<Expense>,
        walletMap: Map<String, String>, // Map ID -> Bank Name
        initialBalance: Double,
        totalIncome: Double,
        totalExpense: Double,
        closingBalance: Double,
        reportTitle: String = "Financial Report"
    ) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val paint = Paint()

        // --- COLORS ---
        val primaryColor = Color.parseColor("#4F5B93")
        val incomeColor = Color.parseColor("#388E3C")
        val expenseColor = Color.parseColor("#D32F2F")
        val lightGray = Color.parseColor("#F8F9FA")

        // 1. DRAW HEADER BACKGROUND
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 100f, paint)

        // 2. DRAW VECTOR LOGO (Exact paths restored)
        val logoPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val logoGradient = LinearGradient(
            24.75f, 25.5f, 81.75f, 79.5f,
            intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#E0E4F3")),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        logoPaint.shader = logoGradient

        val walletPath = Path().apply {
            moveTo(78.75f, 70.5f)
            lineTo(78.75f, 73.5f)
            cubicTo(78.75f, 76.8f, 76.05f, 79.5f, 72.75f, 79.5f)
            lineTo(30.75f, 79.5f)
            cubicTo(27.42f, 79.5f, 24.75f, 76.8f, 24.75f, 73.5f)
            lineTo(24.75f, 31.5f)
            cubicTo(24.75f, 28.2f, 27.42f, 25.5f, 30.75f, 25.5f)
            lineTo(72.75f, 25.5f)
            cubicTo(76.05f, 25.5f, 78.75f, 28.2f, 78.75f, 31.5f)
            lineTo(78.75f, 34.5f)
            lineTo(51.75f, 34.5f)
            cubicTo(48.42f, 34.5f, 45.75f, 37.2f, 45.75f, 40.5f)
            lineTo(45.75f, 64.5f)
            cubicTo(45.75f, 67.8f, 48.42f, 70.5f, 51.75f, 70.5f)
            lineTo(78.75f, 70.5f)
            close()

            moveTo(51.75f, 64.5f)
            lineTo(81.75f, 64.5f)
            lineTo(81.75f, 40.5f)
            lineTo(51.75f, 40.5f)
            lineTo(51.75f, 64.5f)
            close()

            moveTo(63.75f, 57f)
            cubicTo(61.26f, 57f, 59.25f, 54.99f, 59.25f, 52.5f)
            cubicTo(59.25f, 50.01f, 61.26f, 48f, 63.75f, 48f)
            cubicTo(66.24f, 48f, 68.25f, 50.01f, 68.25f, 52.5f)
            cubicTo(68.25f, 54.99f, 66.24f, 57f, 63.75f, 57f)
            close()
            fillType = Path.FillType.EVEN_ODD
        }
        canvas.drawPath(walletPath, logoPaint)
        logoPaint.shader = null

        // 3. TITLE TEXT
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("SpendWise", 90f, 55f, paint)
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText(reportTitle, 90f, 75f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Generated: ${DateUtils.formatDate(System.currentTimeMillis())}", 570f, 55f, paint)

        // 4. SUMMARY CARD (Evenly Spaced)
        val cardY = 120f
        val startX = 20f
        val endX = 575f
        val usableWidth = endX - startX

        // Calculate even anchor points for 3 columns
        val col1 = startX + 20f             // Left
        val col2 = startX + (usableWidth / 2) // Center
        val col3 = endX - 20f               // Right

        paint.color = lightGray
        canvas.drawRoundRect(startX, cardY, endX, cardY + 90f, 20f, 20f, paint)

        // Labels
        paint.textSize = 9f
        paint.color = Color.GRAY
        paint.isFakeBoldText = true

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("OPENING BALANCE", col1, cardY + 30f, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("TOTAL INCOME", col2, cardY + 30f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TOTAL EXPENSE", col3, cardY + 30f, paint)

        // Values
        paint.textSize = 14f
        paint.isFakeBoldText = true

        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        canvas.drawText(CurrencyUtils.formatINR(initialBalance), col1, cardY + 60f, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.color = incomeColor
        canvas.drawText("+ ${CurrencyUtils.formatINR(totalIncome)}", col2, cardY + 60f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = expenseColor
        canvas.drawText("- ${CurrencyUtils.formatINR(totalExpense)}", col3, cardY + 60f, paint)

        // Closing Balance Bar
        paint.color = primaryColor
        canvas.drawRect(startX, cardY + 90f, endX, cardY + 120f, paint)
        paint.color = Color.WHITE
        paint.textSize = 12f

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("CLOSING BALANCE", col1, cardY + 110f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatINR(closingBalance), col3, cardY + 110f, paint)
        // 5. TABLE COLUMNS
        // Date(30), Bank(110), Title(210), Cat(360), Amt(500)
        val colDate = 30f
        val colBank = 110f
        val colTitle = 210f
        val colCat = 360f
        val colAmt = 500f

        fun drawTableHeaders(y: Float) {
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#E8EAF6")
            canvas.drawRect(20f, y - 15f, 575f, y + 10f, paint)
            paint.color = primaryColor
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("DATE", colDate, y, paint)
            canvas.drawText("ACCOUNT", colBank, y, paint) // New Column
            canvas.drawText("DESCRIPTION", colTitle, y, paint)
            canvas.drawText("CATEGORY", colCat, y, paint)
            canvas.drawText("AMOUNT", colAmt, y, paint)
            paint.isFakeBoldText = false
        }

        var currentY = 280f
        drawTableHeaders(currentY)
        currentY += 30f

        paint.textSize = 10f

        for ((index, tx) in transactions.withIndex()) {
            // Pagination
            if (currentY > pageHeight - 50) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // Simple Header
                paint.color = primaryColor
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 40f, paint)
                paint.color = Color.WHITE
                paint.textSize = 12f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("SpendWise Report - Page $pageNumber", 20f, 25f, paint)

                currentY = 80f
                drawTableHeaders(currentY)
                currentY += 30f
                paint.textSize = 10f
            }

            // Zebra Striping
            if (index % 2 != 0) {
                paint.color = Color.parseColor("#F9F9F9")
                canvas.drawRect(20f, currentY - 12f, 575f, currentY + 8f, paint)
            }

            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.LEFT

            // Data
            canvas.drawText(DateUtils.formatDate(tx.date).dropLast(5), colDate, currentY, paint)

            // BANK NAME (New)
            val bank = walletMap[tx.walletId] ?: "Unknown"
            canvas.drawText(bank.take(12), colBank, currentY, paint)

            canvas.drawText(tx.title.take(20), colTitle, currentY, paint)

            paint.color = Color.GRAY
            canvas.drawText(tx.category.substringBefore(" -> ").take(15), colCat, currentY, paint)

            val isIncome = tx.amount > 0 || tx.type == "INCOME"
            paint.color = if (tx.amount >= 0) incomeColor else expenseColor
            paint.isFakeBoldText = true
            val sign = if (tx.amount >= 0) "+" else "-"
            canvas.drawText("$sign ${CurrencyUtils.formatINR(abs(tx.amount))}", colAmt, currentY, paint)
            paint.isFakeBoldText = false

            currentY += 25f
        }

        pdfDocument.finishPage(page)

        // Save & Share
        val file = File(context.cacheDir, "SpendWise_Report_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            sharePdf(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun sharePdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}