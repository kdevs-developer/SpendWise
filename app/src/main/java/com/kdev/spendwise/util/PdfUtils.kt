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
import kotlin.math.abs

object PdfUtils {

    fun exportTransactionsToPdf(
        context: Context,
        transactions: List<Expense>,
        initialBalance: Double,   // Opening Balance
        totalIncome: Double,      // Income for period
        totalExpense: Double,     // Expense for period
        availableBalance: Double  // Closing Balance
    ) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        // Create Page 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val paint = Paint()

        // --- COLORS ---
        val primaryColor = Color.parseColor("#4F5B93")
        val incomeColor = Color.parseColor("#388E3C")
        val expenseColor = Color.parseColor("#D32F2F")
        val lightGray = Color.parseColor("#F8F9FA")

        // ==========================================
        // DRAW PAGE 1 HEADER (Logo & Summary)
        // ==========================================
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 100f, paint)

        // --- LOGO DRAWING LOGIC ---
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

        // Header Text
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("SpendWise", 90f, 55f, paint)
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Financial Report", 90f, 75f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Generated: ${DateUtils.formatDate(System.currentTimeMillis())}", 570f, 55f, paint)

        // Summary Cards
        paint.textAlign = Paint.Align.LEFT
        val cardY = 120f
        paint.color = lightGray
        canvas.drawRoundRect(20f, cardY, 575f, cardY + 90f, 20f, 20f, paint)

        paint.textSize = 11f
        paint.color = Color.GRAY
        canvas.drawText("OPENING BALANCE", 45f, cardY + 30f, paint)
        canvas.drawText("INCOME", 220f, cardY + 30f, paint)
        canvas.drawText("EXPENSE", 380f, cardY + 30f, paint)

        paint.textSize = 15f
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        canvas.drawText(CurrencyUtils.formatINR(initialBalance), 45f, cardY + 60f, paint)
        paint.color = incomeColor
        canvas.drawText("+ ${CurrencyUtils.formatINR(totalIncome)}", 220f, cardY + 60f, paint)
        paint.color = expenseColor
        canvas.drawText("- ${CurrencyUtils.formatINR(totalExpense)}", 380f, cardY + 60f, paint)

        // Closing Balance Bar
        paint.color = primaryColor
        canvas.drawRect(20f, cardY + 90f, 575f, cardY + 130f, paint)
        paint.color = Color.WHITE
        paint.textSize = 14f
        canvas.drawText("CLOSING BALANCE", 45f, cardY + 116f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatINR(availableBalance), 550f, cardY + 116f, paint)

        // ==========================================
        // TABLE SETUP
        // ==========================================
        val colDate = 35f
        val colTitle = 135f
        val colCategory = 320f
        val colAmount = 480f

        // Helper to draw table headers
        fun drawTableHeaders(y: Float) {
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#E8EAF6")
            canvas.drawRect(20f, y - 20f, 575f, y + 10f, paint)

            paint.color = primaryColor
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText("DATE", colDate, y, paint)
            canvas.drawText("DESCRIPTION", colTitle, y, paint)
            canvas.drawText("CATEGORY", colCategory, y, paint)
            canvas.drawText("AMOUNT", colAmount, y, paint)
            paint.isFakeBoldText = false
        }

        var currentY = 285f
        drawTableHeaders(currentY)
        currentY += 35f

        // ==========================================
        // ROWS WITH PAGINATION
        // ==========================================
        paint.textSize = 11f

        for ((index, expense) in transactions.withIndex()) {

            // --- PAGINATION LOGIC ---
            if (currentY > pageHeight - 50) {
                // 1. Finish current page
                pdfDocument.finishPage(page)

                // 2. Start new page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // 3. Draw Simplified Header on new page
                paint.color = primaryColor
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 60f, paint)
                paint.color = Color.WHITE
                paint.textSize = 16f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("SpendWise Report (Page $pageNumber)", 30f, 38f, paint)

                // 4. Reset Y and draw table header again
                currentY = 100f
                drawTableHeaders(currentY)
                currentY += 35f
                paint.textSize = 11f
            }

            // Draw Zebra Striping
            if (index % 2 != 0) {
                paint.color = Color.parseColor("#FDFDFD")
                canvas.drawRect(20f, currentY - 18f, 575f, currentY + 10f, paint)
            }

            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(DateUtils.formatDate(expense.date), colDate, currentY, paint)

            val safeTitle = if (expense.title.length > 22) expense.title.take(20) + ".." else expense.title
            canvas.drawText(safeTitle, colTitle, currentY, paint)

            paint.color = Color.GRAY
            val catShort = if (expense.category.length > 20) expense.category.take(18) + ".." else expense.category
            canvas.drawText(catShort, colCategory, currentY, paint)

            val isIncome = expense.amount > 0 || expense.type == "INCOME"
            // Special handling for Transfer (could be neg or pos depending on logic, but usually we just show amount)
            // Assuming amount in Expense object is signed correctly
            val amountText = if(isIncome) "+ ${CurrencyUtils.formatINR(abs(expense.amount))}" else "- ${CurrencyUtils.formatINR(abs(expense.amount))}"

            paint.isFakeBoldText = true
            paint.color = if (expense.amount >= 0) incomeColor else expenseColor
            canvas.drawText(amountText, colAmount, currentY, paint)

            paint.isFakeBoldText = false
            currentY += 30f
        }

        // Finish the final page
        pdfDocument.finishPage(page)

        // Save File
        val file = File(context.cacheDir, "SpendWise_Report.pdf")
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