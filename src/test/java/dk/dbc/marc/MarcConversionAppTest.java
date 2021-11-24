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

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

class MarcConversionAppTest {
    @Test
    void appLineConcatOutput() throws Exception {
        String capturedStdout = tapSystemOut(() -> {
            MarcConversionApp.runWith(new String[]{"src/test/resources/marc_collection.xml", "--format=LINE_CONCAT"});
        });

        assertThat(capturedStdout, is(readResourceAsString("src/test/resources/marc_collection.lin_concat", StandardCharsets.UTF_8)));
    }

    @Test
    void returnZeroOnSucces() {
        int exitCode=MarcConversionApp.runWith(new String[]{"src/test/resources/marc_collection.xml", "--format=LINE_CONCAT"});

        assertThat( exitCode, is (0));
    }

    @Test
    void ReturnNonZerrorOnFail() {
        int exitCode=MarcConversionApp.runWith(new String[]{"src/test/resources/marc_collection.xml", "--format=NON_EXISTING_FORMAT"});

        assertThat( exitCode, is(not(0)));
    }

    @Test
    void appLineOutput() throws Exception {
        String capturedStdout = tapSystemOut(() -> {
            MarcConversionApp.runWith(new String[]{"src/test/resources/marc_collection.iso", "-o", "danmarc2", "--format=iso"});
        });

        assertThat(capturedStdout, is(readResourceAsString("src/test/resources/marc_collection.expected_dm2.iso", StandardCharsets.UTF_8)));
    }

    private String readResourceAsString(String resource, Charset encoding) throws IOException {
        return Files.readString(Paths.get(resource), encoding);
    }
}