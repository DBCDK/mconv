/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.marc;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class EncodingTest {
    @Test
    void danmarc2() {
        assertThat(Encoding.of("danmarc-2").name(), is("DanMarc2"));
    }

    @Test
    void marc8() {
        assertThat(Encoding.of("marc-8").name(), is("Marc8"));
    }

    @Test
    void latin1() {
        assertThat(Encoding.of("latin-1").name(), is("ISO-8859-1"));
    }

    @Test
    void iso88591() {
        assertThat(Encoding.of("iso-8859-1").name(), is("ISO-8859-1"));
    }

    @Test
    void utf8() {
        assertThat(Encoding.of("UTF-8").name(), is("UTF-8"));
    }
}