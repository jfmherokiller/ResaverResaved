package resaver.pex

import mf.BufferUtil
import kotlin.Throws
import java.io.IOException
import java.lang.StringBuilder
import java.nio.ByteBuffer

/**
 * Describes the header of a PexFile file. Useless beyond that.
 */
class Header internal constructor(input: ByteBuffer) {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    fun write(output: ByteBuffer) {
        output.putInt(magic)
        output.putInt(version)
        output.putLong(compilationTime)
        BufferUtil.putWString(output, soureFilename)
        BufferUtil.putWString(output, userName)
        BufferUtil.putWString(output, machineName)
    }

    /**
     * @return The size of the `Header`, in bytes.
     */
    fun calculateSize(): Int {
        return 22 + soureFilename.length + userName.length + machineName.length
    }

    /**
     * Pretty-prints the Header.
     *
     * @return A string representation of the Header.
     */
    override fun toString(): String {
        return StringBuilder()
            .append(soureFilename)
            .append(" compiled at ")
            .append(compilationTime)
            .append(" by ")
            .append(userName)
            .append(" on ")
            .append(machineName)
            .append(".\n")
            .toString()
    }

    private var magic = 0
    private var version = 0
    var compilationTime: Long = 0
    var soureFilename = ""
    private var userName = ""
    private var machineName = ""

    /**
     * Creates a Header by reading from a DataInput.
     *
     * @param input A ByteBuffer for a Skyrim PEX file.
     * @throws IOException Exceptions aren't handled.
     */
    init {
        magic = input.int
        version = input.int
        compilationTime = input.long
        soureFilename = BufferUtil.getUTF(input)
        userName = BufferUtil.getUTF(input)
        machineName = BufferUtil.getUTF(input)
    }
}