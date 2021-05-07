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
package resaver.esp;

import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import resaver.IString;

/**
 * Describes script fragments for INFO records and PACK records.
 *
 * @author Mark
 */
public class FragmentInfoPack extends FragmentBase {

    public FragmentInfoPack(@NotNull ByteBuffer input, @NotNull ESPContext ctx) {
        this.UNKNOWN = input.get();
        this.FLAGS = input.get();

        if (ctx.GAME.isFO4()) {
            ctx.pushContext("FragmentInfoPack");
            this.FILENAME = null;
            this.SCRIPT = new Script(input, ctx);
                ctx.PLUGIN_INFO.addScriptData(this.SCRIPT);
        } else {
            this.FILENAME = mf.BufferUtil.getUTF(input);
            this.SCRIPT = null;
            ctx.pushContext("FragmentInfoPack:" + this.FILENAME);
        }

        this.FRAGMENTS = new java.util.LinkedList<>();

        int flagsCount = FragmentBase.NumberOfSetBits(this.FLAGS);
        for (int i = 0; i < flagsCount; i++) {
            Fragment fragment = new Fragment(input);
            this.FRAGMENTS.add(fragment);
        }
    }

    @Override
    public void write(@NotNull ByteBuffer output) {
        output.put(this.UNKNOWN);
        output.put(this.FLAGS);
        if (null != this.SCRIPT) {
            this.SCRIPT.write(output);
        }
        if (null != this.FILENAME) {
            output.put(this.FILENAME.getBytes(UTF_8));
        }

        this.FRAGMENTS.forEach(fragment -> fragment.write(output));
    }

    @Override
    public int calculateSize() {
        int sum = 2;
        sum += (null != this.SCRIPT ? this.SCRIPT.calculateSize() : 0);
        sum += (null != this.FILENAME ? 2 + this.FILENAME.length() : 0);
        int result = 0;
        for (Fragment FRAGMENT : this.FRAGMENTS) {
            int calculateSize = FRAGMENT.calculateSize();
            result += calculateSize;
        }
        sum += result;
        return sum;
    }

    @Override
    public String toString() {
        if (null != this.SCRIPT) {
            return String.format("InfoPack: %s (%d, %d, %d frags)", this.SCRIPT.NAME, this.FLAGS, this.UNKNOWN, this.FRAGMENTS.size());
        } else if (null != this.FILENAME) {
            return String.format("InfoPack: %s (%d, %d, %d frags)", this.FILENAME, this.FLAGS, this.UNKNOWN, this.FRAGMENTS.size());
        } else {
            return String.format("InfoPack: (%d, %d, %d frags)", this.FLAGS, this.UNKNOWN, this.FRAGMENTS.size());
        }
    }

    final byte UNKNOWN;
    final byte FLAGS;
    @Nullable
    final Script SCRIPT;
    @Nullable
    final String FILENAME;
    @NotNull
    final List<Fragment> FRAGMENTS;

    /**
     *
     */
    public class Fragment implements Entry {

        public Fragment(@NotNull ByteBuffer input) {
            this.UNKNOWN = input.get();
            this.SCRIPTNAME = IString.get(mf.BufferUtil.getUTF(input));
            this.FRAGMENTNAME = IString.get(mf.BufferUtil.getUTF(input));
        }

        @Override
        public void write(@NotNull ByteBuffer output) {
            output.put(this.UNKNOWN);
            output.put(this.SCRIPTNAME.getUTF8());
            output.put(this.FRAGMENTNAME.getUTF8());
        }

        @Override
        public int calculateSize() {
            return 5 + this.SCRIPTNAME.length() + this.FRAGMENTNAME.length();
        }

        final private byte UNKNOWN;
        @NotNull
        final private IString SCRIPTNAME;
        @NotNull
        final private IString FRAGMENTNAME;
    }
}
