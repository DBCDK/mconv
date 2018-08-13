/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.md
 */

package dk.dbc.marc;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

class Cli {
    Namespace args;

    Cli(String[] args) throws CliException {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser("mconv")
                .description("Reads in marcXchange records and outputs them as line format variant");
        parser.addArgument("IN")
                .type(Arguments.fileType().acceptSystemIn().verifyCanRead())
                .help("Input file or standard input if given as a dash (-)");
        parser.addArgument("-f", "--format")
                .choices("LINE", "LINE_CONCAT")
                .setDefault("LINE_CONCAT")
                .help("Output format");
        parser.addArgument("--include-leader")
                .setDefault(Arguments.storeFalse())
                .action(Arguments.storeTrue())
                .help("Include leader in line format output (MARC21 only)");
        try {
            this.args = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            throw new CliException(e);
        }
    }
}
