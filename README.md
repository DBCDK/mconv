# mconv
mconv is a commandline-tool to read in and parse MARC records from file and
output in either MARCXCHANGE, MARC21, DANMARC2 line-format, JSONL or ISO2709.

## Installation

```bash
$ curl -sL http://mavenrepo.dbc.dk/content/repositories/releases/dk/dbc/mconv/1.3/mconv-1.3.jar -o mconv.jar && unzip -op mconv.jar mconv | bash -s -- --install
```

Keep the installation up-to-date using the selfupdate action
```bash
mconv --selfupdate
```

## Usage
```bash
$ mconv -h
Usage: mconv  [-chlpV] [-f=<outputFormat>] [-i=<inputEncoding>] [-m=<mode>]
              [-o=<outputEncoding>] inputfile
Parses MARC records while supporting output in various formats
      inputfile          Input file or standard input if given as a dash (-)
  -c, --as-collection    Output all input records in the same collection.
                           Requires that the output format has support for
                           collections.
                         Defaults to false.
  -f, --format=<outputFormat>
                         Output format LINE, LINE_CONCAT, MARCXCHANGE, ISO,
                           JSONL
                         Defaults to LINE.
  -h, --help             Show this help message and exit.
  -i, --input-encoding=<inputEncoding>
                         Character set of the input MARC record(s)
                         eg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.
                         Defaults to UTF-8.
  -l, --include-leader   Include leader in line format output.
  -m, --mode=<mode>      Output mode LAX, STRICT
                         See README.md for a detailed description of the mode
                           option.
                         Defaults to LAX.
  -o, --output-encoding=<outputEncoding>
                         Character set of the output MARC record(s)
                         eg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.
                         Defaults to UTF-8.
  -p, --include-whitespace-padding
                         Pad subfields with whitespace in line format output.
  -V, --version          Print version information and exit.
```

```bash
$ mconv marc_collection.xml
```

```bash
$ cat marc_collection.xml | mconv -
```

## Output format

* LINE - line format DANMARC2 or MARC21 variant
* LINE_CONCAT - paste-into-code friendly DANMARC2 or MARC21 line format (default)
* ISO - ISO2709
* JSONL - MARC JSON in newline delimited json format
* MARCXCHANGE - MarcXchange XML

## Lax vs Strict mode

| Output format                  | Lax                                                                                                        | Strict                                                                                                       |
|--------------------------------|------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| ISO                            | Same                                                                                                       | Same                                                                                                         |
| JSONL                          | Same                                                                                                       | Same                                                                                                         |
| LINE (DANMARC2 variant)        | Include leader<br/>Include whitespace padding<br/>No line wrapping<br/>Use newline as end-of-record marker | Include leader<br/>No whitespace padding<br/>Wrap lines<br/>Use dollar ($) + newline as end-of-record marker |
| LINE_CONCAT (DANMARC2 variant) | Same as LINE (DANMARC2 variant) lax mode                                                                   | Same as LINE (DANMARC2 variant) Strict mode                                                                  |
| LINE (MARC21 variant)          | Include leader<br/>Include whitespace padding<br/>Use star (*) subfield marker                             | Include leader<br/>No whitespace padding<br/>Use dollar ($) subfield marker                                  |
| LINE_CONCAT (MARC21 variant)   | Same as LINE (MARC21 variant) Lax mode                                                                     | Same as LINE (MARC21 variant) Strict mode                                                                    |
| MARCXCHANGE                    | No XML declaration                                                                                         | Include XML declaration                                                                                      |
          
The `mconv` tool itself is only guaranteed to be able to read output from `Strict` mode.

## Error reporting

When MARC errors are encountered in the input, and the input format is ISO2709, error details together with
the raw input record is written to the file `mconv.errdump` in the current working directory, otherwise errors
are reported directly to the console. In both cases mconv will return with exit code 1.
