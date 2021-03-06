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
package ess.papyrus

import PlatformByteBuffer
import ess.*
import java.nio.charset.StandardCharsets


/**
 * Describes the FragmentTask field of an `ActiveScriptData`.
 *
 * @author Mark Fairchild
 */
class FragmentTask(input: PlatformByteBuffer, unknown3: Byte, context: PapyrusContext) : PapyrusElement, Linkable {
    /**
     * @see Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        assert(null != output)
        assert(null != TYPECODE || null != VARIABLE)

        // Corresponds to the unknown3 == 2 case.
        if (null != TYPECODE) {
            output!!.putInt(TYPECODE.size)
            output.put(TYPECODE)
        }
        DATA!!.write(output)
        VARIABLE?.write(output)
    }

    /**
     * @see Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 0
        if (null != TYPECODE) {
            sum = 4 + TYPECODE.size
        }
        sum += DATA?.calculateSize() ?: 0
        sum += VARIABLE?.calculateSize() ?: 0
        return sum
    }

    /**
     * @see Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        assert(null != TYPECODE || null != VARIABLE)
        val BUILDER = StringBuilder()
        BUILDER.append("UNK4:")
        if (null != TYPECODE) {
            BUILDER.append(TYPE)
        }
        BUILDER.append(DATA!!.toHTML(target))
        if (null != VARIABLE) {
            BUILDER.append(" (").append(VARIABLE!!.toHTML(null)).append(")")
        }
        return BUILDER.toString()
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUILDER = StringBuilder()
        BUILDER.append("FragmentTask")
        if (null != TYPECODE) {
            BUILDER.append("(").append(TYPE).append("): ")
        }
        BUILDER.append(DATA)
        if (null != VARIABLE) {
            BUILDER.append("(").append(VARIABLE).append(")")
        }
        return BUILDER.toString()
    }

    val TYPECODE: ByteArray?

    var TYPE: FragmentType? = null
    var DATA: FragmentData? = null
    var VARIABLE: Variable? = null

    //final public RefID QUESTID;
    //final public Byte BYTE;
    //final public Integer INT;
    //final public Integer UNKNOWN_4BYTES;
    //final public TString TSTRING;
    //final public Short STAGE;
    //final public ChangeForm FORM;
    enum class FragmentType {
        QuestStage, ScenePhaseResults, SceneActionResults, SceneResults, TerminalRunResults, TopicInfo
    }

    interface FragmentData : Linkable, PapyrusElement

    /**
     * Stores the data for the other type of fragment.
     */
    class Type2(input: PlatformByteBuffer?, context: PapyrusContext) : FragmentData {
        /**
         * @see Element.write
         * @param output The output stream.
         */
        override fun write(output: PlatformByteBuffer?) {
            RUNNING_ID?.write(output)
        }

        /**
         * @see Element.calculateSize
         * @return The size of the `Element` in bytes.
         */
        override fun calculateSize(): Int {
            return RUNNING_ID?.calculateSize() ?: 0
        }

        /**
         * @see Linkable.toHTML
         * @param target A target within the `Linkable`.
         * @return
         */
        override fun toHTML(target: Element?): String {
            val BUILDER = StringBuilder()
            BUILDER.append("FragmentTask.Type2")
            if (null != RUNNING) {
                BUILDER.append(" ").append(RUNNING!!.toHTML(null))
            } else if (null != RUNNING_ID) {
                BUILDER.append(" ").append(RUNNING_ID)
            }
            return BUILDER.toString()
        }

        /**
         * @return String representation.
         */
        override fun toString(): String {
            val BUILDER = StringBuilder()
            BUILDER.append("Type2")
            if (null != RUNNING_ID) {
                BUILDER.append(RUNNING_ID).append(" ")
            }
            return BUILDER.toString()
        }

        var RUNNING_ID: EID? = null
        private var RUNNING: ActiveScript? = null

        init {
            if (context.game?.isFO4 == true) {
                RUNNING_ID = input?.let { context.readEID32(it) }
                RUNNING = context.findActiveScript(RUNNING_ID)
            } else {
                RUNNING_ID = null
                RUNNING = null
            }
        }
    }

    /**
     * Stores the data for a QuestStage fragment.
     */
    class QuestStage(input: PlatformByteBuffer, context: PapyrusContext) : FragmentData {
        override fun write(output: PlatformByteBuffer?) {
            QUESTID.write(output)
            output!!.putShort(STAGE)
            FLAGS.write(output)
            if (null != UNKNOWN_4BYTES) {
                output.putInt(UNKNOWN_4BYTES)
            }
        }

