/*
 * Copyright 2020 Mark.
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
package ess.papyrus;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ess.ESS;
import ess.Header;
import static resaver.ResaverFormatting.makeHTMLList;

import ess.RefID;
import ess.WStringElement;

/**
 *
 * @author Mark
 */
final public class Worrier {

    public Worrier() {
        this.message = null;
        this.shouldWorry = false;
        this.disableSaving = false;
        this.previousESS = null;
        this.previousCanaries = null;
        this.previousNamespaces = null;
    }

    private CharSequence checkFatal(ESS.Result result) {
        final StringBuilder BUF = new StringBuilder();
        final ESS ESS = result.ESS;
        final Papyrus PAPYRUS = ESS.getPapyrus();

        // Check the first fatal condition -- truncation.
        if (ESS.isTruncated()) {
            BUF.append("<p><em>THIS FILE IS TRUNCATED.</em> It is corrupted and can never be recovered, not even by scrubbing it with baking soda and vinegar.");

            if (PAPYRUS.getStringTable().isTruncated()) {
                int missing = PAPYRUS.getStringTable().getMissingCount();
                BUF.append("<br/><strong>TRUNCATED STRING TABLE.</strong> ")
                        .append(missing)
                        .append(" strings missing. The cause of this is unknown, but sometimes involves the scripts that append to strings in a loop.");
            } else if (PAPYRUS.isTruncated()) {
                BUF.append("<br/><strong>TRUNCATED PAPYRUS BLOCK.</strong> This is usually caused by too many scripts running at once, or recursive scripts without proper boundary conditions.");
            }
            boolean b = false;
            for (int i : ESS.getFormIDs()) {
                if (i == 0) {
                    b = true;
                    break;
                }
            }
            if (b) {
                int present = 0;
                while (present < ESS.getFormIDs().length && ESS.getFormIDs()[present] != 0) {
                    present++;
                }
                BUF.append("<br/><strong>TRUNCATED FORMID ARRAY</strong>. ")
                        .append(present).append('/').append(ESS.getFormIDs().length)
                        .append(" formIDs read. This is sometimes caused by updating mods without following their proper updating procedure.");
            }
            BUF.append("</p>");

            this.shouldWorry = true;
            this.disableSaving = true;
        }

        // Check the second fatal condition -- the string table bug.
        if (PAPYRUS.getStringTable().hasSTB()) {
            BUF.append("<p><em>THIS FILE HAS THE STRING-TABLE-BUG.</em> It is corrupted and can never be recovered, not even with lasers or cheetah blood.</p>");
            this.shouldWorry = true;
            this.disableSaving = true;
        }

        return BUF;
    }

    private CharSequence checkPerformance(ESS.Result result) {
        final StringBuilder BUF = new StringBuilder();

        double time = result.TIME_S;
        double size = result.SIZE_MB;

        BUF.append("<p>The savefile was successfully loaded.<ul>");
        BUF.append(String.format("<li>Read %1.1f mb in %1.1f seconds.</li>", size, time));
        if (result.ESS.hasCosave()) {
            BUF.append(String.format("<li>%s co-save was loaded.</li>", result.GAME.COSAVE_EXT.toUpperCase()));
        } else {
            BUF.append("<li>No co-save was found.</li>");
        }

        BUF.append("</ul></p>");
        return BUF;
    }

    private CharSequence checkNonFatal(ESS.Result result) {
        final StringBuilder BUF = new StringBuilder();

        int unattached = result.ESS.getPapyrus().countUnattachedInstances();
        if (unattached > 0) {
            String msg = String.format("<p>There are %d unattached instances.</p>", unattached);
            BUF.append(msg);
            this.shouldWorry = true;
        }

        int[] undefined = result.ESS.getPapyrus().countUndefinedElements();

        if (undefined[0] > 0) {
            String msg = String.format("<p>There are %d undefined elements.</p>", undefined[0]);
            BUF.append(msg);
            this.shouldWorry = true;
        }

        if (undefined[1] > 0) {
            String msg = String.format("<p>There are %d undefined threads.</p>", undefined[1]);
            BUF.append(msg);
            this.shouldWorry = true;
        }

        int numStacks = result.ESS.getPapyrus().getActiveScripts().size();

        Stream<Script> active = result.ESS.getPapyrus().getActiveScripts().values().parallelStream()
                .filter(ActiveScript::hasStack)
                .flatMap(as -> as.getStackFrames().stream())
                .map(StackFrame::getScript);

        Stream<Script> suspended = result.ESS.getPapyrus().getSuspendedStacks().values().parallelStream()
                .map(SuspendedStack::getScript)
                .filter(Objects::nonNull);

        Map<Script, Long> frameCounts = Stream.concat(active, suspended)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(f -> f, Collectors.counting()));

