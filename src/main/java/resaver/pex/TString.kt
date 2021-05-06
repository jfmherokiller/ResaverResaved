package resaver.pex

import mf.BufferUtil
import resaver.WString
import java.io.IOException
import java.nio.ByteBuffer

/**
 * A case-insensitive string with value semantics that reads and writes as a
 * two-byte index into a string table.
 *
 * @author Mark Fairchild
 */
class TString
/**
 * Creates a new `TString` from a character sequence and an
 * index.
 *
 * @param cs    The `CharSequence`.
 * @param index The index of the `TString`.
 */ internal constructor(cs: CharSequence?, INDEX: Int) : WString(cs) {
    val INDEX = INDEX

    /**
     * @param output The output stream.
     * @throws IOException
     * @see WString.write
     */
    @Throws(IOException::class)
    fun writeFull(output: ByteBuffer?) {
        BufferUtil.putWString(output, super.toString())
    }

    /**
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        output?.putShort(INDEX.toShort())
    }
}