        override fun calculateSize(): Int {
            var sum = 2
            sum += QUESTID.calculateSize()
            sum += FLAGS.calculateSize()
            sum += if (UNKNOWN_4BYTES == null) 0 else 4
            return sum
        }

        override fun toHTML(target: Element?): String {
            val BUF = StringBuilder()
            BUF.append(QUEST?.toHTML(target) ?: QUESTID)
            return BUF.append(" stage=")
                .append(STAGE.toInt())
                .append(" ").append(FLAGS)
                .append(UNKNOWN_4BYTES)
                .toString()
        }

        override fun toString(): String {
            val BUF = StringBuilder()
            BUF.append(QUEST ?: QUESTID)
            return BUF.append(" stage=")
                .append(STAGE.toInt())
                .append(" ").append(FLAGS)
                .append(UNKNOWN_4BYTES)
                .toString()
        }

        val QUESTID: RefID = context.readRefID(input)
        val STAGE: Short = input.getShort()
        val FLAGS: Flags.FlagsByte = Flags.readByteFlags(input)
        val UNKNOWN_4BYTES: Int? = if (context.game?.isFO4 == true) input.getInt() else null
        val QUEST: ChangeForm? = context.getChangeForm(QUESTID)

    }

    /**
     * Stores the data for a ScenePhaseResults fragment.
     */
    class ScenePhaseResults(input: PlatformByteBuffer, context: PapyrusContext) : FragmentData {
        /**
         * @see Element.write
         * @param output The output stream.
         */
        override fun write(output: PlatformByteBuffer?) {
            QUESTID.write(output)
            output!!.putInt(INT)
            if (null != UNKNOWN_4BYTES) {
                output.putInt(UNKNOWN_4BYTES)
            }
        }

        override fun calculateSize(): Int {
            var sum = 4
            sum += QUESTID.calculateSize()
            sum += if (UNKNOWN_4BYTES == null) 0 else 4
            return sum
        }

        override fun toHTML(target: Element?): String {
            val BUF = StringBuilder()
            BUF.append(QUEST?.toHTML(target) ?: QUESTID)
            return BUF.append(" stage=")
                .append(INT)
                .append(UNKNOWN_4BYTES)
                .toString()
        }

        override fun toString(): String {
            val BUF = StringBuilder()
            BUF.append(QUEST ?: QUESTID)
            return BUF.append(" stage=")
                .append(INT)
                .append(UNKNOWN_4BYTES)
                .toString()
        }

        val QUESTID: RefID = context.readRefID(input)
        val INT: Int = input.getInt()
        val UNKNOWN_4BYTES: Int? = if (context.game?.isFO4 == true) input.getInt() else null
        val QUEST: ChangeForm? = context.getChangeForm(QUESTID)

    }

    /**
     * Stores the data for a SceneActionResults fragment.
     */
    class SceneActionResults(input: PlatformByteBuffer, context: PapyrusContext) : FragmentData {
        /**
         * @see Element.write
         * @param output The output stream.
         */
        override fun write(output: PlatformByteBuffer?) {
            QUESTID.write(output)
            output!!.putInt(INT)
            if (null != UNKNOWN_4BYTES) {
                output.putInt(UNKNOWN_4BYTES)
            }
        }

        override fun calculateSize(): Int {
            var sum = 4
            sum += QUESTID.calculateSize()
            sum += if (UNKNOWN_4BYTES == null) 0 else 4
            return sum
        }

        override fun toHTML(target: Element?): String {
            val BUF = StringBuilder()
            BUF.append(QUEST?.toHTML(target) ?: QUESTID)
            return BUF.append(" stage=")
                .append(INT)
                .append(UNKNOWN_4BYTES)
                .toString()
        }

        override fun toString(): String {
            val BUF = StringBuilder()
            BUF.append(QUEST ?: QUESTID)
            return BUF.append(" stage=")
                .append(INT)
                .append(UNKNOWN_4BYTES)
                .toString()
        }

        val QUESTID: RefID = context.readRefID(input)
        val INT: Int = input.getInt()
        val UNKNOWN_4BYTES: Int? = if (context.game?.isFO4 == true) input.getInt() else null
        val QUEST: ChangeForm? = context.getChangeForm(QUESTID)

    }

