package com.rsstraightpoolscorer.app.net

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object RemoteCsvDownloader {

    suspend fun download(context: Context, url: String, filename: String): File =
        withContext(Dispatchers.IO) {
            val target = File(context.filesDir, filename)
            URL(url).openStream().use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            target
        }
}