        List<Script> frames = new ArrayList<>(frameCounts.keySet());
        frames.sort((a, b) -> frameCounts.get(b).compareTo(frameCounts.get(a)));

        if (frames.size() > 0) {
            Script most = frames.get(0);

            long numFrames = 0L;
            for (Long v : frameCounts.values()) {
                long l = v;
                numFrames += l;
            }

            if (numStacks > 200 || numFrames > 1000) {
                BUF.append(String.format("<p>There are %d stacks and %d frames, which probably indicates a problem.<br/>", numStacks, numFrames));
                BUF.append(String.format("%s occurs the most often (%d occurrences)</p>", most.toHTML(null), frameCounts.get(most)));

                this.shouldWorry = true;

            } else if (numStacks > 50 || numFrames > 150) {
                BUF.append(String.format("<p>There are %d stacks and %d frames, which may indicate a problem.</p>", numStacks, numFrames));
                BUF.append(String.format("%s occurs the most often (%d occurrences)</p>", most.toHTML(null), frameCounts.get(most)));

                this.shouldWorry = true;
            }

            List<ActiveScript> deep = new ArrayList<>();
            for (ActiveScript thread : result.ESS.getPapyrus().getActiveScripts().values()) {
                if (thread.getStackFrames().size() >= 100) {
                    deep.add(thread);
                }
            }
            deep.sort((a1, a2) -> Integer.compare(a2.getStackFrames().size(), a1.getStackFrames().size()));

            if (!deep.isEmpty()) {
                ActiveScript deepest = deep.get(0);
                int depth = deepest.getStackFrames().size();
                String msg = String.format("<p>There is a stack %d frames deep (%s).</p>", depth, deepest.toHTML(null));
                BUF.append(msg);
                this.shouldWorry = true;
            }
        }

        // Get a map of namespaces to scriptInstances in that namespace.
        Map<String, List<ScriptInstance>> currentNamespaces = new HashMap<>();
        for (ScriptInstance instance : result.ESS.getPapyrus().getScriptInstances().values()) {
            if (instance.getScriptName().toString().contains(":")) {
                currentNamespaces.computeIfAbsent(instance.getScriptName().toString().split(":")[0], k -> new ArrayList<>()).add(instance);
            }
        }

        Map<Script, Integer> currentCanaries = new HashMap<>();
        for (ScriptInstance instance : result.ESS.getPapyrus().getScriptInstances().values()) {
            if (instance.hasCanary()) {
                if (currentCanaries.put(instance.getScript(), instance.getCanary()) != null) {
                    throw new IllegalStateException("Duplicate key");
                }
            }
        }

