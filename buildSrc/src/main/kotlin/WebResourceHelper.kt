package com.lansent.gradle

import com.yahoo.platform.yui.compressor.YUICompressor
import org.apache.commons.io.FileUtils
import java.io.File

/**
 *
 * @author <a href="mailto:hyysguyang@gmail.com">Young Gu</a>
 */
open class WebResourceHelper(val srcFiles: List<File>, val type: String = "js", val isCompress: Boolean = true) {


    open fun compressFile(src: File): String {

        val tempDir = FileUtils.getTempDirectoryPath() + "/yui-compressor/"
        FileUtils.deleteDirectory(File(tempDir))
        FileUtils.forceMkdir(File(tempDir))

        val tempFile = "$tempDir/${src.name}"
        val cmdParam = "--type $type -o $tempFile ${src.absolutePath}"
        YUICompressor.main(cmdParam.split(" ").toTypedArray())
        return FileUtils.readFileToString(File(tempFile))
    }


    open fun transform(src: File): String = if (isCompress) compressFile(src) else FileUtils.readFileToString(src)

    fun compress(): String {
        val destContent = srcFiles.fold("") { result, tempFile ->
            result + "\n" + transform(tempFile)
        }
        return destContent
    }

    fun content() = compress()

}