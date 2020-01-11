package kim.guler.berkin.simpledownload

import kotlin.Exception

/**
 * Created by Berkin Güler on 11.01.2020.
 */

inline fun useResources(vararg resources: AutoCloseable, block: () -> Unit) {
    try {
        block.invoke()
    } catch (ex: Exception) {
        resources.forEach { it.close() }
        throw ex
    }
    resources.forEach { it.close() }
}