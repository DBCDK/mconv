/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.md
 */

package dk.dbc.marc;

import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReader;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.writer.DanMarc2LineFormatWriter;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class MarcConversionApp {
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

    static void runWith(String[] args) throws CliException {
        final Cli cli = new Cli(args);
        final File in = cli.args.get("IN");
        try (InputStream is = "-".equals(in.getName()) ? System.in
                : new FileInputStream((File) cli.args.get("IN"))) {
            final MarcReader marcReader = getMarcReader(is);
            final MarcWriter marcWriter = getMarcWriter(cli);
            MarcRecord record = marcReader.read();
            while (record != null) {
                System.out.write(marcWriter.write(record, StandardCharsets.UTF_8));
                record = marcReader.read();
            }
        } catch (FileNotFoundException e) {
            throw new CliException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (MarcReaderException | MarcWriterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static MarcReader getMarcReader(InputStream is) throws MarcReaderException {
        // TODO: 13-02-18 In time this should made to guess the marc format instead of assuming marcXchange
        return new MarcXchangeV1Reader(is, StandardCharsets.UTF_8);
    }

    private static MarcWriter getMarcWriter(Cli cli) {
        final String format = cli.args.getString("format");
        switch (format) {
            case "LINE": return new DanMarc2LineFormatWriter();
            case "LINE_CONCAT": return new DanMarc2LineFormatConcatWriter();
            default: throw new IllegalStateException("Unhandled format: " + format);
        }
    }
}
