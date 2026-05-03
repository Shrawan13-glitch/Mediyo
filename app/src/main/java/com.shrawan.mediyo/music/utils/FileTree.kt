package com.shrawan.mediyo.music.utils

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LOG_DIR = "logs"
private const val LOG_FILE = "mediyo.log"

object AppLogs {
    fun logFile(context: Context): File = context.filesDir.resolve(LOG_DIR).resolve(LOG_FILE)

    fun read(context: Context): String {
        val file = logFile(context)
        return if (file.exists()) file.readText() else ""
    }

    fun clear(context: Context) {
        logFile(context).delete()
    }
}

class FileTree(
    private val context: Context,
) : Timber.DebugTree() {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)

        val file = AppLogs.logFile(context)
        file.parentFile?.mkdirs()

        val level = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> priority.toString()
        }

        synchronized(lock) {
            val builder = StringBuilder()
                .append(formatter.format(Date()))
                .append(' ')
                .append(level)
                .append('/')
                .append(tag ?: "Mediyo")
                .append(": ")
                .append(message)

            if (t != null) {
                builder.append('\n').append(Log.getStackTraceString(t))
            }

            file.appendText(builder.append('\n').toString())
        }
    }
}
