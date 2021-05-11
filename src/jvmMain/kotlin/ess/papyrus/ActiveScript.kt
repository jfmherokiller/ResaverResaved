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

import ess.*
import ess.Linkable.Companion.makeLink
import ess.papyrus.Variable.Companion.read
import mu.KotlinLogging
import resaver.Analysis
import resaver.ListException
import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * Describes an active script in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
private val logger = KotlinLogging.logger {}
class ActiveScript(input: ByteBuffer, context: PapyrusContext) : AnalyzableElement, HasID, SeparateData {
    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        iD.write(output)
        output?.put(type)
    }

    /**
     * @see SeparateData.readData
     * @param input
     * @param context
     * @throws PapyrusElementException
     * @throws PapyrusFormatException
     */
    @Throws(PapyrusElementException::class, PapyrusFormatException::class)
    override fun readData(input: ByteBuffer?, context: PapyrusContext?) {
        data = input?.let { context?.let { it1 -> ActiveScriptData(it, it1) } }
    }

    /**
     * @see SeparateData.writeData
     * @param input
     */
    override fun writeData(input: ByteBuffer?) {
        data!!.write(input)
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 1 + iD.calculateSize()
        sum += if (data == null) 0 else data!!.calculateSize()
        return sum
    }

    /**
     * Replaces the opcodes of each `StackFrame` with NOPs.
     */
    fun zero() {
        stackFrames.forEach { obj: StackFrame -> obj.zero() }
    }

    /**
     * Shortcut for getData().getStackFrames().
     *
     * @return
     */
    val stackFrames: List<StackFrame>
        get() = if (null != data) data!!.STACKFRAMES else emptyList()

    /**
     * Shortcut for getData().getAttached().
     *
     * @return The attached field.
     */
    val attached: EID?
        get() = if (null != data) data!!.ATTACHED else null

    /**
     * Shortcut for getData().getAttachedElement().
     *
     * @return
     */
    val attachedElement: HasID?
        get() = if (data == null) null else data!!.ATTACHED_ELEMENT

    /**
     * Tests if the activescript has any stackframes.
     *
     * @return
     */
    fun hasStack(): Boolean {
        return stackFrames.isNotEmpty()
    }// Suspended stacks aren't terminated.

    /**
     * @return A flag indicating if the `ActiveScript` is terminated.
     */
    val isTerminated: Boolean
        get() {
            // Suspended stacks aren't terminated.
            if (isSuspended || !hasStack()) {
                return false
            }
            val FIRST = stackFrames[0]
            if (FIRST.isNative || !FIRST.isZeroed) return false
            for (f in stackFrames) {
                if (!f.isZeroed && !f.isNative) {
                    return false
                }
            }
            return true
        }

    /**
     * @return A flag indicating if the `ActiveScript` is suspended.
     */
    val isSuspended: Boolean
        get() = suspendedStack != null

    /**
     * @see ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        return makeLink("thread", iD, this.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUILDER = StringBuilder()
        if (null == data) {
            return "$iD-${String.format("%02x", type)}"
        }
        if (isUndefined) {
            BUILDER.append("#")
        }
        if (hasStack()) {
            val topFrame = stackFrames[0]
            val scriptName = topFrame.scriptName
            BUILDER.append(scriptName).append(" ")
        } else if (suspendedStack != null && suspendedStack!!.message != null) {
            BUILDER.append(suspendedStack!!.message)
        } else {
        }
        BUILDER.append("(").append(iD).append(") ")
        BUILDER.append(stackFrames.size).append(" frames")
        val attached = attached
        if (null != attached && attached.isZero) {
            BUILDER.append(" (zero attached)")
        } else if (null != attached) {
            BUILDER.append(" (valid attached ").append(this.attached).append(")")
        }
        if (isTerminated) {
            BUILDER.append(" (TERMINATED)")
        } else if (isSuspended) {
            BUILDER.append(" (SUSPENDED)")
        }
        return BUILDER.toString()
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        when {
            isTerminated -> {
                BUILDER.append("<html><h3>ACTIVESCRIPT (TERMINATED)</h3>")
                BUILDER.append("<p><em>WARNING: SCRIPT TERMINATED!</em><br/>This thread has been terminated and all of its instructions erased.</p>")
            }
            isSuspended -> {
                BUILDER.append("<html><h3>ACTIVESCRIPT (SUSPENDED)</h3>")
                BUILDER.append("<p><em>WARNING: SCRIPT SUSPENDED!</em><br/>This script has been suspended. Terminating it may have unpredictable results.</p>")
                BUILDER.append("<p>Suspended stack: ").append(suspendedStack!!.toHTML(null)).append("</p>")
            }
            else -> {
                BUILDER.append("<html><h3>ACTIVESCRIPT</h3>")
            }
        }
        if (isUndefined) {
            BUILDER.append("<p><em>WARNING: SCRIPT MISSING!</em><br/>Remove Undefined Instances\" will terminate this thread.</p>")
        }
        BUILDER.append("<p>Attachment ID: ").append(attached).append("</p>")
        if (null != attachedElement) {
            BUILDER.append("<p>Attachment element: ").append(attachedElement!!.toHTML(this)).append("</p>")
        } else if (attached != null && !attached!!.isZero) {
            BUILDER.append("<p>Attachment element: <em>not found</em></p>")
        } else {
            BUILDER.append("<p>Attachment element: <em>None</em></p>")
        }
        if (null != analysis && hasStack()) {
            val topFrame = stackFrames[0]
            val mods = analysis.SCRIPT_ORIGINS[topFrame.scriptName.toIString()]
            if (null != mods) {
                val mod = mods.last()
                BUILDER.append("<p>Probably running code from mod $mod.</p>")
            }
        }
        if (null == instance) {
            BUILDER.append("<p>This thread doesn't seem to be attached to an instance.</p>")
        } else if (instance is ScriptInstance) {
            val instance = instance as ScriptInstance?
            val REF = instance!!.refID
            val PLUGIN = REF.PLUGIN
            if (PLUGIN != null) {
                BUILDER.append(
                    "<p>This thread is attached to an instance from ${PLUGIN.toHTML(this)}.</p>"
                )
            } else if (instance.refID.type === RefID.Type.CREATED) {
                BUILDER.append("<p>This thread is attach to instance that was created in-game.</p>")
            }
        }
        when (instance) {
            null -> {
                BUILDER.append("<p>No owner was identified.</p>")
            }
            is Linkable -> {
                val l = instance as Linkable?
                val type = instance!!.javaClass.simpleName
                BUILDER.append("<p>$type: ${l!!.toHTML(this)}</p>")
            }
            else -> {
                val type = instance!!.javaClass.simpleName
                BUILDER.append("<p>$type: $instance</p>")
            }
        }
        BUILDER.append("<p>")
        BUILDER.append("ID: $iD<br/>")
        BUILDER.append(String.format("Type: %02x<br/>", type))
        if (null == data) {
            BUILDER.append("<h3>DATA MISSING</h3>")
        } else {
            BUILDER.append(String.format("Version: %d.%d<br/>", this.majorVersion, minorVersion))
            BUILDER.append("Unknown (var): ${this.unknownVar?.toHTML(this)}<br/>")
            BUILDER.append(String.format("Flag: %08x<br/>", flag))
            BUILDER.append(String.format("Unknown1 (byte): %02x<br/>", this.unknownByte))
            BUILDER.append(String.format("Unknown2 (int): %08x<br/>", this.unknown2))
            BUILDER.append(String.format("Unknown3 (byte): %02x<br/>", this.unknown3))
            if (null != this.unknown4) {
                BUILDER.append("FragmentData (struct): ${this.unknown4?.toHTML(this)}<br/>")
            } else {
                BUILDER.append("FragmentData (struct): ${this.unknown4}<br/>")
            }
            if (null != this.unknown5) {
                BUILDER.append(String.format("Unknown5 (byte): %02x<br/>", this.unknown5))
            } else {
                BUILDER.append("Unknown5 (byte): <em>absent</em><br/>")
            }
            val UNKNOWN2 = this.unknown2?.let { save?.papyrus!!.context.broadSpectrumSearch(it) }
            if (null != UNKNOWN2) {
                BUILDER.append("<p>Potential match for unknown2 found using general search:<br/>")
                BUILDER.append(UNKNOWN2.toHTML(this))
                BUILDER.append("</p>")
            }
        }
        BUILDER.append("</html>")
        return BUILDER.toString()
    }

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        for (v in stackFrames) {
            if (v.matches(analysis, mod)) {
                return true
            }
        }
        return false
    }

    /**
     * @param stacks The SuspendedStacks.
     */
    fun resolveStack(stacks: Map<EID?, SuspendedStack?>) {
        if (hasStack()) {
            val ref = stackFrames[0].owner
            if (ref is VarRef) {
                instance = ref.referent
            } else if (ref is VarNull) {
                instance = null
            }
        }
        suspendedStack = stacks.getOrDefault(iD, null)
    }

    /**
     * Checks if any of the stackframes in the `ActiveScript` is
     * running code from the specified `Script`.
     *
     * @param script The `Script` the check.
     * @return A flag indicating if any of the stackframes in the script matches
     * the specified `Script`.
     */
    fun hasScript(script: Script): Boolean {
        if (null == data) {
            return false
        }
        for (frame in stackFrames) {
            if (frame.script == script) {
                return true
            }
        }
        return false
    }
    /**
     * @return The major version field.
     */
    val majorVersion: Byte
        get() = data?.majorVersion!!

    /**
     * @return The minor version field.
     */
    val minorVersion: Int
        get() = data!!.MINORVERSION.toInt()

    /**
     * @return The flag field.
     */
    val flag: Int
        get() = data!!.FLAG.toInt()

    /**
     * @return The unknown variable field.
     */
    val unknownVar: Variable?
        get() = data?.unknownVar

    /**
     * @return The FragmentTask field.
     */
    val unknown4: FragmentTask?
        get() = data?.unknown4

    /**
     * @return The Unknown byte field.
     */
    val unknownByte: Byte?
        get() = data?.unknownByte

    /**
     * @return The Unknown2 field.
     */
    val unknown2: Int?
        get() = data?.unknown2


    /**
     * @return The Unknown3 field.
     */
    val unknown3: Byte?
        get() =  data?.unknown3


    /**
     * @return The Unknown5 field.
     */
    val unknown5: Byte?
        get() = data?.unknown5

    /**
     * @return A flag indicating if the `ActiveScript` is undefined.
     * An `ActiveScript` is undefined if any of its stack frames are
     * undefined.
     */
    val isUndefined: Boolean
        get() = when {
            null == data -> {
                false
            }
            hasStack() -> {
                for (stackFrame in stackFrames) {
                    stackFrame.isUndefined
                }
                false
            }
            suspendedStack != null -> {
                suspendedStack!!.isUndefined
            }
            else -> {
                false
            }
        }

    /**
     * @return The ID of the papyrus element.
     */
    override val iD: EID

    /**
     * @return The type of the script.
     */
    val type: Byte
    private var data: ActiveScriptData? = null

    /**
     * @return The instance field.
     */
    var instance: AnalyzableElement?
        private set
    private var suspendedStack: SuspendedStack?

    inner class ActiveScriptData internal constructor(input: ByteBuffer, context: PapyrusContext) :
        PapyrusDataFor<ActiveScript?> {
        /**
         * @see ess.Element.write
         * @param output The output stream.
         */
        override fun write(output: ByteBuffer?) {
            iD.write(output)
            majorVersion.let { output?.put(it) }
            MINORVERSION.let { output?.put(it) }
            unknownVar.write(output)
            output?.put(FLAG)
            unknownByte.let { output?.put(it) }
            if ((FLAG and 0x01.toByte()) == 0x01.toByte()) {
                unknown2.let { output?.putInt(it) }
            }
            unknown3.let { output?.put(it) }
            if (unknown4 != null) {
                unknown4!!.write(output)
            }
            if (ATTACHED != null) {
                ATTACHED?.write(output)
            }
            output?.putInt(STACKFRAMES.size)
            STACKFRAMES.forEach { frame: StackFrame -> frame.write(output) }
            if (unknown5 != null) {
                output?.put(unknown5!!)
            }
        }

        /**
         * @see ess.Element.calculateSize
         * @return The size of the `Element` in bytes.
         */
        override fun calculateSize(): Int {
            var sum = 2
            sum += iD.calculateSize()
            sum += unknownVar.calculateSize()
            sum += 2
            sum += if ((FLAG and 0x01.toByte()) == 0x01.toByte()) 4 else 0
            sum += 5
            sum += (if (null == ATTACHED) 0 else ATTACHED?.calculateSize())!!
            sum += if (null == unknown4) 0 else unknown4!!.calculateSize()
            sum += if (null != unknown5) 1 else 0
            var result = 0
            for (STACKFRAME in STACKFRAMES) {
                val calculateSize = STACKFRAME.calculateSize()
                result += calculateSize
            }
            sum += result
            return sum
        }

        /**
         * @return The major version field.
         */
        var majorVersion: Byte
        var MINORVERSION: Byte
        /**
         * @return The unknown variable field.
         */
        var unknownVar: Variable
        val FLAG: Byte

        /**
         * @return The Unknown byte field.
         */
        var unknownByte: Byte

        /**
         * @return The Unknown2 field.
         */
        var unknown2: Int

        /**
         * @return The Unknown3 field.
         */
        var unknown3: Byte

        /**
         * @return The FragmentTask field.
         */
        var unknown4: FragmentTask?
        var ATTACHED: EID? = null
        var ATTACHED_ELEMENT: HasID? = null
        var STACKFRAMES: MutableList<StackFrame> = mutableListOf()

        /**
         * @return The Unknown5 field.
         */
        var unknown5: Byte?

        /**
         * Creates a new `ActiveScriptData`.
         *
         * @param input The input stream.
         * @param context The `PapyrusContext` info.
         * @throws PapyrusElementException
         * @throws PapyrusFormatException
         */
        init {
            majorVersion = input.get()
            MINORVERSION = input.get()
            require((MINORVERSION < 1 || MINORVERSION > 2).not()) { "Wrong minor version = $MINORVERSION" }
            unknownVar = read(input, context)
            FLAG = input.get()
            unknownByte = input.get()
            unknown2 = if ((FLAG and 0x01.toByte()) == 0x01.toByte()) input.int else 0
            unknown3 = input.get()
            unknown4 = when (unknown3.toInt()) {
                1, 2, 3 -> FragmentTask(input, unknown3, context)
                else -> null
            }
            ATTACHED = if (context.game!!.isFO4) {
                if (null == unknown4) {
                    context.readEID64(input)
                } else if (null != unknown4!!.TYPE && unknown4!!.TYPE !== FragmentTask.FragmentType.TerminalRunResults) {
                    context.readEID64(input)
                } else {
                    null
                }
            } else {
                null
            }
            if (null != ATTACHED && ATTACHED?.isZero?.not() == true) {
                ATTACHED_ELEMENT = context.findAny(ATTACHED!!)
                if (ATTACHED_ELEMENT == null) {
                    logger.warn{"Attachment ID did not match anything: $ATTACHED\n"}
                }
            } else {
                ATTACHED_ELEMENT = null
            }
            try {
                val count = input.int
                require(count >= 0) { "Invalid stackFrame count: $count" }
                STACKFRAMES = mutableListOf()
                for (i in 0 until count) {
                    val frame = StackFrame(input, this@ActiveScript, context)
                    STACKFRAMES.add(frame)
                }
            } catch (ex: ListException) {
                throw PapyrusElementException("Failed to read with StackFrame data.", ex, this)
            }
            unknown5 = if (FLAG.toInt() != 0) input.get() else null
        }
    }

    companion object {
    }

    /**
     * Creates a new `ActiveScript` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `PapyrusContext` info.
     */
    init {
        iD = context.readEID32(input)
        type = input.get()
        instance = null
        suspendedStack = null
    }
}