/*
 * Copyright 2017 Mark.
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
package resaver.ess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import resaver.Game;
import resaver.ProgressModel;

/**
 * Tests the read and write methods of the <code>ESS</code> class.
 *
 * @author Mark Fairchild
 */
public class ESSTest {

    final static public Path WORK_DIR = Paths.get(System.getProperty("user.dir"));
    final static public Path TESTSAVES_DIR = WORK_DIR.resolve("src/test/resources/TestSaves");
    static final private Logger LOG = Logger.getLogger(ESSTest.class.getCanonicalName());
    final private java.util.List<Path> PATHS;
    

    public ESSTest() {
        // Set up logging stuff.
        LOG.getParent().getHandlers()[0].setFormatter(new java.util.logging.Formatter() {
            @Override
            public String format(LogRecord record) {
                final java.util.logging.Level LEVEL = record.getLevel();
                final String MSG = record.getMessage();
                final String SRC = record.getSourceClassName() + "." + record.getSourceMethodName();
                final String LOG = String.format("%s: %s: %s\n", SRC, LEVEL, MSG);
                return LOG;
            }
        });

        LOG.getParent().getHandlers()[0].setLevel(Level.INFO);

        java.util.List<Path> paths;

        try {
            paths = Files.walk(TESTSAVES_DIR)
                    .filter(p -> Game.FILTER_ALL.accept(p.toFile()))
                    .filter(Files::isReadable)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            System.out.println("Error while reading test files.");
            System.err.println(ex.getMessage());
            paths = Collections.emptyList();
        }

        this.PATHS = paths;
    }

    /*static public Stream<Path> pathProvider() {
        java.util.List<Path> paths;

        try {
            paths = Files.walk(TESTSAVES_DIR)
                    .filter(p -> Game.FILTER_ALL.accept(p.toFile()))
                    .filter(Files::isReadable)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            System.out.println(paths);
        } catch (IOException ex) {
            System.out.println("Error while reading test files.");
            System.err.println(ex.getMessage());
            paths = Collections.emptyList();
        }

        return paths.stream();
    }*/

    @Test
    public void testReadESS() {
        int index = 0;
        final int COUNT = this.PATHS.size();
        
        for (Path path : this.PATHS) {
            System.out.println(String.format("Test save %d / %d : %s", index, COUNT, path));
            testReadESS(path);
            index++;
        }
    }
    
    /**
     * Test of readESS and writeESS methods, of class ESS.
     *
     * @param path
     */
    //@ParameterizedTest
    //@MethodSource("pathProvider")    
    public void testReadESS(Path path) {
        //System.out.printf("readESS (%s)\n", WORK_DIR.relativize(path));
        try {
            ModelBuilder MODEL_ORIGINAL = new ModelBuilder(new ProgressModel(1));
            final ESS.Result IN_RESULT = ESS.readESS(path, MODEL_ORIGINAL);
            final ESS ORIGINAL = IN_RESULT.ESS;

            if (ORIGINAL.isTruncated() || ORIGINAL.getPapyrus().getStringTable().hasSTB()) {                
                return;
            }
            
            final String EXT = "." + ORIGINAL.getHeader().GAME.SAVE_EXT;

            final Path F2 = Files.createTempFile("ess_test", EXT);
            ESS.writeESS(ORIGINAL, F2);

            ModelBuilder MODEL_RESAVE = new ModelBuilder(new ProgressModel(1));
            final ESS.Result OUT_RESULT = ESS.readESS(F2, MODEL_RESAVE);
            final ESS REWRITE = OUT_RESULT.ESS;

            assertEquals(ORIGINAL.getDigest(), REWRITE.getDigest(), "Verify that digests match for " + path);
            ESS.verifyIdentical(ORIGINAL, REWRITE);

        } catch (RuntimeException | AssertionError | IOException ex) {
            System.err.println("Problem with " + path.getFileName() + "\n" + ex.getMessage());
            ex.printStackTrace(System.err);
            fail(path.getFileName().toString());
        }
    }

}
