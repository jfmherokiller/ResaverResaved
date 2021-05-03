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
package resaver.ess.papyrus

import resaver.Analysis
import resaver.ListException
import resaver.ess.*
import resaver.ess.papyrus.ActiveScript
import java.nio.ByteBuffer
import java.util.*
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.experimental.and

/**
 * Describes an active script in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class ActiveScript(input: ByteBuffer, context: PapyrusContext) : AnalyzableElement, HasID, SeparateData {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer) {
        iD.write(output)
        output.put(type)
    }

    /**
     * @see SeparateData.readData
     * @param input
     * @param context
     * @throws PapyrusElementException
     * @throws PapyrusFormatException
     */
    @Throws(PapyrusElementException::class, PapyrusFormatException::class)
    override fun readData(input: ByteBuffer, context: PapyrusContext) {
        data = ActiveScriptData(input, context)
    }

    /**
     * @see SeparateData.writeData
     * @param output
     */
    override fun writeData(output: ByteBuffer) {
        data!!.write(output)
    }

    /**
     * @see resaver.ess.Element.calculateSize
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
        stackFrames!!.forEach(Consumer { obj: StackFrame -> obj.zero() })
    }

    /**
     * Shortcut for getData().getStackFrames().
     *
     * @return
     */
    val stackFrames: List<StackFrame>?
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
        return !stackFrames!!.isEmpty()
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
            val FIRST = stackFrames!![0]
            return (!FIRST.isNative && FIRST.isZeroed
                    && stackFrames!!.stream().allMatch { f: StackFrame -> f.isZeroed || f.isNative })
        }

    /**
     * @return A flag indicating if the `ActiveScript` is suspended.
     */
    val isSuspended: Boolean
        get() = suspendedStack != null

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        return Linkable.makeLink("thread", iD, this.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUILDER = StringBuilder()
        if (null == data) {
            return iD.toString() + "-" + String.format("%02x", type)
        }
        if (isUndefined) {
            BUILDER.append("#")
        }
        if (hasStack()) {
            val topFrame = stackFrames!![0]
            val scriptName = topFrame.scriptName
            BUILDER.append(scriptName).append(" ")
        } else if (suspendedStack != null && suspendedStack!!.message != null) {
            BUILDER.append(suspendedStack!!.message.toString())
        } else {
        }
        BUILDER.append("(").append(iD).append(") ")
        BUILDER.append(stackFrames!!.size).append(" frames")
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
    override fun getInfo(analysis: Analysis, save: ESS): String {
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
        if (hasStack()) {
            val topFrame = stackFrames!![0]
            val mods = analysis.SCRIPT_ORIGINS[topFrame.scriptName.toIString()]
            if (null != mods) {
                val mod = mods.last()
                BUILDER.append(String.format("<p>Probably running code from mod %s.</p>", mod))
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
                    String.format(
                        "<p>This thread is attached to an instance from %s.</p>",
                        PLUGIN.toHTML(this)
                    )
                )
            } else if (instance.refID.type === RefID.Type.CREATED) {
                BUILDER.append("<p>This thread is attach to instance that was created in-game.</p>")
            }
        }
        if (null == instance) {
            BUILDER.append("<p>No owner was identified.</p>")
        } else if (instance is Linkable) {
            val l = instance as Linkable?
            val type = instance!!.javaClass.simpleName
            BUILDER.append(String.format("<p>%s: %s</p>", type, l!!.toHTML(this)))
        } else {
            val type = instance!!.javaClass.simpleName
            BUILDER.append(String.format("<p>%s: %s</p>", type, instance))
        }
        BUILDER.append("<p>")
        BUILDER.append(String.format("ID: %s<br/>", iD))
        BUILDER.append(String.format("Type: %02x<br/>", type, type))
        if (null == data) {
            BUILDER.append("<h3>DATA MISSING</h3>")
        } else {
            BUILDER.append(String.format("Version: %d.%d<br/>", this.data!!.majorVersion, minorVersion))
            BUILDER.append(String.format("Unknown (var): %s<br/>", this.data!!.unknownVar.toHTML(this)))
            BUILDER.append(String.format("Flag: %08x<br/>", flag))
            BUILDER.append(String.format("Unknown1 (byte): %02x<br/>", this.data!!.unknownByte))
            BUILDER.append(String.format("Unknown2 (int): %08x<br/>", this.data!!.unknown2))
            BUILDER.append(String.format("Unknown3 (byte): %02x<br/>", this.data!!.unknown3))
            if (null != this.data!!.unknown4) {
                BUILDER.append(String.format("FragmentData (struct): %s<br/>", this.data!!.unknown4?.toHTML(this)))
            } else {
                BUILDER.append(String.format("FragmentData (struct): %s<br/>", this.data!!.unknown4))
            }
            if (null != this.data!!.unknown5) {
                BUILDER.append(String.format("Unknown5 (byte): %02x<br/>", this.data!!.unknown5))
            } else {
                BUILDER.append("Unknown5 (byte): <em>absent</em><br/>")
            }
            val UNKNOWN2 = save.papyrus.context.broadSpectrumSearch(this.data!!.unknown2)
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
    override fun matches(analysis: Analysis, mod: String): Boolean {
        Objects.requireNonNull(analysis)
        Objects.requireNonNull(mod)
        return stackFrames!!.stream().anyMatch { v: StackFrame -> v.matches(analysis, mod) }
    }

    /**
     * @param stacks The SuspendedStacks.
     */
    fun resolveStack(stacks: Map<EID?, SuspendedStack?>) {
        if (hasStack()) {
            val ref = stackFrames!![0].owner
            if (ref is Variable.Ref) {
                instance = ref.referent
            } else if (ref is Variable.Null) {
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
        return if (null == data) {
            false
        } else stackFrames!!.stream().anyMatch { frame: StackFrame -> frame.script == script }
    }

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
     * @return A flag indicating if the `ActiveScript` is undefined.
     * An `ActiveScript` is undefined if any of its stack frames are
     * undefined.
     */
    val isUndefined: Boolean
        get() = if (null == data) {
            false
        } else if (hasStack()) {
            stackFrames!!.stream().anyMatch { obj: StackFrame -> obj.isUndefined }
        } else if (suspendedStack != null) {
            suspendedStack!!.isUndefined
        } else {
            false
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
         * @see resaver.ess.Element.write
         * @param output The output stream.
         */
        override fun write(output: ByteBuffer) {
            iD.write(output)
            output.put(majorVersion)
            output.put(MINORVERSION)
            unknownVar.write(output)
            output.put(FLAG)
            output.put(unknownByte)
            if ((FLAG and 0x01).toInt() == 0x01) {
                output.putInt(unknown2)
            }
            output.put(unknown3)
            if (null != unknown4) {
                unknown4!!.write(output)
            }
            ATTACHED?.write(output)
            output.putInt(STACKFRAMES!!.size)
            STACKFRAMES!!.forEach(Consumer { frame: StackFrame -> frame.write(output) })
            if (null != unknown5) {
                output.put(unknown5!!)
            }
        }

        /**
         * @see resaver.ess.Element.calculateSize
         * @return The size of the `Element` in bytes.
         */
        override fun calculateSize(): Int {
            var sum = 2
            sum += iD.calculateSize()
            sum += unknownVar.calculateSize()
            sum += 2
            sum += if ((FLAG and 0x01).toInt() == 0x01) 4 else 0
            sum += 5
            sum += ATTACHED?.calculateSize() ?: 0
            sum += if (null == unknown4) 0 else unknown4!!.calculateSize()
            sum += if (null != unknown5) 1 else 0
            sum += STACKFRAMES!!.stream().mapToInt { obj: StackFrame -> obj.calculateSize() }.sum()
            return sum
        }

        /**
         * @return The major version field.
         */
        var majorVersion: Byte
            get() = data!!.majorVersion
        val MINORVERSION: Byte

        /**
         * @return The unknown variable field.
         */
        var unknownVar: Variable
            get() = data!!.unknownVar
        val FLAG: Byte

        /**
         * @return The Unknown byte field.
         */
        var unknownByte: Byte
            get() = data!!.unknownByte

        /**
         * @return The Unknown2 field.
         */
        var unknown2: Int
            get() = data!!.unknown2

        /**
         * @return The Unknown3 field.
         */
        var unknown3: Byte
            get() = data!!.unknown3

        /**
         * @return The FragmentTask field.
         */
        var unknown4: FragmentTask? = null
            get() = data?.unknown4
        var ATTACHED: EID? = null
        var ATTACHED_ELEMENT: HasID? = null
        var STACKFRAMES: MutableList<StackFrame>? = null

        /**
         * @return The Unknown5 field.
         */
        var unknown5: Byte?
            get() = data!!.unknown5

        /**
         * Creates a new `ActiveScriptData`.
         *
         * @param input The input stream.
         * @param context The `PapyrusContext` info.
         * @throws PapyrusElementException
         * @throws PapyrusFormatException
         */
        init {
            Objects.requireNonNull(input)
            Objects.requireNonNull(context)
            majorVersion = input.get()
            MINORVERSION = input.get()
            require(!(MINORVERSION < 1 || MINORVERSION > 2)) { "Wrong minor version = $MINORVERSION" }
            unknownVar = Variable.read(input, context)
            FLAG = input.get()
            unknownByte = input.get()
            unknown2 = if ((FLAG and 0x01).toInt() == 0x01) input.int else 0
            unknown3 = input.get()
            when (unknown3.toInt()) {
                1, 2, 3 -> unknown4 = FragmentTask(input, unknown3, context)
                else -> unknown4 = null
            }
            if (context.game.isFO4) {
                if (null == unknown4) {
                    ATTACHED = context.readEID64(input)
                } else if (null != unknown4!!.TYPE && unknown4!!.TYPE !== FragmentTask.FragmentType.TerminalRunResults) {
                    ATTACHED = context.readEID64(input)
                } else {
                    ATTACHED = null
                }
            } else {
                ATTACHED = null
            }
            if (null != ATTACHED && !ATTACHED!!.isZero) {
                ATTACHED_ELEMENT = context.findAny(ATTACHED)
                if (ATTACHED_ELEMENT == null) {
                    LOG.log(Level.WARNING, String.format("Attachment ID did not match anything: %s\n", ATTACHED))
                }
            } else {
                ATTACHED_ELEMENT = null
            }
            try {
                val count = input.int
                require(count >= 0) { "Invalid stackFrame count: $count" }
                STACKFRAMES = mutableListOf()
                for (i in 0 until count) {
                    try {
                        val frame = StackFrame(input, this@ActiveScript, context)
                        STACKFRAMES!!.add(frame)
                    } catch (ex: PapyrusFormatException) {
                        throw ListException(i, count, ex)
                    }
                }
            } catch (ex: ListException) {
                throw PapyrusElementException("Failed to read with StackFrame data.", ex, this)
            }
            unknown5 = if (FLAG.toInt() != 0) input.get() else null
        }
    }

    companion object {
        private val LOG = Logger.getLogger(ActiveScript::class.java.canonicalName)
    }

    /**
     * Creates a new `ActiveScript` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `PapyrusContext` info.
     */
    init {
        Objects.requireNonNull(input)
        Objects.requireNonNull(context)
        iD = context.readEID32(input)
        type = input.get()
        instance = null
        suspendedStack = null
    }
}