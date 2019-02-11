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
                .description("Reads in and parses MARC records from file\n" +
                        "and supports output in both MARC21 or DANMARC2 line-format and ISO2709");
        parser.addArgument("IN")
                .type(Arguments.fileType().acceptSystemIn().verifyCanRead())
                .help("Input file or standard input if given as a dash (-)");
        parser.addArgument("-f", "--format")
                .choices("LINE", "LINE_CONCAT", "ISO")
                .setDefault("LINE_CONCAT")
                .help("Output format.\n" +
                        "Defaults to LINE_CONCAT");
        parser.addArgument("-i", "--input-encoding")
                .setDefault("UTF-8")
                .help("Character set of the input MARC record(s)\n" +
                        "eg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.\n" +
                        "Defaults to UTF-8.");
        parser.addArgument("-o", "--output-encoding")
                .setDefault("UTF-8")
                .help("Character set of the output MARC record(s)\n" +
                        "eg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.\n" +
                        "Defaults to UTF-8.");
        parser.addArgument("--include-leader")
                .setDefault(Arguments.storeFalse())
                .action(Arguments.storeTrue())
                .help("Include leader in line format output (MARC21 only).\n" +
                        "Defaults to false.");
        try {
            this.args = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            throw new CliException(e);
        }
    }
}
