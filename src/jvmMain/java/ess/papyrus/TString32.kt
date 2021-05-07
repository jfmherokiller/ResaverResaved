package ess.papyrus

import resaver.Analysis
import ess.WStringElement
import java.nio.ByteBuffer

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
     * @see resaver.ess.Element.write
     */
    override fun write(output: ByteBuffer?) {
        output!!.putInt(this.index)
    }

    /**
     * @return The size of the `Element` in bytes.
     * @see resaver.ess.Element.calculateSize
     */
    override fun calculateSize(): Int {
        return 4
    }

    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        return false
    }
}