package com.rsstraightpoolscorer.app.data

import android.content.Context
import java.io.File

object SeedData {
    fun ensureRemoteDir(ctx: Context): File {
        val dir = File(ctx.filesDir, "remote")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun copyAssetIfMissing(ctx: Context, assetPath: String, outName: String) {
        val dir = ensureRemoteDir(ctx)
        val outFile = File(dir, outName)
        if (outFile.exists()) return

        ctx.assets.open(assetPath).use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
