/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.md
 */

package dk.dbc.marc;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.marc.reader.Iso2709Reader;
import dk.dbc.marc.reader.LineFormatReader;
import dk.dbc.marc.reader.MarcReader;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.writer.DanMarc2LineFormatWriter;
import dk.dbc.marc.writer.Iso2709Writer;
import dk.dbc.marc.writer.LineFormatWriter;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;

public class MarcConversionApp {
    private static final int PUSHBACK_BUFFER_SIZE = 1000;

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
        try (PushbackInputStream is = "-".equals(in.getName())
                ? new PushbackInputStream(System.in, PUSHBACK_BUFFER_SIZE)
                : new PushbackInputStream(new FileInputStream((File) cli.args.get("IN")), PUSHBACK_BUFFER_SIZE)) {
            final Charset inputEncoding = Encoding.of(cli.args.getString("input_encoding"));
            final Charset outputEncoding = Encoding.of(cli.args.getString("output_encoding"));
            final MarcReader marcRecordReader = getMarcReader(cli, is, inputEncoding);
            MarcRecord record = marcRecordReader.read();
            final MarcWriter marcWriter = getMarcWriter(cli, record);
            while (record != null) {
                System.out.write(marcWriter.write(record, outputEncoding));
                record = marcRecordReader.read();
            }
        } catch (FileNotFoundException e) {
            throw new CliException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (MarcReaderException | MarcWriterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static MarcReader getMarcReader(Cli cli, PushbackInputStream is, Charset encoding) throws MarcReaderException {
        final MarcFormatDeducer marcFormatDeducer = new MarcFormatDeducer(PUSHBACK_BUFFER_SIZE);

        Charset sampleEncoding = encoding;
        if (encoding instanceof DanMarc2Charset) {
            // Don't complicate the format deduction
            // by introducing the DanMarc2 charset
            // into the mix.
            sampleEncoding = Encoding.of("LATIN1");
        }
        final MarcFormatDeducer.FORMAT format =
                marcFormatDeducer.deduce(is, sampleEncoding);

        if (format == MarcFormatDeducer.FORMAT.LINE
                && encoding instanceof DanMarc2Charset) {
            // For line format we need a special
            // variant of the DanMarc2 charset.
            encoding = new DanMarc2Charset(DanMarc2Charset.Variant.LINE_FORMAT);
        }

        switch (format) {
            case LINE:
                return new LineFormatReader(is, encoding)
                        .setProperty(LineFormatReader.Property.INCLUDE_WHITESPACE_PADDING,
                                cli.args.getBoolean("include_whitespace_padding"));
            case DANMARC2_LINE:
                return new DanMarc2LineFormatReader(is, encoding);
            case MARCXCHANGE:
                return new MarcXchangeV1Reader(is, encoding);
            default:
                return new Iso2709Reader(is, encoding);
        }
    }

    private static MarcWriter getMarcWriter(Cli cli, MarcRecord record) {
        final String format = cli.args.getString("format");
        switch (format) {
            case "LINE": // pass-through
            case "LINE_CONCAT":
                return getLineFormatWriterVariant(cli, record);
            case "ISO":
                return new Iso2709Writer();
            default:
                throw new IllegalStateException("Unhandled format: " + format);
        }
    }

    private static MarcWriter getLineFormatWriterVariant(Cli cli, MarcRecord record) {
        final String format = cli.args.getString("format");
        if (isDanMarc2(record)) {
            return format.equals("LINE") ? new DanMarc2LineFormatWriter()
                    : new DanMarc2LineFormatConcatWriter();
        }
        final LineFormatWriter lineFormatWriter = format.equals("LINE")
                ? new LineFormatWriter() : new LineFormatConcatWriter();
        return lineFormatWriter
                .setProperty(LineFormatWriter.Property.INCLUDE_LEADER,
                        cli.args.getBoolean("include_leader"))
                .setProperty(LineFormatWriter.Property.INCLUDE_WHITESPACE_PADDING,
                        cli.args.getBoolean("include_whitespace_padding"));
    }

    private static boolean isDanMarc2(MarcRecord record) {
        final List<Field> fields = record.getFields();
        return !fields.isEmpty() && fields.get(0) instanceof DataField;
    }
}
