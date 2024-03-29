package dk.dbc.marc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;

@ResourceLock(SYSTEM_OUT)
class MarcConversionAppTest {

    private static final Path errdumpFile = Paths.get(MarcConversionApp.ERRDUMP_FILENAME);

    @AfterEach
    void removeErrorDump() throws IOException {
        Files.deleteIfExists(errdumpFile);
    }

    @Test
    void returnZeroOnSucces() {
        int exitCode = MarcConversionApp.runWith(resource("marc_collection.xml"), "--format=LINE_CONCAT");
        assertThat("exit code", exitCode, is(0));
        assertThat("errdump file exists", Files.exists(errdumpFile), is(false));
    }

    @Test
    void returnNonZeroOnFail() {
        int exitCode = MarcConversionApp.runWith(resource("marc_collection.xml"), "--format=NON_EXISTING_FORMAT");
        assertThat("exit code", exitCode, is(not(0)));
        assertThat("errdump file exists", Files.exists(errdumpFile), is(false));
    }

    @Test
    void failWithErrorDump() {
        int exitCode = MarcConversionApp.runWith(resource("err.mrc"));
        assertThat("exit code", exitCode, is(not(0)));
        assertThat("errdump file exists", Files.exists(errdumpFile), is(true));
    }

    @Test
    void formatMarcXchangeCollection() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marc.jsonl"), "-c", "--format=MARCXCHANGE"));

