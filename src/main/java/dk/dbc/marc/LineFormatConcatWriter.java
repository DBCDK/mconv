/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.marc;

import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.writer.LineFormatWriter;
import dk.dbc.marc.writer.MarcWriterException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public class LineFormatConcatWriter extends LineFormatWriter {
    @Override
    public byte[] write(MarcRecord marcRecord, Charset encoding)
            throws UnsupportedCharsetException, MarcWriterException {
        final byte[] bytes = super.write(marcRecord, encoding);
        if (bytes != null) {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            final String[] lines = new String(bytes, encoding).split("\n");
            try {
                for (int i = 0; i < lines.length; i++) {
                    String concatLine = "\"" + lines[i].replaceAll("\"", "\\\\\\\"") + "\\n\"";
                    if (i < lines.length - 1) {
                        concatLine += " +\n";
                    }
                    buffer.write(concatLine.getBytes(encoding));
                }
                buffer.write("\n".getBytes(encoding));
            } catch (IOException e) {
                throw new MarcWriterException("", e);
            }
            return buffer.toByteArray();
        }
        return null;
    }
}
