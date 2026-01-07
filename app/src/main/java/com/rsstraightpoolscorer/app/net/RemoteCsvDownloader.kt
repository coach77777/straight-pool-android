package com.rsstraightpoolscorer.app.net

import android.content.Context
import java.io.File
import java.net.URL

object RemoteCsvDownloader {

    fun download(context: Context, url: String, filename: String): File {
        val target = File(context.filesDir, filename)
        URL(url).openStream().use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return target
    }
}

