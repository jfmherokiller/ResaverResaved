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
package resaver.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import resaver.Mod;
import resaver.Analysis;
import resaver.ess.*;
import resaver.ess.papyrus.*;
import resaver.gui.FilterTreeModel.Node;
import static resaver.ess.ChangeFlagConstantsRef.CHANGE_FORM_FLAGS;

/**
 *
 * @author Mark
 */
public class FilterMaker {

    /**
     * Setup a Mod analysis setFilter.
     *
     * @param mod
     * @param plugins
     * @param analysis
     * @return
     */
    static public Predicate<Node> createModFilter(Mod mod, PluginInfo plugins, Analysis analysis) {
        Objects.requireNonNull(mod);
        Objects.requireNonNull(analysis);
        LOG.info(String.format("Filtering: mod = \"%s\"", mod));

        final String MODNAME = mod.getName();

        final Set<Plugin> PLUGINS = new HashSet<>();
        mod.getESPNames().stream().forEach(espName -> plugins.getFullPlugins()
                .stream()
                .filter(p -> p.NAME.equalsIgnoreCase(espName))
                .findAny()
                .ifPresent(PLUGINS::add));

        Predicate<Node> modFilter = node -> node.hasElement()
                && node.getElement() instanceof AnalyzableElement
                && ((AnalyzableElement) node.getElement()).matches(analysis, MODNAME);

        Predicate<Node> pluginFilter = createPluginFilter(PLUGINS);
        return modFilter.or(pluginFilter);
    }

    /**
     * Setup a plugin setFilter.
     *
     * @param plugins
     * @return
     */
    static public Predicate<Node> createPluginFilter(Set<Plugin> plugins) {
        Objects.requireNonNull(plugins);
        LOG.info(String.format("Filtering: plugins = \"%s\"", plugins));

        return node -> {
            // If the node doesn't contain an element, it automatically fails.
            if (!node.hasElement()) {
                return false;

            } // Check if the element is the plugin itself.
            else if (node.getElement() instanceof Plugin) {
                return plugins.contains((Plugin) node.getElement());

            } // Check if the element is an instance with a matching refid.
            else if (node.getElement() instanceof ScriptInstance) {
                ScriptInstance instance = (ScriptInstance) node.getElement();
                RefID refID = instance.getRefID();
                return null != refID && refID.PLUGIN != null && plugins.contains(refID.PLUGIN);

            } // Check if the element is a ChangeForm with a matching refid.
            else if (node.getElement() instanceof ChangeForm) {
                ChangeForm form = (ChangeForm) node.getElement();
                RefID refID = form.getRefID();
                return null != refID && refID.PLUGIN != null && plugins.contains(refID.PLUGIN);

            } // If the element is not an instance, it automatically fails.
            return false;
        };
    }

    /**
     * Setup a regex setFilter.
     *
     * @param regex
     * @return
     */
    static public Predicate<Node> createRegexFilter(String regex) {
        Objects.requireNonNull(regex);
        LOG.info(String.format("Filtering: regex = \"%s\"", regex));

        if (!regex.isEmpty()) {
            try {
                LOG.info(String.format("Filtering: regex = \"%s\"", regex));
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                return node -> pattern.matcher(node.getName()).find();
            } catch (PatternSyntaxException ex) {
            }
        }

        return node -> true;
    }

    /**
     * Setup an undefined element setFilter.
     *
     * @return
     */
    static public Predicate<Node> createUndefinedFilter() {
        return node -> {
            if (node.hasElement()) {
                Element e = node.getElement();
                if (e instanceof Script) {
                    return ((Script) e).isUndefined();
                } else if (e instanceof ScriptInstance) {
                    return ((ScriptInstance) e).isUndefined();
                } else if (e instanceof Reference) {
                    return ((Reference) e).isUndefined();
                } else if (e instanceof Struct) {
                    return ((Struct) e).isUndefined();
                } else if (e instanceof StructInstance) {
                    return ((StructInstance) e).isUndefined();
                } else if (e instanceof ActiveScript) {
                    return ((ActiveScript) e).isUndefined();
                } else if (e instanceof FunctionMessage) {
                    return ((FunctionMessage) e).isUndefined();
                } else if (e instanceof StackFrame) {
                    return ((StackFrame) e).isUndefined();
                } else if (e instanceof SuspendedStack) {
                    return ((SuspendedStack) e).isUndefined();
                }
            }
            return false;
        };
    }

