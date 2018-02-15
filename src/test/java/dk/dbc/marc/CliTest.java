/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.md
 */

package dk.dbc.marc;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CliTest {
    @Test
    void cli() {
        final Cli cli = new Cli(new String[]{"-", "--format=LINE"});
        assertThat("IN", cli.args.get("IN") instanceof File, is(true));
        assertThat("format", cli.args.getString("format"), is("LINE"));
    }

    @Test
    void defaults() {
        final Cli cli = new Cli(new String[]{"-"});
        assertThat("format", cli.args.getString("format"), is("LINE_CONCAT"));
    }

    @Test
    void inputFileNotFound() {
        assertThrows(CliException.class, () -> new Cli(new String[]{"no-such-file"}));
    }

    @Test
    void invalidFormat() {
        assertThrows(CliException.class, () -> new Cli(new String[]{"-f UNKNOWN"}));
    }
}