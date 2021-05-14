package resaver.pex

import PlatformByteBuffer
import java.io.IOException

/**
 * Describe the debugging info section of a PEX file.
 */
class DebugInfo internal constructor(private val pexFile: PexFile, input: PlatformByteBuffer, strings: StringTable?) {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    fun write(output: PlatformByteBuffer) {
        output.put(hasDebugInfo)
        if (hasDebugInfo.toInt() != 0) {
            output.putLong(modificationTime)
            output.putShort(DEBUGFUNCTIONS.size.toShort())
            for (function in DEBUGFUNCTIONS) {
                function.write(output)
            }
            if (pexFile.GAME!!.isFO4) {
                output.putShort(PROPERTYGROUPS.size.toShort())
                for (function in PROPERTYGROUPS) {
                    function.write(output)
                }
                output.putShort(STRUCTORDERS.size.toShort())
                for (function in STRUCTORDERS) {
                    function.write(output)
                }
            }
        }
    }

    /**
     * Removes all debug info.
     */
    fun clear() {
        hasDebugInfo = 0
        DEBUGFUNCTIONS.clear()
        PROPERTYGROUPS.clear()
        STRUCTORDERS.clear()
    }

    /**
     * Collects all of the strings used by the DebugInfo and adds them to a
     * set.
     *
     * @param strings The set of strings.
     */
    fun collectStrings(strings: MutableSet<TString?>?) {

        if(strings != null) {
            for (DEBUGFUNCTION in DEBUGFUNCTIONS) {
                DEBUGFUNCTION.collectStrings(strings)
            }
            for (PROPERTYGROUP in PROPERTYGROUPS) {
                PROPERTYGROUP.collectStrings(strings.filterNotNull().toMutableSet())
            }
            for (f in STRUCTORDERS) {
                f.collectStrings(strings.filterNotNull().toMutableSet())
            }
        }
    }

    /**
     * @return The size of the `DebugInfo`, in bytes.
     */
    fun calculateSize(): Int {
        var sum = 1
        if (hasDebugInfo.toInt() != 0) {
            sum += 8
            val result1 = DEBUGFUNCTIONS.sumOf { it.calculateSize() }
            sum += 2 + result1
            if (pexFile.GAME!!.isFO4) {
                val sum1 = PROPERTYGROUPS.sumOf { it.calculateSize() }
                sum += 2 + sum1
                val result = STRUCTORDERS.sumOf { it.calculateSize() }
                sum += 2 + result
            }
        }
        return sum
    }

    /**
     * Pretty-prints the DebugInfo.
     *
     * @return A string representation of the DebugInfo.
     */
    override fun toString(): String {
        val buf = StringBuilder()
        buf.append("DEBUGINFO\n")
        for (function in DEBUGFUNCTIONS) {
            buf.append('\t').append(function).append('\n')
        }
        buf.append('\n')
        return buf.toString()
    }

    private var hasDebugInfo: Byte
    private var modificationTime: Long = 0
    private val DEBUGFUNCTIONS: ArrayList<DebugFunction>
    private val PROPERTYGROUPS: ArrayList<PropertyGroup>
    private val STRUCTORDERS: ArrayList<StructOrder>

    /**
     * Creates a DebugInfo by reading from a DataInput.
     *
     * @param input   A datainput for a Skyrim PEX file.
     * @param strings The `StringTable` for the
     * `PexFile`.
     * @throws IOException Exceptions aren't handled.
     */
    init {
        hasDebugInfo = input.getByte()
        DEBUGFUNCTIONS = ArrayList(0)
        PROPERTYGROUPS = ArrayList(0)
        STRUCTORDERS = ArrayList(0)
        if (hasDebugInfo.toInt() == 0) {
        } else {
            modificationTime = input.getLong()
            val functionCount = UtilityFunctions.toUnsignedInt(input.getShort())
            DEBUGFUNCTIONS.ensureCapacity(functionCount)
            for (i in 0 until functionCount) {
                DEBUGFUNCTIONS.add(DebugFunction(input, strings!!))
            }
            if (pexFile.GAME!!.isFO4) {
                val propertyCount = UtilityFunctions.toUnsignedInt(input.getShort())
                PROPERTYGROUPS.ensureCapacity(propertyCount)
                for (i in 0 until propertyCount) {
                    PROPERTYGROUPS.add(PropertyGroup(input, strings!!))
                }
                val orderCount = UtilityFunctions.toUnsignedInt(input.getShort())
                STRUCTORDERS.ensureCapacity(orderCount)
                for (i in 0 until orderCount) {
                    strings?.let { StructOrder(input, it) }?.let { STRUCTORDERS.add(it) }
                }
            }
        }
    }
}