package com.shrawan.mediyo.music.utils

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LOG_DIR = "logs"
private const val DECLARED_LOG_FILE = "declared.log"
private const val TIMBER_LOG_FILE = "timber.log"

object AppLogs {
    private val lock = Any()

    private fun declaredLogFile(context: Context): File = context.filesDir.resolve(LOG_DIR).resolve(DECLARED_LOG_FILE)

    fun read(context: Context): String {
        val file = declaredLogFile(context)
        return if (file.exists()) file.readText() else ""
    }

    fun clear(context: Context) {
        declaredLogFile(context).delete()
    }

    fun declare(context: Context, section: String, message: String, throwable: Throwable? = null) {
        appendLine(
            declaredLogFile(context),
            "I",
            section,
            message,
            throwable
        )
    }

    fun declare(context: Context, section: String, throwable: Throwable) {
        declare(context, section, throwable.message.orEmpty(), throwable)
    }

    private fun appendLine(file: File, level: String, tag: String, message: String, throwable: Throwable?) {
        file.parentFile?.mkdirs()
        synchronized(lock) {
            val builder = StringBuilder()
                .append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()))
                .append(' ')
                .append(level)
                .append('/')
                .append(tag)
                .append(": ")
                .append(message)

            if (throwable != null) {
                builder.append('\n').append(Log.getStackTraceString(throwable))
            }

            file.appendText(builder.append('\n').toString())
        }
    }
}

class FileTree(
    private val context: Context,
) : Timber.DebugTree() {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)

        val file = context.filesDir.resolve(LOG_DIR).resolve(TIMBER_LOG_FILE)
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