    /**
     * Setup an unattached element setFilter.
     *
     * @return
     */
    static public Predicate<Node> createUnattachedFilter() {
        return node -> {
            if (node.hasElement() && node.getElement() instanceof ScriptInstance) {
                return ((ScriptInstance) node.getElement()).isUnattached();
            }
            return false;
        };
    }

    /**
     * Setup an unattached element setFilter.
     *
     * @return
     */
    static public Predicate<Node> createMemberlessFilter() {
        return node -> {
            if (node.hasElement() && node.getElement() instanceof ScriptInstance) {
                return ((ScriptInstance) node.getElement()).hasMemberlessError();
            }
            return false;
        };
    }

    /**
     * Setup an unattached element setFilter.
     *
     * @return
     */
    static public Predicate<Node> createCanaryFilter() {
        return node -> {
            if (node.hasElement() && node.getElement() instanceof ScriptInstance) {
                ScriptInstance instance = (ScriptInstance) node.getElement();
                return instance.hasCanary() && instance.getCanary() == 0;
            }
            return false;
        };
    }

    /**
     * Setup a nullref setFilter.
     *
     * @return
     */
    static public Predicate<Node> createNullRefFilter() {
        return node -> {
            if (!node.hasElement() || !(node.getElement() instanceof ChangeForm)) {
                return false;
            }
            return false;
            /*
            final ChangeForm FORM = (ChangeForm) node.getElement();
            final ChangeFormData DATA = FORM.getData();
            if (!(DATA instanceof ChangeFormFLST)) {
                return false;
            }

            return ((ChangeFormFLST) DATA).containsNullrefs();*/
        };
    }

    /**
     * Setup a non-existent element setFilter.
     *
     * @param ess The save file.
     * @return
     */
    static public Predicate<Node> createNonExistentFilter(ESS ess) {
        return node -> {
            if (node.hasElement() && node.getElement() instanceof ScriptInstance) {
                ScriptInstance instance = (ScriptInstance) node.getElement();
                RefID refID = instance.getRefID();
                return refID.getType() == RefID.Type.CREATED && !ess.getChangeForms().containsKey(refID);
            }
            return false;
        };
    }

    /**
     * Setup a non-existent element setFilter.
     *
     * @return
     */
    static public Predicate<Node> createLongStringFilter() {
        return node -> {
            if (node.hasElement() && node.getElement() instanceof TString) {
                TString str = (TString) node.getElement();
                return str.length() >= 512;
            }
            return false;
        };
    }

    /**
     * Setup a deleted element setFilter.
     *
     * @param context
     * @param analysis
     * @return
     */
    static public Predicate<Node> createDeletedFilter(ESS.ESSContext context, Analysis analysis) {
        return node -> {
            if (!node.hasElement()) {
                return false;
            }
            if (!(node.getElement() instanceof ChangeForm)) {
                return false;
            }

            final ChangeForm FORM = (ChangeForm) node.getElement();

            if (!(FORM.getType() == ChangeForm.Type.ACHR || FORM.getType() == ChangeForm.Type.REFR)) {
                return false;
            }

            if (!FORM.getChangeFlags().getFlag(1) && !FORM.getChangeFlags().getFlag(3)) {
                return false;
            }

            final ChangeFormData DATA = FORM.getData(analysis, context, true);

            if (DATA == null) {
                return false;
            }
            if (!(DATA instanceof GeneralElement)) {
                return false;
            }

            final GeneralElement ROOT = (GeneralElement) DATA;
            final Element MOVECELL = ROOT.getElement("MOVE_CELL");

            if (MOVECELL == null) {
                return false;
            }

            if (!(MOVECELL instanceof RefID)) {
                throw new IllegalStateException("MOVE_CELL was not a RefID: " + MOVECELL);
            }

            final RefID REF = (RefID) MOVECELL;
            return REF.FORMID == 0xFFFFFFFF;

        };
    }

