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
import dk.dbc.marc.reader.MarcXmlReader;
import dk.dbc.marc.writer.DanMarc2LineFormatWriter;
import dk.dbc.marc.writer.Iso2709MarcRecordWriter;
import dk.dbc.marc.writer.LineFormatWriter;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static dk.dbc.marc.RecordFormat.LINE;
import static dk.dbc.marc.RecordFormat.LINE_CONCAT;

@CommandLine.Command(name = "mconv ", version = "1.0", mixinStandardHelpOptions = true)
public class MarcConversionApp implements Runnable {
    private static final int PUSHBACK_BUFFER_SIZE = 1000;

    @CommandLine.Parameters(
            paramLabel = "inputfile",
            defaultValue = "-",
            description="Input file or standard input if given as a dash (-)" )
    File inputFile;

    @CommandLine.Option(
            names = { "-f", "--format"},
            defaultValue = "LINE_CONCAT",
            description = "Output format ${COMPLETION-CANDIDATES}, (default: ${DEFAULT_VALUE}).")
    RecordFormat outputFormat=LINE_CONCAT;

    @CommandLine.Option(
            names = {"-i", "--input-encoding"},
            defaultValue = "UTF8",
            description = "Character set of the input MARC record(s)\neg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.\nDefaults to ${DEFAULT_VALUE})"
    )
    Charset inputEncoding;
    @CommandLine.Option(
            names = {"-o", "--output-encoding"},
            defaultValue = "UTF8",
            description = "Character set of the output MARC record(s)\neg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.\nDefaults to ${DEFAULT_VALUE})"
    )
    Charset outputEncoding = StandardCharsets.UTF_8;

    @CommandLine.Option(names = {"-l", "--include-leader"},
            defaultValue = "false",
            description = "Include leader in line format output (MARC21 only). Defaults to ${DEFAULT_VALUE})"
    )
    Boolean includeLeader = Boolean.FALSE;
    @CommandLine.Option(names = {"-p", "--include-whitespace-padding"},
            defaultValue = "false",
            description = "Pad subfields with whitespace in line format output (MARC21 only). Defaults to ${DEFAULT_VALUE})"
    )
    Boolean includeWhitespacePadding = Boolean.FALSE;




    public static void main(String[] args) {
        int exitCode=0;
        try {
            exitCode=runWith(args);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(exitCode);
    }


    static int runWith(String[] args) throws CliException {
        CommandLine cli=new CommandLine( new MarcConversionApp()).setCaseInsensitiveEnumValuesAllowed(true);
        return cli.execute( args );
    }

    @Override
    public void run() {

        final File in = inputFile;
        try (PushbackInputStream is = "-".equals(in.getName())
                ? new PushbackInputStream(System.in, PUSHBACK_BUFFER_SIZE)
                : new PushbackInputStream(new FileInputStream( inputFile.getAbsolutePath() ), PUSHBACK_BUFFER_SIZE)) {
            final MarcReader marcRecordReader = getMarcReader( is, inputEncoding);
            MarcRecord record = marcRecordReader.read();
            if (record == null) {
                throw new IllegalArgumentException("Unknown input format");
            }
            final MarcWriter marcWriter = getMarcWriter(record);
            while (record != null) {
                System.out.write(marcWriter.write(record, outputEncoding));
                record = marcRecordReader.read();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (MarcReaderException | MarcWriterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private MarcReader getMarcReader(PushbackInputStream is, Charset encoding) throws MarcReaderException {
        final MarcFormatDeducer marcFormatDeducer = new MarcFormatDeducer(PUSHBACK_BUFFER_SIZE);

        Charset sampleEncoding = encoding;
        if (encoding instanceof DanMarc2Charset) {
            // Don't complicate the format deduction
            // by introducing the DanMarc2 charset
            // into the mix.
            sampleEncoding = StandardCharsets.ISO_8859_1;
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
                        .setProperty(LineFormatReader.Property.INCLUDE_WHITESPACE_PADDING, includeWhitespacePadding);
            case DANMARC2_LINE:
                return new DanMarc2LineFormatReader(is, encoding);
            case MARCXCHANGE:
                return new MarcXchangeV1Reader(is, encoding);
            case MARCXML:
                return new MarcXmlReader(is, encoding);
            default:
                return new Iso2709Reader(is, encoding);
        }
    }

    private MarcWriter getMarcWriter(MarcRecord record) {
        switch (outputFormat) {
            case LINE: // pass-through
            case LINE_CONCAT:
                return getLineFormatWriterVariant(record);
            case ISO:
                return new Iso2709MarcRecordWriter();
            default:
                throw new IllegalStateException("Unhandled format: Shut not happen" );
        }
    }

    private MarcWriter getLineFormatWriterVariant(MarcRecord record) {
        if (isDanMarc2(record)) {
            return outputFormat == LINE ? new DanMarc2LineFormatWriter()
                    : new DanMarc2LineFormatConcatWriter();
        }
        final LineFormatWriter lineFormatWriter = outputFormat == LINE
                ? new LineFormatWriter() : new LineFormatConcatWriter();

        return lineFormatWriter
                .setProperty(LineFormatWriter.Property.INCLUDE_LEADER,
                        includeLeader)
                .setProperty(LineFormatWriter.Property.INCLUDE_WHITESPACE_PADDING,
                        includeWhitespacePadding);
    }

    private static boolean isDanMarc2(MarcRecord record) {
        final List<Field> fields = record.getFields();
        return !fields.isEmpty() && fields.get(0) instanceof DataField;
    }
}
