/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.md
 */

package dk.dbc.marc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;

@ResourceLock( SYSTEM_OUT )
class MarcConversionAppTest {
    @Test
    void appLineConcatOutput() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/marc_collection.xml", "--format=LINE_CONCAT"));

        assertThat(capturedStdout, is(readResourceAsString("src/test/resources/marc_collection.lin_concat")));
    }

    @Test
    void returnZeroOnSucces() {
        int exitCode=MarcConversionApp.runWith("src/test/resources/marc_collection.xml", "--format=LINE_CONCAT");

        assertThat( exitCode, is (0));
    }

    @Test
    void returnNonZeroOnFail() {
        int exitCode=MarcConversionApp.runWith("src/test/resources/marc_collection.xml", "--format=NON_EXISTING_FORMAT");

        assertThat( exitCode, is(not(0)));
    }

    @Test
    void appLineOutput() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/marc_collection.iso", "-o", "danmarc2", "--format=iso"));

        assertThat(capturedStdout, is(readResourceAsString("src/test/resources/marc_collection.expected_dm2.iso")));
    }

    @Test
    void marcXmlLine() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/marcxml_minimal.xml", "--format=LINE"));

        assertThat(capturedStdout, is("001 control1\n100    $acode-a$bcode-b\n\n"));
    }

    @Test
    void marcMmlLineWhiteSpaceShort() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/marcxml_minimal.xml", "-p","--format=LINE"));

        assertThat(capturedStdout, is("001 control1\n100    $a code-a $b code-b\n\n"));
    }


    @Test
    void marcXmlLineWhiteSpaceLong() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/marcxml_minimal.xml", "--include-whitespace-padding","--format=LINE"));

        assertThat(capturedStdout, is("001 control1\n100    $a code-a $b code-b\n\n"));
    }

    @Test
    void marcXmlLineWithLeaderShort() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/marcxml_minimal.xml", "-l","--format=LINE"));

        assertThat(capturedStdout, is("00925njm  22002777a 4500\n001 control1\n100    $acode-a$bcode-b\n\n"));
    }

    @Test
    void marcXmlLineWithLeaderLong() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/marcxml_minimal.xml", "--include-leader","--format=LINE"));

        assertThat(capturedStdout, is("00925njm  22002777a 4500\n001 control1\n100    $acode-a$bcode-b\n\n"));
    }


    @Test
    void marc8isoOutput() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/record_with_utf8.xml", "-o", "marc8", "--format=iso"));

        assertThat(capturedStdout, is(readResourceAsString("src/test/resources/record_with_utf8_in_marc8.iso")));
    }

    @Test void marc8Input() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/record_with_utf8_in_marc8.iso", "-i", "marc8", "--format=line"));

        assertThat( capturedStdout, is("010 00 *axαx\n$\n"));
    }

    @Test void danmarc2isoOutput() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/record_with_utf8.xml", "-o", "danmarc2", "--format=iso"));

        assertThat(capturedStdout, is(readResourceAsString("src/test/resources/record_with_utf8_in_danmarc2.iso")));
    }

    @Test
    void danmarc2Input() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("src/test/resources/record_with_utf8_in_danmarc2.iso", "-i", "danmarc2", "--format=line"));

        assertThat( capturedStdout, is("010 00 *axαx\n$\n"));
    }

    @Test
    void readRecordsFromStandardInWhenNoFilenameIsGiven() throws Exception {
       String standardInput=readResourceAsString("src/test/resources/marcxml_minimal.xml");

        withTextFromSystemIn(standardInput).execute(() -> {
           String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("--format=LINE"));

           assertThat(capturedStdout, is("001 control1\n100    $acode-a$bcode-b\n\n"));
        });
    }

    @Test
    void readRecordsFromStandardInWhenDashAsFileNameIsGiven() throws Exception {
        String standardInput=readResourceAsString("src/test/resources/marcxml_minimal.xml");

        withTextFromSystemIn(standardInput).execute(() -> {
            String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("-", "--format=LINE"));

            assertThat(capturedStdout, is("001 control1\n100    $acode-a$bcode-b\n\n"));
        });
    }

    private String readResourceAsString(String resource) throws IOException {
        return Files.readString(Paths.get(resource), StandardCharsets.UTF_8);
    }
}