        if (previousESS != null) {
            Header H1 = previousESS.getHeader();
            Header H2 = result.ESS.getHeader();

            if (WStringElement.compare(H1.NAME, H2.NAME) == 0 && H1.FILETIME < H2.FILETIME) {
                int previousSize = this.previousESS.calculateSize();
                int currentSize = result.ESS.calculateSize();
                double difference = 200.0 * (currentSize - previousSize) / (currentSize + previousSize);
                if (difference < -5.0) {
                    String msg = String.format("<p>This savefile has %2.2f%% less papyrus data the previous one.</p>", -difference);
                    BUF.append(msg);
                    this.shouldWorry = true;
                }
            }

            List<String> missingNamespaces = new ArrayList<>();
            for (String namespace : this.previousNamespaces.keySet()) {
                if (!currentNamespaces.containsKey(namespace)) {
                    for (ScriptInstance scriptInstance : this.previousNamespaces.get(namespace)) {
                        RefID refID = scriptInstance.getRefID();
                        if (!refID.isZero()) {
                            if (result.ESS.getChangeForms().containsKey(refID)) {
                                missingNamespaces.add(namespace);
                                break;
                            }
                        }
                    }
                }
            }

            if (!missingNamespaces.isEmpty()) {
                String msg = "This savefile has %d missing namespaces (the Canary error).";
                BUF.append(makeHTMLList(msg, missingNamespaces, LIMIT, i -> i));
                this.shouldWorry = true;
            }

            List<Script> canaryErrors = new ArrayList<>();
            for (Script script : this.previousCanaries.keySet()) {
                if (currentCanaries.containsKey(script)) {
                    if (previousCanaries.get(script) != 0) {
                        if (currentCanaries.get(script) == 0) {
                            canaryErrors.add(script);
                        }
                    }
                }
            }

            if (!canaryErrors.isEmpty()) {
                String msg = "This savefile has %d zeroed canaries.";
                BUF.append(makeHTMLList(msg, canaryErrors, LIMIT, i -> i.toHTML(null)));
                this.shouldWorry = true;
            }
        }

        List<ScriptInstance> memberless = new ArrayList<>();
        for (ScriptInstance scriptInstance : result.ESS.getPapyrus().getScriptInstances().values()) {
            if (scriptInstance.hasMemberlessError()) {
                memberless.add(scriptInstance);
            }
        }

        if (!memberless.isEmpty()) {
            String msg = "This savefile has %d script instances whose data is missing.";
            BUF.append(makeHTMLList(msg, memberless, LIMIT, i -> i.getScript().toHTML(null)));
            this.shouldWorry = true;
        }

        List<ScriptInstance> definitionErrors = new ArrayList<>();
        for (ScriptInstance scriptInstance : result.ESS.getPapyrus().getScriptInstances().values()) {
            if (scriptInstance.hasDefinitionError()) {
                definitionErrors.add(scriptInstance);
            }
        }

        if (!definitionErrors.isEmpty()) {
            String msg = "This savefile has %d script instances with mismatched member data.";
            BUF.append(makeHTMLList(msg, definitionErrors, LIMIT, i -> i.getScript().toHTML(null)));
            this.shouldWorry = true;
        }

        this.previousNamespaces = currentNamespaces;
        this.previousCanaries = currentCanaries;
        this.previousESS = result.ESS;

        return BUF;
    }

    public void check(ESS.Result result) {
        this.message = null;
        this.shouldWorry = false;
        this.disableSaving = false;

        final CharSequence PERFORMANCE = this.checkPerformance(result);
        final CharSequence FATAL = this.checkFatal(result);
        final CharSequence NONFATAL = this.checkNonFatal(result);

        final StringBuilder BUF = new StringBuilder();

        if (this.shouldDisableSaving()) {
            BUF.append("<h3>Serious problems were identified</h2><h3>Saving is disabled. Trust me, it's for your own good.</h3>");
            BUF.append(FATAL).append("<hr/>");
        }

        BUF.append(PERFORMANCE).append("<hr/>");

        if (this.shouldWorry()) {
            BUF.append(this.shouldDisableSaving()
                    ? "<h3>Additional problems were identified</h3>"
                    : "<h3>Potential problems were identified</h3>");
            BUF.append(NONFATAL).append("<hr/>");
        }

        this.message = BUF.toString();
    }

    public String getMessage() {
        return this.message;
    }

    public boolean shouldDisableSaving() {
        return this.disableSaving;
    }

    public boolean shouldWorry() {
        return this.shouldWorry;
    }

    private String message;
    private boolean shouldWorry;
    private boolean disableSaving;
    private ESS previousESS;
    private Map<Script, Integer> previousCanaries;
    private Map<String, List<ScriptInstance>> previousNamespaces;

    static final private Logger LOG = Logger.getLogger(Worrier.class.getCanonicalName());
    static final private PathMatcher MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{fos,ess}");
    static final private int LIMIT = 12;
}
