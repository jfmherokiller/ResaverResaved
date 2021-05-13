/*
 * Copyright 2016 Mark.
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
package resaver.gui

import ess.*
import ess.ESS.ESSContext
import ess.papyrus.*
import mf.Duad
import mu.KLoggable
import mu.KLogger
import resaver.Analysis
import resaver.Mod
import java.nio.BufferUnderflowException
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/**
 *
 * @author Mark
 */
class FilterMaker {
    companion object:KLoggable {
        /**
         * Setup a Mod analysis setFilter.
         *
         * @param mod
         * @param plugins
         * @param analysis
         * @return
         */
        fun createModFilter(mod: Mod, plugins: PluginInfo, analysis: Analysis?): Predicate<FilterTreeModel.Node> {
            logger.info{"Filtering: mod = \"$mod\""}
            val MODNAME = mod.getName()
            val PLUGINS: MutableSet<Plugin?> = HashSet()
            mod.getESPNames().forEach { espName: String? ->
                for (p in plugins.fullPlugins) {
                    if (p.NAME.equals(espName, ignoreCase = true)) {
                        PLUGINS.add(p)
                        break
                    }
                }
            }
            val modFilter = Predicate { node: FilterTreeModel.Node ->
                (node.hasElement()
                        && node.element is AnalyzableElement
                        && (node.element as AnalyzableElement?)!!.matches(analysis, MODNAME))
            }
            val pluginFilter = createPluginFilter(PLUGINS)
            return modFilter.or(pluginFilter)
        }

        /**
         * Setup a plugin setFilter.
         *
         * @param plugins
         * @return
         */
        fun createPluginFilter(plugins: Set<Plugin?>): Predicate<FilterTreeModel.Node> {
            logger.info{"Filtering: plugins = \"$plugins\""}
            return Predicate { node: FilterTreeModel.Node ->
                // If the node doesn't contain an element, it automatically fails.
                if (!node.hasElement()) {
                    return@Predicate false
                } // Check if the element is the plugin itself.
                else if (node.element is Plugin) {
                    return@Predicate plugins.contains(node.element as Plugin?)
                } // Check if the element is an instance with a matching refid.
                else if (node.element is ScriptInstance) {
                    val instance = node.element as ScriptInstance?
                    val refID = instance!!.refID
                    return@Predicate refID.PLUGIN != null && plugins.contains(refID.PLUGIN)
                } // Check if the element is a ChangeForm with a matching refid.
                else if (node.element is ChangeForm) {
                    val form = node.element as ChangeForm?
                    val refID = form!!.refID
                    return@Predicate refID?.PLUGIN != null && plugins.contains(refID.PLUGIN)
                } // If the element is not an instance, it automatically fails.
                false
            }
        }

        /**
         * Setup a regex setFilter.
         *
         * @param regex
         * @return
         */
        fun createRegexFilter(regex: String): Predicate<FilterTreeModel.Node> {
            logger.info{"Filtering: regex = \"$regex\""}
            if (regex.isNotEmpty()) {
                try {
                    logger.info{"Filtering: regex = \"$regex\""}
                    val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
                    return Predicate { node: FilterTreeModel.Node -> pattern.matcher(node.name).find() }
                } catch (ex: PatternSyntaxException) {
                }
            }
            return Predicate { node: FilterTreeModel.Node? -> true }
        }

        /**
         * Setup an undefined element setFilter.
         *
         * @return
         */
        fun createUndefinedFilter(): Predicate<FilterTreeModel.Node> {
            return Predicate { node: FilterTreeModel.Node ->
                if (node.hasElement()) {
                    when (val e = node.element) {
                        is Script -> {
                            return@Predicate e.isUndefined
                        }
                        is ScriptInstance -> {
                            return@Predicate e.isUndefined
                        }
                        is Reference -> {
                            return@Predicate e.isUndefined
                        }
                        is Struct -> {
                            return@Predicate e.isUndefined
                        }
                        is StructInstance -> {
                            return@Predicate e.isUndefined
                        }
                        is ActiveScript -> {
                            return@Predicate e.isUndefined
                        }
                        is FunctionMessage -> {
                            return@Predicate e.isUndefined
                        }
                        is StackFrame -> {
                            return@Predicate e.isUndefined
                        }
                        is SuspendedStack -> {
                            return@Predicate e.isUndefined
                        }
                    }
                }
                false
            }
        }

        /**
         * Setup an unattached element setFilter.
         *
         * @return
         */
        fun createUnattachedFilter(): Predicate<FilterTreeModel.Node> {
            return Predicate { node: FilterTreeModel.Node ->
                if (node.hasElement() && node.element is ScriptInstance) {
                    return@Predicate (node.element as ScriptInstance?)!!.isUnattached
                }
                false
            }
        }

        /**
         * Setup an unattached element setFilter.
         *
         * @return
         */
        fun createMemberlessFilter(): Predicate<FilterTreeModel.Node> {
            return Predicate { node: FilterTreeModel.Node ->
                if (node.hasElement() && node.element is ScriptInstance) {
                    return@Predicate (node.element as ScriptInstance?)!!.hasMemberlessError()
                }
                false
            }
        }

        /**
         * Setup an unattached element setFilter.
         *
         * @return
         */
        fun createCanaryFilter(): Predicate<FilterTreeModel.Node> {
            return Predicate { node: FilterTreeModel.Node ->
                if (node.hasElement() && node.element is ScriptInstance) {
                    val instance = node.element as ScriptInstance?
                    return@Predicate instance!!.hasCanary() && instance.canary == 0
                }
                false
            }
        }

        /**
         * Setup a nullref setFilter.
         *
         * @return
         */
        fun createNullRefFilter(): Predicate<FilterTreeModel.Node> {
            return Predicate { node: FilterTreeModel.Node ->
                if (!node.hasElement() || node.element !is ChangeForm) {
                    return@Predicate false
                }
                false
            }
        }

        /**
         * Setup a non-existent element setFilter.
         *
         * @param ess The save file.
         * @return
         */
        fun createNonExistentFilter(ess: ESS): Predicate<FilterTreeModel.Node> {
            return Predicate { node: FilterTreeModel.Node ->
                if (node.hasElement() && node.element is ScriptInstance) {
                    val instance = node.element as ScriptInstance?
                    val refID = instance!!.refID
                    return@Predicate refID.type === RefID.Type.CREATED && !ess.changeForms.containsKey(refID)
                }
                false
            }
        }

        /**
         * Setup a non-existent element setFilter.
         *
         * @return
         */
        fun createLongStringFilter(): Predicate<FilterTreeModel.Node> {
            return Predicate { node: FilterTreeModel.Node ->
                if (node.hasElement() && node.element is TString) {
                    val str = node.element as TString?
                    return@Predicate str!!.length() >= 512
                }
                false
            }
        }

        /**
         * Setup a deleted element setFilter.
         *
         * @param context
         * @param analysis
         * @return
         */
        fun createDeletedFilter(context: ESSContext?, analysis: Analysis?): Predicate<FilterTreeModel.Node> {
            return Predicate { node: FilterTreeModel.Node ->
                if (!node.hasElement()) {
                    return@Predicate false
                }
                if (node.element !is ChangeForm) {
                    return@Predicate false
                }
                val FORM = node.element as ChangeForm?
                if (!(FORM!!.type === ChangeFormType.ACHR || FORM!!.type === ChangeFormType.REFR)) {
                    return@Predicate false
                }
                if (!FORM!!.changeFlags.getFlag(1) && !FORM.changeFlags.getFlag(3)) {
                    return@Predicate false
                }
                val DATA = FORM.getData(analysis, context, true) ?: return@Predicate false
                if (DATA !is GeneralElement) {
                    return@Predicate false
                }
                val ROOT = DATA as GeneralElement
                val MOVECELL = ROOT.getElement("MOVE_CELL") ?: return@Predicate false
                check(MOVECELL is RefID) { "MOVE_CELL was not a RefID: $MOVECELL" }
                MOVECELL.FORMID == -0x1
            }
        }

        /**
         * Setup a deleted element setFilter.
         *
         * @param context
         * @param analysis
         * @return
         */
        fun createVoidFilter(context: ESSContext?, analysis: Analysis?): Predicate<FilterTreeModel.Node> {
            return Predicate { node: FilterTreeModel.Node ->
                if (!node.hasElement()) {
                    return@Predicate false
                }
                if (node.element !is ChangeForm) {
                    return@Predicate false
                }
                val FORM = node.element as ChangeForm?
                if (!(FORM!!.type === ChangeFormType.ACHR || FORM!!.type === ChangeFormType.REFR)) {
                    return@Predicate false
                }
                val FLAGS: Flags = FORM!!.changeFlags
                var i = 0
                while (i <= 7) {
                    if (FLAGS.getFlag(i)) {
                        return@Predicate false
                    }
                    i++
                }
                val DATA = FORM.getData(analysis, context, true) ?: return@Predicate false
                if (DATA !is GeneralElement) {
                    return@Predicate false
                }
                val ROOT = DATA as GeneralElement
                if (ROOT.values.isEmpty()) {
                    return@Predicate true
                }
                if (ROOT.hasVal("INITIAL") && ROOT.count() <= 2) {
                    val initial = ROOT.getGeneralElement("INITIAL")
                    if (initial!!.values.isEmpty()) {
                        if (ROOT.count() == 1) {
                            return@Predicate true
                        }
                        if (ROOT.hasVal("EXTRADATA")) {
                            val EXTRA = ROOT.getGeneralElement("EXTRADATA")
                            val count = EXTRA!!.getVal("DATA_COUNT") as VSVal?
                            return@Predicate count!!.value == 0
                        }
                    }
                }
                false
            }
        }

        /**
         * Setup a ChangeFlag setFilter.
         *
         * @param mask
         * @param filter
         * @return
         */
        fun createChangeFlagFilter(mask: Int, filter: Int): Predicate<FilterTreeModel.Node> {
            return if (mask == 0) {
                Predicate { node: FilterTreeModel.Node? -> true }
            } else {
                Predicate { node: FilterTreeModel.Node ->
                    if (!node.hasElement()) {
                        return@Predicate false
                    }
                    if (node.element !is ChangeForm) {
                        return@Predicate false
                    }
                    val FORM = node.element as ChangeForm?
                    val FLAGS = FORM!!.changeFlags
                    val flags = FLAGS.FLAGS
                    val filtered = filter.inv() xor flags
                    val masked = filtered or mask.inv()
                    masked == -1
                }
            }
        }

        /**
         * Setup a ChangeFormFlag setFilter.
         *
         * @param context
         * @param mask
         * @param filter
         * @return
         */
        fun createChangeFormFlagFilter(context: ESSContext?, mask: Int, filter: Int): Predicate<FilterTreeModel.Node> {
            return if (mask == 0) {
                Predicate { node: FilterTreeModel.Node? -> true }
            } else {
                Predicate { node: FilterTreeModel.Node ->
                    if (!node.hasElement()) {
                        return@Predicate false
                    }
                    if (node.element !is ChangeForm) {
                        return@Predicate false
                    }
                    val FORM = node.element as ChangeForm?
                    val FLAGS = FORM!!.changeFlags
                    if (!FLAGS.getFlag(ChangeFlagConstantsRef.CHANGE_FORM_FLAGS)) {
                        return@Predicate false
                    }
                    try {
                        val data = FORM.getData(null, context, true)
                        if (data !is GeneralElement) {
                            return@Predicate false
                        }
                        val DATA = data as GeneralElement
                        if (!DATA.hasVal(ChangeFlagConstantsRef.CHANGE_FORM_FLAGS)) {
                            return@Predicate false
                        }
                        val CFF = DATA.getElement(ChangeFlagConstantsRef.CHANGE_FORM_FLAGS) as ChangeFormFlags?
                        val flags = CFF!!.flags
                        val filtered = filter.inv() xor flags
                        val masked = filtered or mask.inv()
                        return@Predicate masked == -1
                    } catch (ex: BufferUnderflowException) {
                        return@Predicate false
                    }
                }
            }
        }

        /**
         * Setup a ChangeFormFlag setFilter.
         *
         * @param context
         * @param analysis
         * @param mask
         * @param filter
         * @return
         */
        fun createChangeFormflagFilter(
            context: ESSContext?,
            analysis: Analysis?,
            mask: Int,
            filter: Int
        ): Predicate<FilterTreeModel.Node> {
            return if (mask == 0) {
                Predicate { node: FilterTreeModel.Node? -> true }
            } else {
                Predicate { node: FilterTreeModel.Node ->
                    if (!node.hasElement()) {
                        return@Predicate false
                    }
                    if (node.element !is ChangeForm) {
                        return@Predicate false
                    }
                    val FORM = node.element as ChangeForm?
                    if (listOf(
                            ChangeFormType.ACHR, ChangeFormType.CELL,
                            ChangeFormType.NPC_, ChangeFormType.REFR
                        ).contains(FORM!!.type)
                    ) {
                        return@Predicate false
                    }
                    if (!FORM.changeFlags.getFlag(0)) {
                        return@Predicate false
                    }
                    val DATA = FORM.getData(analysis, context, true)
                    if (DATA == null) {
                        return@Predicate false
                    } else if (DATA is GeneralElement) {
                        val GEN = DATA as GeneralElement
                        val KEY = "ChangeFormFlags"
                        if (!GEN.hasVal(KEY)) {
                            return@Predicate false
                        } else if (GEN.getElement(KEY) !is ChangeFormFlags) {
                            return@Predicate false
                        } else {
                            val CFF = GEN.getElement(KEY) as ChangeFormFlags?
                            val flags = CFF!!.flags
                            val filtered = filter.inv() xor flags
                            val masked = filtered or mask.inv()
                            return@Predicate masked == -1
                        }
                    } else if (DATA is ChangeFormNPC) {
                        val flags = DATA.changeFormFlags!!.flags
                        val filtered = filter.inv() xor flags
                        val masked = filtered or mask.inv()
                        return@Predicate masked == -1
                    } else {
                        return@Predicate false
                    }
                }
            }
        }

        /**
         * Create an empty filter.
         *
         */
        fun createFilter(): Predicate<FilterTreeModel.Node> {
            return Predicate { x: FilterTreeModel.Node? -> true }
        }

        /**
         * Create a filter.
         *
         * @param mod
         * @param savefile
         * @param plugin
         * @param regex
         * @param analysis
         * @param undefined
         * @param unattached
         * @param memberless
         * @param canaries
         * @param nullrefs
         * @param nonexistent
         * @param longStrings
         * @param deleted
         * @param empty
         * @param changeFlags
         * @param changeFormFlags
         * @return
         */
        @JvmStatic
        fun createFilter(
            savefile: ESS,
            mod: Mod?,
            plugin: Plugin?,
            regex: String, analysis: Analysis?,
            undefined: Boolean, unattached: Boolean, memberless: Boolean,
            canaries: Boolean, nullrefs: Boolean, nonexistent: Boolean,
            longStrings: Boolean, deleted: Boolean, empty: Boolean,
            changeFlags: Duad<Int>?, changeFormFlags: Duad<Int>?
        ): Predicate<FilterTreeModel.Node>? {
            logger.info{"Updating filter."}
            val FILTERS = ArrayList<Predicate<FilterTreeModel.Node>>(4)
            val SUBFILTERS = ArrayList<Predicate<FilterTreeModel.Node>>(4)
            val context = savefile.context

            // Setup a Mod analysis setFilter.
            if (null != mod && null != analysis) {
                FILTERS.add(createModFilter(mod, savefile.pluginInfo, analysis))
            }

            // Setup a plugin setFilter.
            if (null != plugin) {
                FILTERS.add(createPluginFilter(setOf(plugin)))
            }

            // Setup a regex setFilter.
            if (regex.isNotEmpty()) {
                FILTERS.add(createRegexFilter(regex))
            }

            // Setup a changeflag setFilter.
            if (null != changeFlags) {
                FILTERS.add(createChangeFlagFilter(changeFlags.A, changeFlags.B))
            }

            // Setup a changeformflag setFilter.
            if (null != changeFormFlags) {
                FILTERS.add(createChangeFormFlagFilter(context, changeFormFlags.A, changeFormFlags.B))
            }
            // Filter undefined.
            if (undefined) {
                SUBFILTERS.add(createUndefinedFilter())
            }

            // Filter unattached.
            if (unattached) {
                SUBFILTERS.add(createUnattachedFilter())
            }

            // Filter memberless.
            if (memberless) {
                SUBFILTERS.add(createMemberlessFilter())
            }

            // Filter canaries.
            if (canaries) {
                SUBFILTERS.add(createCanaryFilter())
            }

            // Filter formlists containing nullrefs.
            if (nullrefs) {
                SUBFILTERS.add(createNullRefFilter())
            }

            // Filter instances attached to nonexistent created forms.
            if (nonexistent) {
                SUBFILTERS.add(createNonExistentFilter(savefile))
            }

            // Filter long strings.
            if (longStrings) {
                SUBFILTERS.add(createLongStringFilter())
            }

            // Filter deleted changeforms.
            if (deleted) {
                SUBFILTERS.add(createDeletedFilter(context, analysis))
            }

            // Filter empty changeforms.
            if (empty) {
                SUBFILTERS.add(createVoidFilter(context, analysis))
            }

            // Combine the filters.
            // OR the subfilters together.
            var seen = false
            var acc: Predicate<FilterTreeModel.Node>? = null
            for (SUBFILTER in SUBFILTERS) {
                if (!seen) {
                    seen = true
                    acc = SUBFILTER
                } else {
                    acc = acc!!.or(SUBFILTER)
                }
            }
            (if (seen) Optional.of(acc!!) else Optional.empty()).ifPresent { e: Predicate<FilterTreeModel.Node> ->
                FILTERS.add(
                    e
                )
            }

            // AND the main filters together.
            var seen1 = false
            var result: Predicate<FilterTreeModel.Node>? = null
            for (FILTER in FILTERS) {
                if (!seen1) {
                    seen1 = true
                    result = FILTER
                } else {
                    result = result!!.and(FILTER)
                }
            }
            return if (seen1) result else null
        }

        /**
         *
         */
        override val logger: KLogger
            get() = logger()
    }
}