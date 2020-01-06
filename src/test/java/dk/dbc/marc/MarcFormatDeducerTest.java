/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.marc;

import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class MarcFormatDeducerTest {
    private static final int PUSHBACK_BUFFER_SIZE = 500;

    final MarcFormatDeducer deducer = new MarcFormatDeducer(PUSHBACK_BUFFER_SIZE);

    @Test
    void deduceMarcxchange() {
        final MarcFormatDeducer.FORMAT format = deducer.deduce(
                fromResource("src/test/resources/marc_collection.xml", PUSHBACK_BUFFER_SIZE),
                StandardCharsets.UTF_8);
        assertThat(format, is(MarcFormatDeducer.FORMAT.MARCXCHANGE));
    }

    @Test
    void deduceMarcXml() {
        final MarcFormatDeducer.FORMAT format = deducer.deduce(
                fromResource("src/test/resources/marcxml_collection.xml", PUSHBACK_BUFFER_SIZE),
                StandardCharsets.UTF_8);
        assertThat(format, is(MarcFormatDeducer.FORMAT.MARCXML));
    }

    @Test
    void deduceLineFormat() {
        final MarcFormatDeducer.FORMAT format = deducer.deduce(
                fromResource("src/test/resources/marc_collection.lin", PUSHBACK_BUFFER_SIZE),
                StandardCharsets.UTF_8);
        assertThat(format, is(MarcFormatDeducer.FORMAT.LINE));
    }

    @Test
    void deduceDanmarc2LineFormat() {
        final MarcFormatDeducer.FORMAT format = deducer.deduce(
                fromResource("src/test/resources/marc_collection.danmarc2_lin", PUSHBACK_BUFFER_SIZE),
                StandardCharsets.UTF_8);
        assertThat(format, is(MarcFormatDeducer.FORMAT.DANMARC2_LINE));
    }

    @Test
    void deduceISO2709() {
        final MarcFormatDeducer.FORMAT format = deducer.deduce(
                fromResource("src/test/resources/marc_collection.iso", PUSHBACK_BUFFER_SIZE),
                StandardCharsets.UTF_8);
        assertThat(format, is(MarcFormatDeducer.FORMAT.ISO2709));
    }

    private PushbackInputStream fromResource(String resource, int pushbackBufferSize) {
        try {
            return new PushbackInputStream(
                    new FileInputStream(Paths.get(resource).toFile()),
                    pushbackBufferSize);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}