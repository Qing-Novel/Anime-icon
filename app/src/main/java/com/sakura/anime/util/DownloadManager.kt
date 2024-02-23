package com.sakura.anime.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object DownloadManager {
    private val client = OkHttpClient.Builder().readTimeout(1L, TimeUnit.MINUTES).build()

    suspend fun getHtml(url: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            var html: String
            if (response.isSuccessful) {
                response.body!!.let { body ->
                    html = body.charStream().readText()
                }
            } else {
                throw IOException(response.toString())
            }
            html
        }
    }
}