    /**
     * Stores the data for a SceneResults fragment.
     */
    class SceneResults(input: PlatformByteBuffer, context: PapyrusContext) : FragmentData {
        /**
         * @see Element.write
         * @param output The output stream.
         */
        override fun write(output: PlatformByteBuffer?) {
            QUESTID.write(output)
            if (null != UNKNOWN_4BYTES) {
                output!!.putInt(UNKNOWN_4BYTES)
            }
        }

        override fun calculateSize(): Int {
            var sum = QUESTID.calculateSize()
            sum += if (UNKNOWN_4BYTES == null) 0 else 4
            return sum
        }

        override fun toHTML(target: Element?): String {
            val BUF = StringBuilder()
            BUF.append(QUEST?.toHTML(target) ?: QUESTID)
            return BUF.append(" stage=")
                .append(UNKNOWN_4BYTES)
                .toString()
        }

        override fun toString(): String {
            val BUF = StringBuilder()
            BUF.append(QUEST ?: QUESTID)
            return BUF.append(" stage=")
                .append(UNKNOWN_4BYTES)
                .toString()
        }

        val QUESTID: RefID = context.readRefID(input)
        val UNKNOWN_4BYTES: Int? = if (context.game?.isFO4 == true) input.getInt() else null
        val QUEST: ChangeForm? = context.getChangeForm(QUESTID)

    }

    /**
     * Stores the data for a TerminalRunResults fragment.
     */
    class TerminalRunResults(input: PlatformByteBuffer, context: PapyrusContext) : FragmentData {
        /**
         * @see Element.write
         * @param output The output stream.
         */
        override fun write(output: PlatformByteBuffer?) {
            output!!.put(BYTE)
            output.putInt(INT)
            REFID.write(output)
            TSTRING.write(output)
        }

        override fun calculateSize(): Int {
            var sum = 5
            sum += REFID.calculateSize()
            sum += TSTRING.calculateSize()
            return sum
        }

        override fun toHTML(target: Element?): String {
            return StringBuilder()
                .append(FORM?.toHTML(target) ?: REFID.toHTML(target))
                .append(" ").append(TSTRING)
                .append(String.format(" %08x %02x", INT, BYTE))
                .toString()
        }

        override fun toString(): String {
            val BUF = StringBuilder()
            return StringBuilder()
                .append(FORM ?: REFID)
                .append(" ").append(TSTRING)
                .append(String.format(" %08x %02x", INT, BYTE))
                .toString()
        }

        val BYTE: Byte = input.getByte()
        val INT: Int = input.getInt()
        val REFID: RefID = context.readRefID(input)
        val TSTRING: TString = context.readTString(input)
        val FORM: ChangeForm? = context.getChangeForm(REFID)

    }

    /**
     * Stores the data for a SceneActionResults fragment.
     */
    class TopicInfo(input: PlatformByteBuffer?, context: PapyrusContext?) : FragmentData {
        /**
         * @see Element.write
         * @param output The output stream.
         */
        override fun write(output: PlatformByteBuffer?) {}
        override fun calculateSize(): Int {
            return 0
        }

        override fun toHTML(target: Element?): String {
            return ""
        }

        override fun toString(): String {
            return ""
        }
    }

    /**
     * Creates a new `Unknown4` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param unknown3 The value of the unknown3 field.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusFormatException
     */
    init {
        try {
            assert(unknown3 in 1..3)
            if (unknown3.toInt() == 2) {
                DATA = Type2(input, context)
                TYPECODE = null
                TYPE = null
            } else {
                TYPECODE = mf.BufferUtil.getLStringRaw(input)
                TYPE = TYPECODE?.let { String(it, StandardCharsets.UTF_8) }?.let { FragmentType.valueOf(it) }
                DATA = when (TYPE) {
                    FragmentType.QuestStage -> QuestStage(input, context)
                    FragmentType.ScenePhaseResults -> ScenePhaseResults(input, context)
                    FragmentType.SceneActionResults -> SceneActionResults(input, context)
                    FragmentType.SceneResults -> SceneResults(input, context)
                    FragmentType.TerminalRunResults -> TerminalRunResults(input, context)
                    FragmentType.TopicInfo -> TopicInfo(input, context)
                    else -> throw PapyrusFormatException("Unknown ActiveScript QuestData")
                }
            }
            VARIABLE = if (unknown3.toInt() == 3 || unknown3.toInt() == 2) Variable.read(input, context) else null
        } catch (ex: PapyrusFormatException) {
            throw ex
        }
    }
}