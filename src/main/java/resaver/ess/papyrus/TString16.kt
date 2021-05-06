package resaver.ess.papyrus

import resaver.Analysis
import resaver.ess.WStringElement
import java.nio.ByteBuffer

/**
 * TString implementation for 16 bit TStrings.
 */
internal class TString16 : TString {
    private val tStrings: StringTable

    /**
     * Creates a new `TString16` from a `WStringElement` and
     * an index.
     *
     * @param wstr  The `WStringElement`.
     * @param index The index of the `TString`.
     */
    constructor(tStrings: StringTable, wstr: WStringElement?, index: Int) : super(wstr, index) {
        this.tStrings = tStrings
    }

    /**
     * Creates a new `TString16` from a character sequence and an
     * index.
     *
     * @param cs    The `CharSequence`.
     * @param index The index of the `TString`.
     */
    private constructor(tStrings: StringTable, cs: CharSequence, index: Int) : super(cs, index) {
        this.tStrings = tStrings
    }

    constructor(tStrings: StringTable, `val`: String?, size: Int) : super(`val`, size) {
        this.tStrings = tStrings
    }

    /**
     * @param output The output stream.
     * @see resaver.ess.Element.write
     */
    override fun write(output: ByteBuffer?) {
        if (this.index > 0xFFF0 && !tStrings.STBCORRECTION) {
            output!!.putShort(0xFFFF.toShort())
            output.putInt(this.index)
        } else {
            output!!.putShort(this.index.toShort())
        }
    }

    /**
     * @return The size of the `Element` in bytes.
     * @see resaver.ess.Element.calculateSize
     */
    override fun calculateSize(): Int {
        return if (this.index > 0xFFF0 && !tStrings.STBCORRECTION) 6 else 2
    }

    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        return false
    }
}