        assertThat(capturedStdout, is(
                "<collection xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                        "<record>" +
                          "<datafield ind1='0' ind2='0' tag='001'>" +
                            "<subfield code='a'>30769430</subfield>" +
                          "</datafield>" +
                        "</record>" +
                        "<record>" +
                          "<datafield ind1='0' ind2='0' tag='001'>" +
                            "<subfield code='a'>30769431</subfield>" +
                          "</datafield>" +
                        "</record>" +
                      "</collection>"));
    }

    @Test
    void formatJsonLine() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marcxml_minimal.xml"), "--format=jsonl"));

        assertThat(capturedStdout, is(
                "{" +
                    "\"leader\":[\"0\",\"0\",\"9\",\"2\",\"5\",\"n\",\"j\",\"m\",\" \",\" \",\"2\",\"2\",\"0\",\"0\",\"2\",\"7\",\"7\",\"7\",\"a\",\" \",\"4\",\"5\",\"0\",\"0\"]," +
                    "\"fields\":[" +
                        "{\"name\":\"001\",\"value\":\"control1\"}," +
                        "{\"name\":\"100\",\"indicator\":[\" \",\" \"]," +
                            "\"subfields\":[" +
                                "{\"name\":\"a\",\"value\":\"code-a\"}," +
                                "{\"name\":\"b\",\"value\":\"code-b\"}" +
                            "]" +
                        "}" +
                    "]" +
                "}\n"
        ));
    }

    @Test
    void formatLineLax() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marcxml_minimal.xml"), "--format=LINE"));
        assertThat(capturedStdout, is("00925njm  22002777a 4500\n001 control1\n100    *a code-a *b code-b\n\n"));
    }

    @Test
    void formatLineLaxNoLeader() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marcxml_minimal.xml"), "--format=LINE", "--include-leader=false"));
        assertThat(capturedStdout, is("001 control1\n100    *a code-a *b code-b\n\n"));
    }

    @Test
    void formatLineLaxNoPadding() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marcxml_minimal.xml"), "--format=LINE", "--include-whitespace-padding=false"));
        assertThat(capturedStdout, is("00925njm  22002777a 4500\n001 control1\n100    *acode-a*bcode-b\n\n"));
    }

    @Test
    void FormatLineStrict() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marcxml_minimal.xml"), "--format=LINE", "--mode=strict"));
        assertThat(capturedStdout, is("00925njm  22002777a 4500\n001 control1\n100    $acode-a$bcode-b\n\n"));
    }

    @Test
    void formatLineStrictNoLeader() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marcxml_minimal.xml"), "--format=LINE", "--mode=strict", "--include-leader=false"));
        assertThat(capturedStdout, is("001 control1\n100    $acode-a$bcode-b\n\n"));
    }

    @Test
    void formatLineStrictWithPadding() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marcxml_minimal.xml"), "--format=LINE", "--mode=strict", "--include-whitespace-padding"));
        assertThat(capturedStdout, is("00925njm  22002777a 4500\n001 control1\n100    $a code-a $b code-b\n\n"));
    }

    @Test
    void formatMarcXchangeLax() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marcxml_minimal.xml"), "--format=MARCXCHANGE"));
        assertThat(capturedStdout, is("<record xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'><leader>00925njm  22002777a 4500</leader><controlfield tag='001'>control1</controlfield><datafield ind1=' ' ind2=' ' tag='100'><subfield code='a'>code-a</subfield><subfield code='b'>code-b</subfield></datafield></record>"));
    }

    @Test
    void formatMarcXchangeStrict() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marcxml_minimal.xml"), "--format=MARCXCHANGE", "--mode=STRICT"));
        assertThat(capturedStdout, is("<?xml version='1.0' encoding='UTF-8'?>\n<record xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'><leader>00925njm  22002777a 4500</leader><controlfield tag='001'>control1</controlfield><datafield ind1=' ' ind2=' ' tag='100'><subfield code='a'>code-a</subfield><subfield code='b'>code-b</subfield></datafield></record>"));
    }

    @Test
    void formatDanmarc2LineLax() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("record_with_utf8_in_danmarc2.iso"), "-i", "danmarc2", "--format=line"));
        assertThat(capturedStdout, is("LDR 00000n    2200000   4500\n010 00 *a xαx\n\n"));
    }

    @Test
    void formatDanmarc2LineLaxNoLeader() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("record_with_utf8_in_danmarc2.iso"), "-i", "danmarc2", "--format=line", "--include-leader=false"));
        assertThat(capturedStdout, is("010 00 *a xαx\n\n"));
    }

    @Test
    void formatDanmarc2LineLaxNoPadding() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("record_with_utf8_in_danmarc2.iso"), "-i", "danmarc2", "--format=line", "--include-whitespace-padding=false"));
        assertThat(capturedStdout, is("LDR 00000n    2200000   4500\n010 00 *axαx\n\n"));
    }

    @Test
    void formatDanmarc2LineStrict() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("record_with_utf8_in_danmarc2.iso"), "-i", "danmarc2", "--format=line", "--mode=strict"));
        assertThat(capturedStdout, is("LDR 00000n    2200000   4500\n010 00 *axαx\n$\n"));
    }

    @Test
    void formatDanmarc2LineStrictNoLeader() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("record_with_utf8_in_danmarc2.iso"), "-i", "danmarc2", "--format=line", "--mode=strict", "--include-leader=false"));
        assertThat(capturedStdout, is("010 00 *axαx\n$\n"));
    }

    @Test
    void formatDanmarc2LineStrictWithPadding() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("record_with_utf8_in_danmarc2.iso"), "-i", "danmarc2", "--format=line", "--mode=strict", "--include-whitespace-padding"));
        assertThat(capturedStdout, is("LDR 00000n    2200000   4500\n010 00 *a xαx\n$\n"));
    }

    @Test
    void formatDanmarc2LineConcat() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marc_collection.xml"), "--format=LINE_CONCAT", "--include-leader=false", "--mode=strict"));
        assertThat(capturedStdout, is(readResourceAsString("marc_collection.lin_concat")));
    }

    @Test
    void formatIso() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("marc_collection.iso"), "-o", "danmarc2", "--format=iso"));
        assertThat(capturedStdout, is(readResourceAsString("marc_collection.expected_dm2.iso")));
    }

    @Test
    void marc8Output() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("record_with_utf8.xml"), "-o", "marc8", "--format=iso"));

        assertThat(capturedStdout, is(readResourceAsString("record_with_utf8_in_marc8.iso")));
    }

    @Test
    void marc8Input() throws Exception {
        String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith(resource("record_with_utf8_in_marc8.iso"), "-i", "marc8", "--format=line", "--mode=strict"));
        assertThat( capturedStdout, is("LDR 00000n    2200000   4500\n010 00 *axαx\n$\n"));
    }

    @Test
    void readRecordsFromStandardInWhenNoFilenameIsGiven() throws Exception {
       String standardInput=readResourceAsString("marcxml_minimal.xml");

        withTextFromSystemIn(standardInput).execute(() -> {
           String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("--format=LINE"));

           assertThat(capturedStdout, is("00925njm  22002777a 4500\n001 control1\n100    *a code-a *b code-b\n\n"));
        });
    }

    @Test
    void readRecordsFromStandardInWhenDashAsFileNameIsGiven() throws Exception {
        String standardInput=readResourceAsString("marcxml_minimal.xml");

        withTextFromSystemIn(standardInput).execute(() -> {
            String capturedStdout = tapSystemOut(() -> MarcConversionApp.runWith("-", "--format=LINE"));

            assertThat(capturedStdout, is("00925njm  22002777a 4500\n001 control1\n100    *a code-a *b code-b\n\n"));
        });
    }

    private String readResourceAsString(String resource) throws IOException {
        URL url = getClass().getClassLoader().getResource(resource);
        try {
            return Files.readString(Paths.get(url.toURI()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String resource(String name) {
        return MarcConversionAppTest.class.getClassLoader().getResource(name).getFile();
    }
}
