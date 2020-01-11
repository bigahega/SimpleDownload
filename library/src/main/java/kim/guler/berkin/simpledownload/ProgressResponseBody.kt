package kim.guler.berkin.simpledownload

import android.os.Handler
import android.os.Looper
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer

/**
 * Created by Berkin Güler on 11.01.2020.
 */
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressCallback: ((progress: Int) -> Unit)?,
    private val completionListener: (success: Boolean, exception: Exception?) -> Unit
) : ResponseBody() {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val bufferedSource by lazy {
        source(responseBody.source()).buffer()
    }

    override fun contentLength() = responseBody.contentLength()
    override fun contentType() = responseBody.contentType()
    override fun source() = bufferedSource

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            override fun read(sink: Buffer, byteCount: Long): Long {
                val read = super.read(sink, byteCount)
                if (read == -1L) {
                    mainHandler.post {
                        completionListener.invoke(true, null)
                    }
                } else {
                    totalBytesRead += read
                    mainHandler.post {
                        progressCallback?.invoke((totalBytesRead.toFloat() / contentLength().toFloat() * 100f).toInt())
                    }
                }
                return read
            }
        }
    }
}