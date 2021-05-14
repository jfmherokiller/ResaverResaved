package ess.papyrus

import PlatformByteBuffer
import ess.WStringElement
import resaver.Analysis
import java.io.IOException

/**
 * TString implementation for 32 bit TStrings.
 */
internal class TString32 : TString {
    /**
     * Creates a new `TString32` from a `WStringElement` and
     * an index.
     *
     * @param wstr  The `WStringElement`.
     * @param index The index of the `TString`.
     */
    constructor(wstr: WStringElement?, index: Int) : super(wstr, index) {}

    /**
     * Creates a new `TString32` from a character sequence and an
     * index.
     *
     * @param cs    The `CharSequence`.
     * @param index The index of the `TString`.
     */
    private constructor(cs: CharSequence, index: Int) : super(cs, index) {}
    constructor(`val`: String?, size: Int) : super(`val`, size) {}

    /**
     * @param output The output stream.
     * @throws IOException
     * @see ess.Element.write
     */
    @Throws(IOException::class)
    override fun write(output: PlatformByteBuffer?) {
        output!!.putInt(this.index)
    }

    /**
     * @return The size of the `Element` in bytes.
     * @see ess.Element.calculateSize
     */
    override fun calculateSize(): Int {
        return 4
    }

    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        return false
    }
}