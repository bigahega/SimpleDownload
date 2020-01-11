package kim.guler.berkin.simpledownload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

/**
 * Created by Berkin Güler on 11.01.2020.
 */
object SimpleDownloader {

    private const val BUFFER_SIZE = 8 * 1024L

    suspend fun String.download(
        targetFilePath: String,
        completionListener: (success: Boolean, exception: Exception?) -> Unit,
        progressCallback: ((progress: Int) -> Unit)? = null
    ) {
        downloadURL(this, targetFilePath, completionListener, progressCallback)
    }

    suspend fun downloadURL(
        url: String,
        targetFilePath: String,
        completionListener: (success: Boolean, exception: Exception?) -> Unit,
        progressCallback: ((progress: Int) -> Unit)? = null
    ): Job {
        val request = Request.Builder().url(url).build()

        val client = with(OkHttpClient.Builder()) {
            addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                val responseBody = originalResponse.body
                originalResponse.newBuilder().body(
                    ProgressResponseBody(
                        responseBody!!,
                        progressCallback,
                        completionListener
                    )
                ).build()
            }
        }.build()

        return coroutineScope {
            launch(Dispatchers.IO) {
                try {
                    val execute = client.newCall(request).execute()
                    if (!isActive) {
                        return@launch
                    }
                    File(targetFilePath).mkdirs()
                    val body = execute.body ?: return@launch
                    val bufferedSource = body.source()
                    val bufferedSink = File(targetFilePath).sink().buffer()

                    useResources(bufferedSource, bufferedSink) {
                        var read: Long
                        do {
                            read = bufferedSource.read(bufferedSink.buffer, BUFFER_SIZE)
                            bufferedSink.emit()
                        } while (read > 0 && isActive)
                    }
                } catch (ex: Exception) {
                    completionListener.invoke(false, ex)
                }
            }
        }
    }
}