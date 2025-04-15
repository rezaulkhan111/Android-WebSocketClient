package com.encept.websocket_client

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream

object Utils {
    fun writeStringToTextFile(data: String, fileName: String?) {
        val sdCard = Environment.getExternalStorageDirectory()
        val dir = File(sdCard.absolutePath + "/MMDLogs")
        dir.mkdirs()
        val file = File(dir, fileName)
        if (!(file.exists())) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
                Log.d("file", e.message!!)
                return
            }
        }

        try {
            val f1 = FileOutputStream(file, true)

            val p = PrintStream(f1)
            p.print(data + System.getProperty("line.separator"))
            p.close()
            f1.close()
            Log.d("file string", data)
            Log.d("file", "file created")
        } catch (e: FileNotFoundException) {
            println("file not found")
            Log.d("file", "file not found")
        } catch (e: IOException) {
            println("ioexception")
            Log.d("file", "ioexception")
        } catch (e: Exception) {
            Log.d("file", "exception")
        }
    }

    fun writeStringToTextFile2(context: Context, data: String, fileName: String) {
        val dir = File(context.getExternalFilesDir(null), "MMDLogs")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, fileName)
        try {
            if (!file.exists()) {
                file.createNewFile()
            }

            val f1 = FileOutputStream(file, true)
            val p = PrintStream(f1)
            p.print(data + System.lineSeparator())
            p.close()
            f1.close()

            Log.d("file", "File written successfully: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("file", "Error writing file: ${e.message}")
        }
    }

    const val CALL_TYPE_KEY = "call Type"
    const val CALL_TYPE_SEND_KEY = "call Type send"
    const val CALL_TYPE_RECIVED_KEY = "call Type recive"
}