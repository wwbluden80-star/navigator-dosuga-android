package ru.navigatordosuga.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.navigatordosuga.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

data class WebResponse(val code:Int,val body:String?,val etag:String?,val lastModified:String?)

class WebDataClient(private val baseUrl:String = BuildConfig.WEB_BASE_URL.trimEnd('/')) {
    val configured: Boolean get() = baseUrl.startsWith("https://") && !baseUrl.contains("REPLACE_WITH")
    suspend fun get(path:String, etag:String?=null): WebResponse = withContext(Dispatchers.IO) {
        if (!configured) return@withContext WebResponse(0,null,null,null)
        val u = if(path.startsWith("http")) path else "$baseUrl${if(path.startsWith('/')) path else "/$path"}"
        val conn=(URL(u).openConnection() as HttpURLConnection).apply {
            requestMethod="GET"; connectTimeout=12000; readTimeout=20000
            setRequestProperty("Accept","application/json")
            setRequestProperty("User-Agent","NavigatorDosugaAndroid/${BuildConfig.VERSION_NAME}")
            if(!etag.isNullOrBlank()) setRequestProperty("If-None-Match",etag)
        }
        try {
            val code=conn.responseCode
            val body=when {
                code==304 -> null
                code in 200..299 -> conn.inputStream.bufferedReader().use { it.readText() }
                else -> conn.errorStream?.bufferedReader()?.use { it.readText() }
            }
            WebResponse(code,body,conn.getHeaderField("ETag"),conn.getHeaderField("Last-Modified"))
        } finally { conn.disconnect() }
    }
    suspend fun postJson(path:String, body:String): WebResponse = withContext(Dispatchers.IO) {
        if (!configured) return@withContext WebResponse(0,null,null,null)
        val u = if(path.startsWith("http")) path else "$baseUrl${if(path.startsWith('/')) path else "/$path"}"
        val conn=(URL(u).openConnection() as HttpURLConnection).apply {
            requestMethod="POST"; connectTimeout=12000; readTimeout=20000; doOutput=true
            setRequestProperty("Accept","application/json"); setRequestProperty("Content-Type","application/json; charset=utf-8")
            setRequestProperty("User-Agent","NavigatorDosugaAndroid/${BuildConfig.VERSION_NAME}")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code=conn.responseCode
            val response=if(code in 200..299) conn.inputStream.bufferedReader().use{it.readText()} else conn.errorStream?.bufferedReader()?.use{it.readText()}
            WebResponse(code,response,conn.getHeaderField("ETag"),conn.getHeaderField("Last-Modified"))
        } finally { conn.disconnect() }
    }

}
