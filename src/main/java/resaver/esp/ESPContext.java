/*
 * Copyright 2016 Mark Fairchild
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
package resaver.esp;

import java.util.LinkedList;
import java.util.Objects;
import resaver.Game;
import resaver.IString;
import resaver.ess.Plugin;

/**
 * Stores the information that ESP elements require to read and write themselves
 * to and from files.
 *
 * SAMPLE checkpointer:
        ctx.check("Skyrim.esm", "MGEF", "0010fc14", "VMAD");
        
 * 
 * @author Mark Fairchild
 */
final public class ESPContext {

    /**
     * Create a new <code>ESSContext</code> from an ESS <code>Header</code>.
     *
     * @param game
     * @param plugin
     * @param tes4
     */
    public ESPContext(Game game, Plugin plugin, RecordTes4 tes4) {
        Objects.requireNonNull(game);
        Objects.requireNonNull(plugin);
        this.GAME = game;
        this.TES4 = tes4;
        this.CONTEXT = new LinkedList<>();
        this.PLUGIN_INFO = new PluginData(plugin);
        pushContext(plugin.NAME);
    }

    public void pushContext(CharSequence ctx) {
        this.CONTEXT.addLast(IString.get(ctx.toString()));
    }

    public void popContext() {
        this.CONTEXT.removeLast();
    }

    public boolean check(String... levels) {        
        int matches = 0;
        
        for (String l : levels) {
            IString level = IString.get(l);
            if (!this.CONTEXT.contains(level)) {
                return false;
            } else {
                matches++;
            }
        }

        return true;
    }

    /**
     * Remaps formIDs. If the formID's master is not available, the plugin field
     * of the formid will be set to 255.
     *
     * @param id The ID to remap.
     * @return
     */
    public int remapFormID(int id) {
        return null == this.TES4 ? id : this.TES4.remapFormID(id, this);
    }
    
    @Override
    public String toString() {
        return this.CONTEXT.toString();
    }

    final public Game GAME;
    final public RecordTes4 TES4;
    final private LinkedList<IString> CONTEXT;
    final public PluginData PLUGIN_INFO;

}
