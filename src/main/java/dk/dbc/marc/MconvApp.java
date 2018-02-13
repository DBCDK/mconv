/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.md
 */

package dk.dbc.marc;

public class MconvApp {
     public static void main(String[] args) {
        try {
            runWith(args);
        } catch (CliException e) {
            System.exit(1);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static void runWith(String[] args) throws CliException {
        final Cli cli = new Cli(args);
    }
}
