package com.fortioridesign.moxykaroo

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

object LocalCrashLogger {
    private const val CRASH_DIR = "crash_reports"
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    fun init(context: Context) {
        // Install handler once (preserve previous handler)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveThrowable(context, thread, throwable)
            } catch (e: Exception) {
                // ignore logging failures
            } finally {
                // Give the write a moment, then delegate to previous handler (or exit)
                try {
                    Thread.sleep(300)
                } catch (ignored: InterruptedException) {
                }
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveThrowable(context: Context, thread: Thread, t: Throwable) {
        val dir = File(context.filesDir, CRASH_DIR)
        if (!dir.exists()) dir.mkdirs()

        val ts = timestampFormat.format(Date())
        val file = File(dir, "crash_$ts.txt")

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println("Timestamp: $ts")
        pw.println("Thread: ${thread.name} (id=${thread.threadId()})")
        pw.println("Package: ${context.packageName}")
        pw.println()
        t.printStackTrace(pw)
        pw.flush()

        // write on a background thread
        thread {
            file.outputStream().bufferedWriter().use { it.write(sw.toString()) }
        }
    }
}