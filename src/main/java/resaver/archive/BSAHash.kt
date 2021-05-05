/*
 * Copyright 2016 Mark Fairchild.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package resaver.archive

import java.io.File
import resaver.archive.BSAHash
import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException
import java.util.regex.Pattern

/**
 * Calculates filename and directory name hashes.
 *
 * INCOMPLETE
 *
 * @author Mark Fairchild
 */
object BSAHash {
    /**
     *
     * @param file
     * @return
     */
    fun genHashFile(file: File): Long {
        var hash: Long = 0
        var hash2: Long = 0
        val MATCHER = FILENAME_PATTERN.matcher(file.name)
        require(MATCHER.matches()) { "Filename does not have the form \"filename.extension\"" }
        val fileName = if (MATCHER.matches()) MATCHER.group(1).toLowerCase() else file.name.toLowerCase()
        val fileExt = if (MATCHER.matches()) MATCHER.group(2).toLowerCase() else ""
        for (ch in fileExt.toCharArray()) {
            hash *= 0x1003f
            hash += ch.toLong()
        }
        val len = fileName.length
        val chars = fileName.toCharArray()
        for (i in 1 until len - 2) {
            hash2 *= 0x1003f
            hash2 += chars[i].toInt()
        }
        hash += hash2
        hash2 = 0
        hash = hash shl 32
        hash2 = chars[len - 1].toLong()
        hash2 = if (len > 2) {
            hash2 or chars[len - 2].toLong()
        } else {
            hash2 or 0
        }
        hash2 = hash2 or (len.toLong() shl 16)
        hash2 = hash2 or (chars[0].toLong() shl 24)
        throw UnsupportedOperationException()
    }

    private const val FILENAME_REGEX = "^(.*)(\\.\\w+)$"
    private val FILENAME_PATTERN = Pattern.compile(FILENAME_REGEX)
}