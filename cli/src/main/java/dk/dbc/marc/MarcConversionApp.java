package dk.dbc.marc;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.marc.reader.Iso2709Reader;
import dk.dbc.marc.reader.Iso2709ReaderException;
import dk.dbc.marc.reader.JsonLineReader;
import dk.dbc.marc.reader.LineFormatReader;
import dk.dbc.marc.reader.MarcReader;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.reader.MarcXmlReader;
import dk.dbc.marc.writer.DanMarc2LineFormatWriter;
import dk.dbc.marc.writer.Iso2709MarcRecordWriter;
import dk.dbc.marc.writer.JsonLineWriter;
import dk.dbc.marc.writer.LineFormatWriter;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dk.dbc.marc.RecordFormat.LINE;

@CommandLine.Command(name = "mconv ", version = "2.0",
        description = "Parses MARC records while supporting output in various formats",
        mixinStandardHelpOptions = true)
public class MarcConversionApp implements Runnable {
    private static final int PUSHBACK_BUFFER_SIZE = 1000;

    private enum Mode {
        LAX,
        STRICT
    }

    @CommandLine.Parameters(
            paramLabel = "inputfile",
            defaultValue = "-",
            description="Input file or standard input if given as a dash (-)" )
    File inputFile;

    @CommandLine.Option(
            names = { "-m", "--mode"},
            defaultValue = "LAX",
            description = "Output mode ${COMPLETION-CANDIDATES}\nSee README.md for a detailed description of the mode option.\nDefaults to ${DEFAULT-VALUE}.")
    Mode mode=Mode.LAX;

    @CommandLine.Option(
            names = { "-f", "--format"},
            defaultValue = "LINE",
            description = "Output format ${COMPLETION-CANDIDATES}\nDefaults to ${DEFAULT-VALUE}.")
    RecordFormat outputFormat=LINE;

    @CommandLine.Option(
            names = {"-i", "--input-encoding"},
            defaultValue = "UTF-8",
            description = "Character set of the input MARC record(s)\neg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.\nDefaults to ${DEFAULT-VALUE}."
    )
    Charset inputEncoding;

    @CommandLine.Option(
            names = {"-o", "--output-encoding"},
            defaultValue = "UTF-8",
            description = "Character set of the output MARC record(s)\neg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.\nDefaults to ${DEFAULT-VALUE}."
    )
    Charset outputEncoding = StandardCharsets.UTF_8;

    @CommandLine.Option(names = {"-l", "--include-leader"},
            description = "Include leader in line format output."
    )
    Optional<Boolean> includeLeader;

    @CommandLine.Option(names = {"-p", "--include-whitespace-padding"},
            description = "Pad subfields with whitespace in line format output."
    )
    Optional<Boolean> includeWhitespacePadding;

    @CommandLine.Option(names = {"-c", "--as-collection"},
            defaultValue = "false",
            description = "Output all input records in the same collection. Requires that the output format has support for collections.\nDefaults to ${DEFAULT-VALUE}."
    )
    Boolean asCollection = Boolean.FALSE;

    public static final String ERRDUMP_FILENAME = "mconv.errdump";
    private FileOutputStream errdumpFile = null;

    public static void main(String[] args) {
        System.exit(runWith(args));
    }

    static int runWith(String... args) throws CliException {
        final CommandLine cli = new CommandLine(new MarcConversionApp())
                .setExecutionExceptionHandler(new ExecutionExceptionHandler())
                .setCaseInsensitiveEnumValuesAllowed(true);
        return cli.execute(args);
    }