    /**
     * Setup a deleted element setFilter.
     *
     * @param context
     * @param analysis
     * @return
     */
    static public Predicate<Node> createVoidFilter(ESS.ESSContext context, Analysis analysis) {
        return node -> {
            if (!node.hasElement()) {
                return false;
            }
            if (!(node.getElement() instanceof ChangeForm)) {
                return false;
            }

            final ChangeForm FORM = (ChangeForm) node.getElement();

            if (!(FORM.getType() == ChangeForm.Type.ACHR || FORM.getType() == ChangeForm.Type.REFR)) {
                return false;
            }

            final Flags FLAGS = FORM.getChangeFlags();
            for (int i = 0; i <= 7; i++) {
                if (FLAGS.getFlag(i)) {
                    return false;
                }
            }

            final ChangeFormData DATA = FORM.getData(analysis, context, true);

            if (DATA == null) {
                return false;
            }
            if (!(DATA instanceof GeneralElement)) {
                return false;
            }

            final GeneralElement ROOT = (GeneralElement) DATA;

            if (ROOT.getValues().isEmpty()) {
                return true;
            }

            if (ROOT.hasVal("INITIAL") && ROOT.count() <= 2) {
                GeneralElement initial = ROOT.getGeneralElement("INITIAL");
                if (initial.getValues().isEmpty()) {
                    if (ROOT.count() == 1) {
                        return true;
                    }

                    if (ROOT.hasVal("EXTRADATA")) {
                        final GeneralElement EXTRA = ROOT.getGeneralElement("EXTRADATA");
                        VSVal count = (VSVal) EXTRA.getVal("DATA_COUNT");
                        return count.getValue() == 0;
                    }
                }
            }

            return false;
        };
    }

