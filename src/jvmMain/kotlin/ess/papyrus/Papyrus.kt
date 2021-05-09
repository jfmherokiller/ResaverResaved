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

import ess.ESS.ESSContext
import ess.Element
import ess.GlobalDataBlock
import ess.Linkable
import ess.ModelBuilder
import ess.papyrus.ActiveScript
import ess.papyrus.Papyrus
import ess.papyrus.ScriptInstance
import mf.Counter
import resaver.ListException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.function.Consumer
import java.util.logging.Logger

/**
 * Describes a the data for a `GlobalData` when it is the Papyrus
 * script section.
 *
 * @author Mark Fairchild
 */
class Papyrus(input: ByteBuffer, essContext: ESSContext, model: ModelBuilder) : PapyrusElement, GlobalDataBlock {
    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        check(!truncated) { "Papyrus is truncated. Cannot write." }
        val startingPosition = output?.position()
        output?.putShort(header)

        // Write the string table.
        STRINGS?.write(output)
        if (null != STRUCTS) {
            output?.putInt(SCRIPTS!!.size)
            STRUCTS?.size?.let { output?.putInt(it) }
            SCRIPTS?.write(output)
            STRUCTS?.write(output)
        } else {
            output?.putInt(SCRIPTS!!.size)
            SCRIPTS?.write(output)
        }
        SCRIPT_INSTANCES!!.write(output)
        REFERENCES!!.write(output)
        if (STRUCT_INSTANCES != null) {
            STRUCT_INSTANCES?.write(output)
        }
        ARRAYS!!.write(output)
        PAPYRUS_RUNTIME!!.write(output)
        ACTIVESCRIPTS!!.write(output)
        SCRIPT_INSTANCES?.values?.forEach(Consumer { data: ScriptInstance -> data.data!!.write(output) })
        REFERENCES?.values?.forEach(Consumer { ref: Reference -> ref.data!!.write(output) })
        STRUCT_INSTANCES?.values?.forEach(Consumer { struct: StructInstance -> struct.writeData(output) })
        ARRAYS?.values?.forEach(Consumer { info: ArrayInfo -> info.writeData(output) })
        ACTIVESCRIPTS?.values?.forEach(Consumer { script: ActiveScript -> script.writeData(output) })

        // Write the function message table and suspended stacks.
        output?.putInt(FUNCTIONMESSAGES.size)
        FUNCTIONMESSAGES.forEach(Consumer { message: FunctionMessage -> message.write(output) })
        SUSPENDEDSTACKS1!!.write(output)
        SUSPENDEDSTACKS2!!.write(output)

        // Write the "unknown" fields.
        output?.putInt(UNK1)
        UNK2.ifPresent { value: Int? -> output?.putInt(value!!) }
        output?.putInt(UNKS.size)
        UNKS.forEach(Consumer { id: EID? -> id!!.write(output) })

        // Write the unbind map.
        UNBINDMAP!!.write(output)

        // Write the save file version field, if present.
        SAVE_FILE_VERSION.ifPresent { value: Short? ->
            output?.putShort(
                value!!
            )
        }