    @Override
    public void run() {
        final File in = inputFile;
        try (PushbackInputStream is = "-".equals(in.getName())
                ? new PushbackInputStream(System.in, PUSHBACK_BUFFER_SIZE)
                : new PushbackInputStream(new FileInputStream(inputFile.getAbsolutePath()), PUSHBACK_BUFFER_SIZE)) {
            final MarcReader marcRecordReader = getMarcReader(is, inputEncoding);
            MarcRecordOrError recordOrError = readMarcRecord(marcRecordReader);
            if (recordOrError == null) {
                throw new IllegalArgumentException("Unknown input format");
            }
            MarcWriter marcWriter = getMarcWriter(recordOrError);

            final List<MarcRecord> recordBuffer = new ArrayList<>();
            while (recordOrError != null) {
                if (recordOrError.isError()) {
                    dumpError(recordOrError);
                } else {
                    if (marcWriter == null) {
                        marcWriter = getMarcWriter(recordOrError);
                    }
                    if (Boolean.TRUE.equals(asCollection)) {
                        recordBuffer.add(recordOrError.getRecord());
                    } else {
                        System.out.write(marcWriter.write(recordOrError.getRecord(), outputEncoding));
                    }
                }
                recordOrError = readMarcRecord(marcRecordReader);
            }

            if (!recordBuffer.isEmpty()) {
                System.out.write(marcWriter.writeCollection(recordBuffer, outputEncoding));
            }

            if (errdumpFile != null) {
                throw new IllegalArgumentException(String.format(
                        "Input contained erroneous MARC data, see %s file for further details", ERRDUMP_FILENAME));
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (MarcReaderException | MarcWriterException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (errdumpFile != null) {
                try {
                    errdumpFile.close();
                } catch (IOException e) {
                    // noop
                }
            }
        }
    }

    private MarcReader getMarcReader(PushbackInputStream is, Charset encoding) throws MarcReaderException {
        final MarcFormatDeducer marcFormatDeducer = new MarcFormatDeducer(PUSHBACK_BUFFER_SIZE);

        Charset sampleEncoding = encoding;
        if (!(encoding.name().equals("UTF-8"))) {
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
            case JSONL:
                return new JsonLineReader(is, encoding);
            case LINE:
                return new LineFormatReader(is, encoding);
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

    private MarcWriter getMarcWriter(MarcRecordOrError recordOrError) {
        if (recordOrError.isError()) {
            return null;
        }
        MarcWriter marcWriter;
        switch (outputFormat) {
            case LINE: // pass-through
            case LINE_CONCAT:
                marcWriter = getLineFormatWriterVariant(recordOrError.getRecord());
                break;
            case ISO:
                marcWriter = new Iso2709MarcRecordWriter();
                break;
            case JSONL:
                marcWriter = new JsonLineWriter();
                break;
            case MARCXCHANGE:
                marcWriter = getMarcXchangeWriter();
                break;
            default:
                throw new IllegalStateException("Unhandled format: " + outputFormat);
        }

        if (Boolean.TRUE.equals(asCollection) && !marcWriter.canOutputCollection()) {
                throw new IllegalArgumentException("Output format " + outputFormat + " does not support collections");
        }

        return marcWriter;
    }

    private MarcWriter getLineFormatWriterVariant(MarcRecord record) {
        if (isDanMarc2(record)) {
            final DanMarc2LineFormatWriter danMarc2LineFormatWriter = outputFormat == LINE
                    ? new DanMarc2LineFormatWriter() : new DanMarc2LineFormatConcatWriter();

            if (mode == Mode.LAX) {
                danMarc2LineFormatWriter
                        .setProperty(DanMarc2LineFormatWriter.Property.INCLUDE_LEADER, true)
                        .setProperty(DanMarc2LineFormatWriter.Property.INCLUDE_WHITESPACE_PADDING, true)
                        .setProperty(DanMarc2LineFormatWriter.Property.USE_NEWLINE_END_OF_RECORD, true)
                        .setProperty(DanMarc2LineFormatWriter.Property.USE_WRAPPED_LINES, false);
            } else {
                danMarc2LineFormatWriter
                        .setProperty(DanMarc2LineFormatWriter.Property.INCLUDE_LEADER, true)
                        .setProperty(DanMarc2LineFormatWriter.Property.INCLUDE_WHITESPACE_PADDING, false)
                        .setProperty(DanMarc2LineFormatWriter.Property.USE_NEWLINE_END_OF_RECORD, false)
                        .setProperty(DanMarc2LineFormatWriter.Property.USE_WRAPPED_LINES, true);
            }

            includeLeader.ifPresent(flag -> danMarc2LineFormatWriter
                    .setProperty(DanMarc2LineFormatWriter.Property.INCLUDE_LEADER, flag));
            includeWhitespacePadding.ifPresent(flag -> danMarc2LineFormatWriter
                    .setProperty(DanMarc2LineFormatWriter.Property.INCLUDE_WHITESPACE_PADDING, flag));

            return danMarc2LineFormatWriter;
        }

        final LineFormatWriter lineFormatWriter = outputFormat == LINE
                ? new LineFormatWriter() : new LineFormatConcatWriter();

        if (mode == Mode.LAX) {
            lineFormatWriter
                    .setProperty(LineFormatWriter.Property.INCLUDE_LEADER, true)
                    .setProperty(LineFormatWriter.Property.INCLUDE_WHITESPACE_PADDING, true)
                    .setProperty(LineFormatWriter.Property.USE_STAR_SUBFIELD_MARKER, true);
        } else {
            lineFormatWriter
                    .setProperty(LineFormatWriter.Property.INCLUDE_LEADER, true)
                    .setProperty(LineFormatWriter.Property.INCLUDE_WHITESPACE_PADDING, false)
                    .setProperty(LineFormatWriter.Property.USE_STAR_SUBFIELD_MARKER, false);
        }

        includeLeader.ifPresent(flag -> lineFormatWriter
                .setProperty(LineFormatWriter.Property.INCLUDE_LEADER, flag));
        includeWhitespacePadding.ifPresent(flag -> lineFormatWriter
                .setProperty(LineFormatWriter.Property.INCLUDE_WHITESPACE_PADDING, flag));

        return lineFormatWriter;
    }

    private MarcWriter getMarcXchangeWriter() {
        final MarcXchangeV1Writer marcXchangeV1Writer = new MarcXchangeV1Writer();
        if (mode == Mode.LAX) {
            marcXchangeV1Writer.setProperty(MarcXchangeV1Writer.Property.ADD_XML_DECLARATION, false);
        } else {
            marcXchangeV1Writer.setProperty(MarcXchangeV1Writer.Property.ADD_XML_DECLARATION, true);
        }
        return marcXchangeV1Writer;
    }

    private MarcRecordOrError readMarcRecord(MarcReader reader) throws MarcReaderException {
        try {
            final MarcRecord record = reader.read();
            if (record == null) {
                return null;
            }
            return MarcRecordOrError.asRecord(record);
        } catch (Iso2709ReaderException e) {
            String errorMessage = e.getMessage();
            final Throwable cause = e.getCause();
            if (cause != null) {
                errorMessage = cause.getMessage();
            }
            return MarcRecordOrError.asError(errorMessage, e.getRecordBytes());
        }
    }

    private void dumpError(MarcRecordOrError error) {
        try {
            String errorMessageProlog = "\n";
            if (errdumpFile == null) {
                errdumpFile = new FileOutputStream(ERRDUMP_FILENAME, false);
                errorMessageProlog = "";
            }
            errdumpFile.write((errorMessageProlog + error.getErrorMessage() + "\n").getBytes(StandardCharsets.UTF_8));
            errdumpFile.write(error.getInputBytes());
            errdumpFile.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing dump file " + ERRDUMP_FILENAME, e);
        }
    }

    private static boolean isDanMarc2(MarcRecord record) {
        final List<Field> fields = record.getFields();
        return !fields.isEmpty() && fields.get(0) instanceof DataField;
    }

    private static class MarcRecordOrError {
        private static int recordNumber = 0;

        private final MarcRecord record;
        private final String errorMessage;
        private final byte[] inputBytes;

        public static MarcRecordOrError asRecord(MarcRecord record) {
            return new MarcRecordOrError(record, null, null);
        }

        public static MarcRecordOrError asError(String errorMessage, byte[] inputBytes) {
            return new MarcRecordOrError(null, errorMessage, inputBytes);
        }

        private MarcRecordOrError(MarcRecord record, String errorMessage, byte[] inputBytes) {
            this.record = record;
            this.errorMessage = errorMessage;
            this.inputBytes = inputBytes;
            recordNumber++;
        }

        public MarcRecord getRecord() {
            return record;
        }

        public String getErrorMessage() {
            return "Record number " + recordNumber + " - " + errorMessage;
        }

        public byte[] getInputBytes() {
            return inputBytes;
        }

        public boolean isError() {
            return errorMessage != null && inputBytes != null;
        }
    }
}