    /**
     * Setup a ChangeFlag setFilter.
     *
     * @param mask
     * @param filter
     * @return
     */
    static public Predicate<Node> createChangeFlagFilter(int mask, int filter) {
        if (mask == 0) {
            return node -> true;
        } else {
            return node -> {
                if (!node.hasElement()) {
                    return false;
                }
                if (!(node.getElement() instanceof ChangeForm)) {
                    return false;
                }

                final ChangeForm FORM = (ChangeForm) node.getElement();

                final Flags.Int FLAGS = FORM.getChangeFlags();
                int flags = FLAGS.FLAGS;
                int filtered = (~filter) ^ flags;
                int masked = filtered | (~mask);
                return masked == -1;
            };
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
    static public Predicate<Node> createChangeFormFlagFilter(ESS.ESSContext context, int mask, int filter) {
        if (mask == 0) {
            return node -> true;
        } else {
            return node -> {
                if (!node.hasElement()) {
                    return false;
                }
                if (!(node.getElement() instanceof ChangeForm)) {
                    return false;
                }

                final ChangeForm FORM = (ChangeForm) node.getElement();

                final Flags.Int FLAGS = FORM.getChangeFlags();
                if (!FLAGS.getFlag(CHANGE_FORM_FLAGS)) {
                    return false;
                }

                try {
                    ChangeFormData data = FORM.getData(null, context, true);
                    if (!(data instanceof GeneralElement)) {
                        return false;
                    }
                    final GeneralElement DATA = (GeneralElement) data;

                    if (!DATA.hasVal(CHANGE_FORM_FLAGS)) {
                        return false;
                    }

                    final ChangeFormFlags CFF = (ChangeFormFlags) DATA.getElement(CHANGE_FORM_FLAGS);
                    int flags = CFF.getFlags();
                    int filtered = (~filter) ^ flags;
                    int masked = filtered | (~mask);
                    return masked == -1;
                    
                } catch (java.nio.BufferUnderflowException ex) {
                    return false;
                }
            };
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
    static public Predicate<Node> createChangeFormflagFilter(ESS.ESSContext context, Analysis analysis, int mask, int filter) {
        if (mask == 0) {
            return node -> true;

        } else {
            return node -> {
                if (!node.hasElement()) {
                    return false;
                }
                if (!(node.getElement() instanceof ChangeForm)) {
                    return false;
                }

                final ChangeForm FORM = (ChangeForm) node.getElement();

                if (Arrays.asList(ChangeForm.Type.ACHR, ChangeForm.Type.CELL,
                        ChangeForm.Type.NPC_, ChangeForm.Type.REFR).contains(FORM.getType())) {
                    return false;
                }

                if (!FORM.getChangeFlags().getFlag(0)) {
                    return false;
                }

                final ChangeFormData DATA = FORM.getData(analysis, context, true);

                if (DATA == null) {
                    return false;

                } else if (DATA instanceof GeneralElement) {
                    final GeneralElement GEN = (GeneralElement) DATA;
                    final String KEY = "ChangeFormFlags";

                    if (!GEN.hasVal(KEY)) {
                        return false;
                    } else if (!(GEN.getElement(KEY) instanceof ChangeFormFlags)) {
                        return false;
                    } else {
                        final ChangeFormFlags CFF = (ChangeFormFlags) GEN.getElement(KEY);
                        int flags = CFF.getFlags();
                        int filtered = (~filter) ^ flags;
                        int masked = filtered | (~mask);
                        return masked == -1;
                    }

                } else if (DATA instanceof ChangeFormNPC) {
                    final ChangeFormNPC NPC = (ChangeFormNPC) DATA;
                    int flags = NPC.getChangeFormFlags().getFlags();
                    int filtered = (~filter) ^ flags;
                    int masked = filtered | (~mask);
                    return masked == -1;

                } else {
                    return false;
                }
            };
        }
    }

    /**
     * Create an empty filter.
     *
     */
    static public Predicate<Node> createFilter() {
        return x -> true;
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
    static public Predicate<Node> createFilter(ESS savefile,
            Mod mod,
            Plugin plugin,
            String regex, Analysis analysis,
            boolean undefined, boolean unattached, boolean memberless,
            boolean canaries, boolean nullrefs, boolean nonexistent,
            boolean longStrings, boolean deleted, boolean empty,
            mf.Duad<Integer> changeFlags, mf.Duad<Integer> changeFormFlags) {
        Objects.requireNonNull(savefile);

        LOG.info("Updating filter.");
        ArrayList<Predicate<Node>> FILTERS = new ArrayList<>(4);
        ArrayList<Predicate<Node>> SUBFILTERS = new ArrayList<>(4);
        ESS.ESSContext context = savefile.getContext();

        // Setup a Mod analysis setFilter.
        if (null != mod && null != analysis) {
            FILTERS.add(createModFilter(mod, savefile.getPluginInfo(), analysis));
        }

        // Setup a plugin setFilter.
        if (null != plugin) {
            FILTERS.add(createPluginFilter(Collections.singleton(plugin)));
        }

        // Setup a regex setFilter.
        if (!regex.isEmpty()) {
            FILTERS.add(createRegexFilter(regex));
        }

        // Setup a changeflag setFilter.
        if (null != changeFlags) {
            FILTERS.add(createChangeFlagFilter(changeFlags.A, changeFlags.B));
        }

        // Setup a changeformflag setFilter.
        if (null != changeFormFlags) {
            FILTERS.add(createChangeFormFlagFilter(context, changeFormFlags.A, changeFormFlags.B));
        }
        // Filter undefined.
        if (undefined) {
            SUBFILTERS.add(createUndefinedFilter());
        }

        // Filter unattached.
        if (unattached) {
            SUBFILTERS.add(createUnattachedFilter());
        }

        // Filter memberless.
        if (memberless) {
            SUBFILTERS.add(createMemberlessFilter());
        }

        // Filter canaries.
        if (canaries) {
            SUBFILTERS.add(createCanaryFilter());
        }

        // Filter formlists containing nullrefs.
        if (nullrefs) {
            SUBFILTERS.add(createNullRefFilter());
        }

        // Filter instances attached to nonexistent created forms.
        if (nonexistent) {
            SUBFILTERS.add(createNonExistentFilter(savefile));
        }

        // Filter long strings.
        if (longStrings) {
            SUBFILTERS.add(createLongStringFilter());
        }

        // Filter deleted changeforms.
        if (deleted) {
            SUBFILTERS.add(createDeletedFilter(context, analysis));
        }

        // Filter empty changeforms.
        if (empty) {
            SUBFILTERS.add(createVoidFilter(context, analysis));
        }

        // Combine the filters.
        // OR the subfilters together.
        SUBFILTERS.stream()
                .reduce(Predicate::or)
                .ifPresent(FILTERS::add);

        // AND the main filters together.
        return FILTERS.stream()
                .reduce(Predicate::and)
                .orElse(null);
    }

    /**
     *
     */
    static final private Logger LOG = Logger.getLogger(FilterMaker.class.getCanonicalName());

}
