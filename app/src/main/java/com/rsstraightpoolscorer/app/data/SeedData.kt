package com.rsstraightpoolscorer.app.data

import android.content.Context
import java.io.File

object SeedData {
    fun copyAssetIfMissing(
        ctx: Context,
        assetPath: String,
        outName: String
    ) {
        val outDir = File(ctx.filesDir, "remote")
        if (!outDir.exists()) outDir.mkdirs()

        val outFile = File(outDir, outName)
        if (outFile.exists()) return

        ctx.assets.open(assetPath).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
