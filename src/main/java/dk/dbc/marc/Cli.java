/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.md
 */

package dk.dbc.marc;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

class Cli {
    Namespace args;

    Cli(String[] args) throws CliException {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser("mconv");
        try {
            this.args = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            throw new CliException(e);
        }
    }
}
