/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.marc;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public final class Encoding {
    private Encoding() {}

    public static Charset of(String name) throws UnsupportedCharsetException {
        final String normalizedName = name.replaceFirst("-", "")
                .toUpperCase();

        switch (normalizedName) {
            case "DANMARC2":
                return new DanMarc2Charset();
            case "MARC8":
                return new Marc8Charset();
            default:
                return Charset.forName(normalizedName);
        }
    }
}
