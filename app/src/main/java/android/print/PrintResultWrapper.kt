package android.print

import android.print.PrintDocumentAdapter.LayoutResultCallback
import android.print.PrintDocumentAdapter.WriteResultCallback

/**
 * A helper class placed in the 'android.print' package to bypass the package-private 
 * constructor restriction of LayoutResultCallback and WriteResultCallback.
 */
object PrintResultWrapper {

    fun getLayoutCallback(
        onLayoutFinished: (info: PrintDocumentInfo?, changed: Boolean) -> Unit,
        onLayoutFailed: (error: CharSequence?) -> Unit,
        onLayoutCancelled: () -> Unit = {}
    ): LayoutResultCallback {
        return object : LayoutResultCallback() {
            override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                onLayoutFinished(info, changed)
            }

            override fun onLayoutFailed(error: CharSequence?) {
                onLayoutFailed(error)
            }

            override fun onLayoutCancelled() {
                onLayoutCancelled()
            }
        }
    }

    fun getWriteCallback(
        onWriteFinished: (pages: Array<out PageRange>?) -> Unit,
        onWriteFailed: (error: CharSequence?) -> Unit,
        onWriteCancelled: () -> Unit = {}
    ): WriteResultCallback {
        return object : WriteResultCallback() {
            override fun onWriteFinished(pages: Array<out PageRange>?) {
                onWriteFinished(pages)
            }

            override fun onWriteFailed(error: CharSequence?) {
                onWriteFailed(error)
            }

            override fun onWriteCancelled() {
                onWriteCancelled()
            }
        }
    }
}
