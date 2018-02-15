/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.md
 */

package dk.dbc.marc;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class MarcConversionAppTest {
    @Test
    void appLineConcatOutput() throws IOException {
        final PrintStream out = System.out;
        try (ByteArrayOutputStream capturedStdout = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(capturedStdout));

            MarcConversionApp.runWith(new String[]
                    {"src/test/resources/marc_collection.xml", "--format=LINE_CONCAT"});

            assertThat(capturedStdout.toString(StandardCharsets.UTF_8.name()),
                    is(readResourceAsString("src/test/resources/marc_collection.lin_concat",
                            StandardCharsets.UTF_8)));
        } finally {
            System.setOut(out);  // restore System.out
        }
    }

    private String readResourceAsString(String resource, Charset encoding) throws IOException {
        return new String(Files.readAllBytes(Paths.get(resource)), encoding);
    }
}