        // Write the remaining data.
        output?.put(ARRAYSBLOCK)
        if (startingPosition != null) {
            check(output.position() == startingPosition + calculateSize()) {
                String.format(
                    "Actual = %d, calculated = %d",
                    output.position(),
                    calculateSize()
                )
            }
        }
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 2 // HEADER
        sum += stringTable.calculateSize()
        sum += scripts.calculateSize()
        sum += if (STRUCTS == null) 0 else structs.calculateSize()
        sum += scriptInstances.calculateSize()
        sum += if (STRUCT_INSTANCES == null) 0 else structInstances.calculateSize()
        sum += references.calculateSize()
        sum += arrays.calculateSize()
        sum += PAPYRUS_RUNTIME?.calculateSize() ?: 0
        sum += activeScripts.calculateSize()
        var result = 0
        for (functionMessage in functionMessages) {
            val calculateSize = functionMessage!!.calculateSize()
            result += calculateSize
        }
        sum += 4 + result
        sum += suspendedStacks1.calculateSize()
        sum += suspendedStacks2.calculateSize()
        sum += 4 // UNK1
        sum += if (UNK2 != null && UNK2.isPresent) 4 else 0
        var sum1 = 0
        for (eid in unknownIDList) {
            val calculateSize = eid!!.calculateSize()
            sum1 += calculateSize
        }
        sum += 4 + sum1
        sum += unbinds.calculateSize()
        sum += if (SAVE_FILE_VERSION != null && SAVE_FILE_VERSION.isPresent) 2 else 0
        sum += if (ARRAYSBLOCK == null) 0 else ARRAYSBLOCK.size
        return sum
    }

    /**
     * @return Accessor for the string table.
     */
    val stringTable: StringTable
        get() = STRINGS ?: StringTable()

    /**
     * @return Accessor for the list of scripts.
     */
    val scripts: ScriptMap
        get() = SCRIPTS ?: ScriptMap()

    /**
     * @return Accessor for the list of structdefs.
     */
    val structs: StructMap
        get() = STRUCTS ?: StructMap()

    /**
     * @return Accessor for the list of script instances.
     */
    val scriptInstances: ScriptInstanceMap
        get() = SCRIPT_INSTANCES ?: ScriptInstanceMap()

    /**
     * @return Accessor for the list of references.
     */
    val references: ReferenceMap
        get() = REFERENCES ?: ReferenceMap()

    /**
     * @return Accessor for the list of structs.
     */
    val structInstances: StructInstanceMap
        get() = STRUCT_INSTANCES ?: StructInstanceMap()

    /**
     * @return Accessor for the list of arrays.
     */
    val arrays: ArrayMap
        get() = ARRAYS ?: ArrayMap()

    /**
     * @return Accessor for the list of active scripts.
     */
    val activeScripts: ActiveScriptMap
        get() = ACTIVESCRIPTS ?: ActiveScriptMap()

    /**
     * @return Accessor for the list of function messages.
     */
    val functionMessages: List<FunctionMessage?>
        get() = FUNCTIONMESSAGES ?: emptyList()

    /**
     * @return Accessor for the combined list of suspended stacks.
     */
    val suspendedStacks: SuspendedStackMap
        get() {
            val s1 = suspendedStacks1
            val s2 = suspendedStacks2
            val combined = SuspendedStackMap()
            combined.putAll(s1)
            combined.putAll(s2)
            return combined
        }

    /**
     * @return Accessor for the first list of suspended stacks.
     */
    val suspendedStacks1: SuspendedStackMap
        get() = SUSPENDEDSTACKS1 ?: SuspendedStackMap()

    /**
     * @return Accessor for the second list of suspended stacks.
     */
    val suspendedStacks2: SuspendedStackMap
        get() = SUSPENDEDSTACKS2 ?: SuspendedStackMap()

    /**
     * @return Accessor for the queued unbinds list.
     */
    val unbinds: UnbindMap
        get() = UNBINDMAP ?: UnbindMap()

    /**
     * @return
     */
    val unknownIDList: List<EID?>
        get() = UNKS ?: emptyList()

    /**
     * @return Accessor for the Arrays block.
     */
    val arraysBlock: ByteBuffer
        get() {
            val BUFFER = ByteBuffer.wrap(ARRAYSBLOCK)
            BUFFER.order(ByteOrder.LITTLE_ENDIAN)
            return BUFFER
        }

    /**
     * Find a `GameElement` by its `EID`.
     * `GameElement` includes `ScriptInstance`,
     * `StructInstance`, and `Reference`.
     *
     * @param id
     * @return
     */
    fun findReferrent(id: EID): GameElement? {
        return if (scriptInstances.containsKey(id)) {
            scriptInstances[id]
        } else if (references.containsKey(id)) {
            references[id]
        } else if (structInstances.containsKey(id)) {
            structInstances[id]
        } else if (id.isZero) {
            null
        } else {
            null
        }
    }

    /**
     * Counts the number of unattached instances, the
     * `ScriptInstance` objects whose refID is 0.
     *
     * @return The number of unattached instances.
     */
    fun countUnattachedInstances(): Int {
        var count = 0L
        for (scriptInstance in scriptInstances.values) {
            if (scriptInstance.isUnattached) {
                count++
            }
        }
        return count.toInt()
    }

    /**
     * Counts the number of `ScriptInstance`, `Reference`,
     * and `ActiveScript` objects whose script is null.
     *
     * @return The number of undefined elements / undefined threads.
     */
    fun countUndefinedElements(): IntArray {
        var count = 0
        var threads = 0
        var result = 0L
        for (script in scripts.values) {
            if (script.isUndefined) {
                result++
            }
        }
        count += result.toInt()
        var count3 = 0L
        for (scriptInstance in scriptInstances.values) {
            if (scriptInstance.isUndefined) {
                count3++
            }
        }
        count += count3.toInt()
        var count1 = 0L
        for (reference in references.values) {
            if (reference.isUndefined) {
                count1++
            }
        }
        count += count1.toInt()
        var result2 = 0L
        for (struct in structs.values) {
            if (struct.isUndefined) {
                result2++
            }
        }
        count += result2.toInt()
        var result1 = 0L
        for (structInstance in structInstances.values) {
            if (structInstance.isUndefined) {
                result1++
            }
        }
        count += result1.toInt()
        var count2 = 0L
        for (v in activeScripts.values) {
            if (v.isUndefined && !v.isTerminated) {
                count2++
            }
        }
        threads += count2.toInt()
        return intArrayOf(count, threads)
    }

    /**
     * Removes all `ScriptInstance` objects whose refID is 0.
     *
     * @return The elements that were removed.
     */
    fun removeUnattachedInstances(): Set<PapyrusElement?> {
        val UNATTACHED: MutableSet<ScriptInstance> = HashSet()
        for (scriptInstance in scriptInstances.values) {
            if (scriptInstance.isUnattached) {
                UNATTACHED.add(scriptInstance)
            }
        }
        return removeElements(UNATTACHED)
    }

    /**
     * Removes all `ScriptInstance` objects whose script is null.
     * Also checks `ActiveScript`, `FunctionMessage`, and
     * `SuspendedStack`.
     *
     * @return The elements that were removed.
     */
    fun removeUndefinedElements(): Set<PapyrusElement?> {
        val set: MutableSet<Script> = HashSet()
        for (script in scripts.values) {
            if (script.isUndefined) {
                set.add(script)
            }
        }
        val REMOVED: MutableSet<PapyrusElement?> = HashSet(removeElements(set))
        val result: MutableSet<Struct> = HashSet()
        for (struct in structs.values) {
            if (struct.isUndefined) {
                result.add(struct)
            }
        }
        REMOVED.addAll(removeElements(result))
        val set1: MutableSet<ScriptInstance> = HashSet()
        for (scriptInstance in scriptInstances.values) {
            if (scriptInstance.isUndefined) {
                set1.add(scriptInstance)
            }
        }
        REMOVED.addAll(removeElements(set1))
        val result1: MutableSet<Reference> = HashSet()
        for (reference in references.values) {
            if (reference.isUndefined) {
                result1.add(reference)
            }
        }
        REMOVED.addAll(removeElements(result1))
        val set2: MutableSet<StructInstance> = HashSet()
        for (structInstance in structInstances.values) {
            if (structInstance.isUndefined) {
                set2.add(structInstance)
            }
        }
        REMOVED.addAll(removeElements(set2))
        for (v in activeScripts.values) {
            if (v.isUndefined && !v.isTerminated) {
                v.zero()
                REMOVED.add(v)
            }
        }
        return REMOVED
    }

    /**
     * Removes all `ScriptInstance` objects whose script is null.
     * Also checks `ActiveScript`, `FunctionMessage`, and
     * `SuspendedStack`.
     *
     * @return The elements that were removed.
     */
    fun terminateUndefinedThreads(): Set<ActiveScript> {
        val TERMINATED: MutableSet<ActiveScript> = HashSet()
        for (v in activeScripts.values) {
            if (v.isUndefined && !v.isTerminated) {
                TERMINATED.add(v)
            }
        }
        TERMINATED.forEach(Consumer { obj: ActiveScript -> obj.zero() })
        return TERMINATED
    }

    /**
     * Removes a `PapyrusElement` collection.
     *
     * @param elements The elements to remove.
     * @return The elements that were removed.
     */
    fun removeElements(elements: Collection<PapyrusElement>?): Set<PapyrusElement> {
        if (elements == null) {
            throw NullPointerException("The set of elements to remove must not be null and must not contain null.")
        }
        val ELEMENTS = LinkedList(elements)
        val REMOVED: MutableSet<PapyrusElement> = mutableSetOf()
        while (!ELEMENTS.isEmpty()) {
            val ELEMENT = ELEMENTS.pop()
            if (ELEMENT is Script) {
                val DEF = ELEMENT as Definition
                val set: MutableSet<ScriptInstance> = hashSetOf()
                for (v in scriptInstances.values) {
                    if (v.definition === DEF) {
                        set.add(v)
                    }
                }
                ELEMENTS.addAll(set)
                scripts.remove(DEF.name)?.let { REMOVED.add(it) }
            } else if (ELEMENT is Struct) {
                val DEF = ELEMENT
                val set: MutableSet<StructInstance> = hashSetOf()
                for (v in structInstances.values) {
                    if (v.definition === DEF) {
                        set.add(v)
                    }
                }
                ELEMENTS.addAll(set)
                structs.remove(DEF.name)?.let { REMOVED.add(it) }
            } else if (ELEMENT is ScriptInstance) {
                val INSTANCE = ELEMENT
                if (scriptInstances.containsKey(INSTANCE.iD)) {
                    scriptInstances.remove(INSTANCE.iD)?.let { REMOVED.add(it) }
                }
            } else if (ELEMENT is StructInstance) {
                val STRUCT = ELEMENT
                if (structInstances.containsKey(STRUCT.iD)) {
                    structInstances.remove(STRUCT.iD)?.let { REMOVED.add(it) }
                }
            } else if (ELEMENT is Reference) {
                val REF = ELEMENT
                if (references.containsKey(REF.iD)) {
                    references.remove(REF.iD)?.let { REMOVED.add(it) }
                }
            } else if (ELEMENT is ArrayInfo) {
                val ARRAY = ELEMENT
                if (arrays.containsKey(ARRAY.iD)) {
                    arrays.remove(ARRAY.iD)?.let { REMOVED.add(it) }
                }
            } else if (ELEMENT is ActiveScript) {
                val ACTIVE = ELEMENT
                if (activeScripts.containsKey(ACTIVE.iD)) {
                    activeScripts.remove(ACTIVE.iD)?.let { REMOVED.add(it) }
                }
            } else if (ELEMENT is SuspendedStack) {
                val STACK = ELEMENT
                if (suspendedStacks1.containsKey(STACK.iD)) {
                    suspendedStacks1.remove(STACK.iD)?.let { REMOVED.add(it) }
                } else if (suspendedStacks2.containsKey(STACK.iD)) {
                    suspendedStacks2.remove(STACK.iD)?.let { REMOVED.add(it) }
                }
            } else {
                LOG.warning("Papyrus.removeElements: can't delete element: $ELEMENT")
            }
        }
        return REMOVED
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return "Papyrus-${super.toString()}"
    }

    /**
     * Searches for all `Linkable` elements that refer to the
     * specified ID.
     *
     * @param id
     * @return
     */
    private fun findMatches(id: EID): ReferrentMap {
        val REFERRENTS = ReferrentMap()
        val set: MutableSet<Linkable> = HashSet()
        for (scriptInstance in scriptInstances.values) {
            if (scriptInstance.data != null) {
                for (m in scriptInstance.variables) {
                    if (m.hasRef(id)) {
                        set.add(scriptInstance)
                        break
                    }
                }
            }
        }
        REFERRENTS[ScriptInstance::class.java] = set
        val result: MutableSet<Linkable> = HashSet()
        for (reference in references.values) {
            for (m in reference.variables) {
                if (m.hasRef(id)) {
                    result.add(reference)
                    break
                }
            }
        }
        REFERRENTS[Reference::class.java] = result
        val set1: MutableSet<Linkable> = HashSet()
        for (arrayInfo in arrays.values) {
            for (m in arrayInfo.variables) {
                if (m!!.hasRef(id)) {
                    set1.add(arrayInfo)
                    break
                }
            }
        }
        REFERRENTS[ArrayInfo::class.java] = set1
        val result1: MutableSet<Linkable> = HashSet()
        for (activeScript in activeScripts.values) {
            if (activeScript.attached === id) {
                result1.add(activeScript)
            }
        }
        REFERRENTS[ActiveScript::class.java] = result1
        val set2: MutableSet<Linkable> = HashSet()
        for (activeScript in activeScripts.values) {
            for (stackFrame in activeScript.stackFrames) {
                if (stackFrame.owner.hasRef(id)) {
                    set2.add(stackFrame)
                }
            }
        }
        REFERRENTS[ActiveScript::class.java] = set2
        val result2: MutableSet<Linkable> = HashSet()
        for (t in activeScripts.values) {
            for (s in t.stackFrames) {
                for (m in s.variables) {
                    if (m.hasRef(id)) {
                        result2.add(s)
                        break
                    }
                }
            }
        }
        REFERRENTS[StackFrame::class.java] = result2
        val set3: MutableSet<Linkable> = HashSet()
        for (v in structInstances.values) {
            for (m in v.variables) {
                if (m.hasRef(id)) {
                    set3.add(v)
                    break
                }
            }
        }
        REFERRENTS[StructInstance::class.java] = set3
        return REFERRENTS
    }

    /**
     * Searches for all `Linkable` elements that refer to the
     * specified ID and prints them to a `StringBuilder`.
     *
     * @param ref
     * @param builder
     * @param myName
     */
    fun printReferrents(ref: GameElement, builder: StringBuilder, myName: String) {
        val REFERENTS = findMatches(ref.iD)
        referrentsPrint(ref, builder, REFERENTS[ActiveScript::class.java], myName, "threads", "attached to")
        referrentsPrint(
            ref,
            builder,
            REFERENTS[StackFrame::class.java],
            myName,
            "stackframes",
            "with member data referring to"
        )
        referrentsPrint(
            ref,
            builder,
            REFERENTS[ScriptInstance::class.java],
            myName,
            "instances",
            "with member data referring to"
        )
        referrentsPrint(
            ref,
            builder,
            REFERENTS[Reference::class.java],
            myName,
            "references",
            "with member data referring to"
        )
        referrentsPrint(
            ref,
            builder,
            REFERENTS[ArrayInfo::class.java],
            myName,
            "arrays",
            "with member data referring to"
        )
        referrentsPrint(
            ref,
            builder,
            REFERENTS[StructInstance::class.java],
            myName,
            "structs",
            "with member data referring to"
        )
    }

    /**
     * @return A flag indicating if the papyrus block has a truncation error.
     */
    val isTruncated: Boolean
        get() = truncated || stringTable.isTruncated


    private var truncated = false

    /**
     * @return Returns a new `PapyrusContext`.
     */
    val context: PapyrusContext

    /**
     * @return The header field.
     */
    var header: Short = 0
    private var PAPYRUS_RUNTIME: EID? = null
    private var SAVE_FILE_VERSION: Optional<Short> = Optional.empty()
    private var UNK1 = 0
    private var UNK2: Optional<Int> = Optional.empty()
    private var STRINGS: StringTable? = null
    private var SCRIPTS: ScriptMap? = null
    private var STRUCTS: StructMap? = null
    private var SCRIPT_INSTANCES: ScriptInstanceMap? = null
    private var REFERENCES: ReferenceMap? = null
    private var STRUCT_INSTANCES: StructInstanceMap? = null
    private var ARRAYS: ArrayMap? = null
    private var ACTIVESCRIPTS: ActiveScriptMap? = null
    private var FUNCTIONMESSAGES: MutableList<FunctionMessage> = mutableListOf()
    private var SUSPENDEDSTACKS1: SuspendedStackMap? = null
    private var SUSPENDEDSTACKS2: SuspendedStackMap? = null
    private var UNBINDMAP: UnbindMap? = null
    private var UNKS: MutableList<EID?> = mutableListOf()

    /**
     * @return Accessor the "other" data.
     */
    var otherData: OtherData? = null
    private val ARRAYSBLOCK: ByteArray
    val EIDS: Map<Number, EID>

    companion object {
        /**
         * Helper for printReferrents.
         *
         * @param builder
         * @param ls
         * @param myname
         * @param lname
         * @param relationship
         */
        private fun referrentsPrint(
            ref: Element,
            builder: StringBuilder,
            ls: Set<Linkable?>?,
            myname: String,
            lname: String,
            relationship: String
        ) {
            if (null == ls) {
                return
            }
            if (ls.isEmpty()) {
                builder.append(String.format("<p>There are zero %s %s this %s.</p>", lname, relationship, myname))
            } else if (ls.size == 1) {
                builder.append(String.format("<p>There is one %s %s this %s.</p>", lname, relationship, myname))
            } else {
                builder.append(
                    String.format(
                        "<p>There are %s %s %s this %s.</p>",
                        ls.size,
                        lname,
                        relationship,
                        myname
                    )
                )
            }
            if (ls.size in 1..49) {
                builder.append("<ul>")
                ls.forEach(Consumer { i: Linkable? -> builder.append("<li>").append(i!!.toHTML(ref)).append("</a>") })
                builder.append("</ul>")
            }
        }

        private val LOG = Logger.getLogger(Papyrus::class.java.canonicalName)
    }

    /**
     * Creates a new `Papyrus` by reading from a byte buffer.
     *
     * @param input The data.
     * @param essContext The `ESSContext` info.
     * @param model A `ModelBuilder`.
     *
     * @throws PapyrusException Thrown if the Papyrus block was partially
     * readRefID.
     */
    init {
        val SUM = Counter(input.capacity())
        SUM.addCountListener { sum: Int ->
            check((truncated || sum != input.position()).not()) {
                String.format(
                    "Position mismatch; counted %d but actual %d.",
                    sum,
                    input.position()
                )
            }
        }
        context = PapyrusContext(essContext, this)
        EIDS = HashMap(100000)
        try {
            if (input.limit() - input.position() < 7) {
                val msg =
                    "The Papyrus block is missing. This can happen if Skyrim is running too many mods or too many scripts.\nUnfortunately, there is literally nothing I can do to help you with this."
                throw PapyrusException(msg, null, this)
            }

            // Read the header.            
            header = input.short
            SUM.click(2)

            // Read the string table.
            var stringTable: StringTable? = null
            try {
                stringTable = StringTable(input, essContext)
            } finally {
                STRINGS = stringTable
                model.addStringTable(stringTable!!)
            }
            SUM.click(STRINGS!!.calculateSize())
            val scriptCount = input.int
            val structCount = if (essContext.game!!.isFO4) input.int else 0
            SUM.click(if (essContext.game!!.isFO4) 8 else 4)

            // Read the scripts.
            var scriptMap: ScriptMap? = null
            try {
                scriptMap = ScriptMap(scriptCount, input, context)
            } finally {
                SCRIPTS = scriptMap
                SCRIPTS!!.values.forEach(Consumer { script: Script ->
                    script.resolveParent(
                        SCRIPTS!!
                    )
                })
                model.addScripts(SCRIPTS!!)
                SUM.click(SCRIPTS!!.calculateSize() - 4)
            }

            // Read the structs.
            if (!essContext.game!!.isFO4) {
                STRUCTS = null
            } else {
                var structMap: StructMap? = null
                try {
                    structMap = StructMap(structCount, input, context)
                } finally {
                    STRUCTS = structMap
                    model.addStructs(STRUCTS!!)
                    SUM.click(STRUCTS?.calculateSize()?.minus(4) ?: 0)
                }
            }

            // Read the script instance table.
            var scriptInstanceMap: ScriptInstanceMap? = null
            try {
                scriptInstanceMap = ScriptInstanceMap(input, SCRIPTS!!, context)
            } finally {
                SCRIPT_INSTANCES = scriptInstanceMap
                SUM.click(SCRIPT_INSTANCES!!.calculateSize())
            }

            // Read the reference table.
            var referenceMap: ReferenceMap? = null
            try {
                referenceMap = ReferenceMap(input, SCRIPTS, context)
            } finally {
                REFERENCES = referenceMap
                SUM.click(REFERENCES!!.calculateSize())
            }

            // Read the struct body table.
            if (!essContext.game!!.isFO4) {
                STRUCT_INSTANCES = null
            } else {
                var structInstanceMap: StructInstanceMap? = null
                try {
                    structInstanceMap =StructInstanceMap(input, STRUCTS!!, context)
                } finally {
                    STRUCT_INSTANCES = structInstanceMap
                    SUM.click(STRUCT_INSTANCES!!.calculateSize())
                }
            }

            // Read the array table.
            var arrayMap: ArrayMap? = null
            try {
                arrayMap = ArrayMap(input, context)
            } finally {
                ARRAYS = arrayMap
                SUM.click(ARRAYS!!.calculateSize())
            }
            PAPYRUS_RUNTIME = context.readEID32(input)
            SUM.click(4)

            // Read the active script table.
            var activeScriptMap: ActiveScriptMap? = null
            try {
                activeScriptMap = ActiveScriptMap(input, context)
            } finally {
                ACTIVESCRIPTS = activeScriptMap
                SUM.click(ACTIVESCRIPTS!!.calculateSize())
            }

            // Read the Script data table.
            try {
                SCRIPT_INSTANCES?.calculateSize()?.let { SUM.unclick(it) }
                val count = SCRIPT_INSTANCES?.size
                for (i in 0 until count!!) {
                    try {
                        val eid = context.readEID(input)
                        val element = scriptInstances[eid]
                        element!!.readData(input, context)
                    } catch (ex: NullPointerException) {
                        throw ListException(i, count, ex)
                    } catch (ex: PapyrusFormatException) {
                        throw ListException(i, count, ex)
                    } catch (ex: PapyrusElementException) {
                        throw ListException(i, count, ex)
                    }
                }
                SCRIPT_INSTANCES?.calculateSize()?.let { SUM.click(it) }
            } catch (ex: ListException) {
                throw PapyrusException("Error reading ScriptInstance data.", ex, this)
            } finally {
                model.addScriptInstances(SCRIPT_INSTANCES!!)
            }

            // Read the reference data table.
            try {
                REFERENCES?.calculateSize()?.let { SUM.unclick(it) }
                val count = REFERENCES?.size
                for (i in 0 until count!!) {
                    try {
                        val eid = context.readEID(input)
                        val element = references[eid]
                        element!!.readData(input, context)
                    } catch (ex: NullPointerException) {
                        throw ListException(i, count, ex)
                    } catch (ex: PapyrusFormatException) {
                        throw ListException(i, count, ex)
                    } catch (ex: PapyrusElementException) {
                        throw ListException(i, count, ex)
                    }
                }
                REFERENCES?.calculateSize()?.let { SUM.click(it) }
            } catch (ex: ListException) {
                throw PapyrusException("Error reading Reference data.", ex, this)
            } finally {
                model.addReferences(referenceMap!!)
            }

            // Read the struct data table.
            if (STRUCT_INSTANCES != null) {
                try {
                    STRUCT_INSTANCES?.calculateSize()?.let { SUM.unclick(it) }
                    val count = structInstances.size
                    for (i in 0 until count) {
                        try {
                            val eid = context.readEID(input)
                            val element = structInstances[eid]
                            element!!.readData(input, context)
                        } catch (ex: NullPointerException) {
                            throw ListException(i, count, ex)
                        } catch (ex: PapyrusFormatException) {
                            throw ListException(i, count, ex)
                        } catch (ex: PapyrusElementException) {
                            throw ListException(i, count, ex)
                        }
                    }
                    STRUCT_INSTANCES?.calculateSize()?.let { SUM.click(it) }
                } catch (ex: ListException) {
                    throw PapyrusException("Error reading StructInstance data.", ex, this)
                } finally {
                    model.addStructInstances(STRUCT_INSTANCES!!)
                }
            }

            // Read the array data table.
            try {
                ARRAYS?.calculateSize()?.let { SUM.unclick(it) }
                val count = ARRAYS?.size
                for (i in 0 until count!!) {
                    try {
                        val eid = context.readEID(input)
                        val element = arrays[eid]
                        element!!.readData(input, context)
                    } catch (ex: NullPointerException) {
                        throw ListException(i, count, ex)
                    } catch (ex: PapyrusFormatException) {
                        throw ListException(i, count, ex)
                    } catch (ex: PapyrusElementException) {
                        throw ListException(i, count, ex)
                    }
                }
                ARRAYS?.calculateSize()?.let { SUM.click(it) }
            } catch (ex: ListException) {
                throw PapyrusException("Error reading Array data.", ex, this)
            } finally {
                model.addArrays(arrayMap!!)
            }

            // Read the ActiveScript data table.
            try {
                ACTIVESCRIPTS?.calculateSize()?.let { SUM.unclick(it) }
                val count = ACTIVESCRIPTS?.size
                for (i in 0 until count!!) {
                    try {
                        val eid = context.readEID32(input)
                        val element = activeScripts[eid]
                        element!!.readData(input, context)
                    } catch (ex: NullPointerException) {
                        throw ListException(i, count, ex)
                    } catch (ex: PapyrusFormatException) {
                        throw ListException(i, count, ex)
                    } catch (ex: PapyrusElementException) {
                        throw ListException(i, count, ex)
                    }
                }
                ACTIVESCRIPTS?.calculateSize()?.let { SUM.click(it) }
            } catch (ex: ListException) {
                throw PapyrusException("Error reading ActiveScript data.", ex, this)
            }

            // Read the function message table.
            val functionMessageCount = input.int
            FUNCTIONMESSAGES = ArrayList(functionMessageCount)
            try {
                for (i in 0 until functionMessageCount) {
                    val message = FunctionMessage(input, context)
                    FUNCTIONMESSAGES.add(message)
                }
                var sum = 0
                for (FUNCTIONMESSAGE in FUNCTIONMESSAGES) {
                    val calculateSize = FUNCTIONMESSAGE.calculateSize()
                    sum += calculateSize
                }
                SUM.click(4 + sum)
            } catch (ex: ListException) {
                throw PapyrusException("Failed to read FunctionMessage table.", ex, this)
            }

            // Read the first SuspendedStack table.
            var suspendStacks1: SuspendedStackMap? = null
             try {
                 suspendStacks1 = SuspendedStackMap(input, context)
            } finally {
                SUSPENDEDSTACKS1 = suspendStacks1
                SUM.click(SUSPENDEDSTACKS1!!.calculateSize())
            }

            // Read the second SuspendedStack table.
            var suspendStacks2: SuspendedStackMap? = null
            try {
                suspendStacks2 = SuspendedStackMap(input, context)
            } finally {
                SUSPENDEDSTACKS2 = suspendStacks2
                SUM.click(SUSPENDEDSTACKS2!!.calculateSize())
            }
            model.addThreads(activeScripts)
            model.addFunctionMessages(functionMessages)
            model.addSuspendedStacks1(suspendedStacks1)
            model.addSuspendedStacks2(suspendedStacks2)

            // Read the "unknown" fields.
            UNK1 = input.int
            SUM.click(4)
            UNK2 = if (UNK1 == 0) Optional.empty() else Optional.of(input.int)
            SUM.click(if (UNK2.isPresent) 4 else 0)
            val unknownCount = input.int
            UNKS = ArrayList(unknownCount)
            for (i in 0 until unknownCount) {
                UNKS.add(context.readEID(input))
            }
            model.addUnknownIDList(UNKS)
            var sum = 0
            for (UNK in UNKS) {
                val calculateSize = UNK!!.calculateSize()
                sum += calculateSize
            }
            SUM.click(4 + sum)
            var unbinds: UnbindMap? = null
            try {
                unbinds = UnbindMap(input, context)
            } finally {
                UNBINDMAP = unbinds
                model.addUnbinds(unbinds!!)
                SUM.click(UNBINDMAP!!.calculateSize())
            }

            // For Skyrim, readRefID the save file version field.
            SAVE_FILE_VERSION = if (essContext.game!!.isSkyrim) Optional.of(input.short) else Optional.empty()
            val stacks: Map<EID?, SuspendedStack?> = suspendedStacks
            activeScripts.values.forEach(Consumer { script: ActiveScript -> script.resolveStack(stacks) })

            // Stuff the remaining data into a buffer.
            val remaining = input.limit() - input.position()
            ARRAYSBLOCK = ByteArray(remaining)
            input[ARRAYSBLOCK]
            check(input.position() == calculateSize()) {
                String.format(
                    "pos = %d, calculated = %d",
                    input.position(),
                    calculateSize()
                )
            }

            // Read the "other" stuff.
            var other: OtherData? = null
            try {
                val ARRAYSBUFFER = ByteBuffer.wrap(ARRAYSBLOCK)
                ARRAYSBUFFER.order(ByteOrder.LITTLE_ENDIAN)
                other = OtherData(input, context)
            } catch (ex: BufferUnderflowException) {
            } finally {
                otherData = other
                model.addOtherData(other)
            }
        } catch (ex: PapyrusException) {
            truncated = true
            val MSG = "Error while reading the Papyrus section of ${essContext.path.fileName}: ${ex.message}."
            throw PapyrusException(MSG, ex, this)
        }
    }
}