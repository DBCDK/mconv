# mconv
mconv is a commandline-tool to read in and parse MARC records from file and
output in either MARCXCHANGE, MARC21, DANMARC2 line-format, JSONL or ISO2709.

## installation

```bash
$ curl -sL http://mavenrepo.dbc.dk/content/repositories/releases/dk/dbc/mconv/1.3/mconv-1.3.jar -o mconv.jar && unzip -op mconv.jar mconv | bash -s -- --install
```

Keep the installation up-to-date using the selfupdate action
```bash
mconv --selfupdate
```

## usage
```bash
$ mconv -h
usage: mconv --version
usage: mconv --selfupdate
Usage: mconv  [-chlpV] [-f=<outputFormat>] [-i=<inputEncoding>]
              [-o=<outputEncoding>] inputfile
Reads in and parses MARC records from file
and supports output in both MARC21 or DANMARC2 line-format and ISO2709
      inputfile          Input file or standard input if given as a dash (-)
  -c, --as-collection    Output all input records in the same collection.
                           Requires that the output format has support for
                           collections.
                         Defaults to false.
  -f, --format=<outputFormat>
                         Output format LINE, LINE_CONCAT, MARCXCHANGE, ISO,
                           JSONL
                         defaults to LINE.
  -h, --help             Show this help message and exit.
  -i, --input-encoding=<inputEncoding>
                         Character set of the input MARC record(s)
                         eg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.
                         Defaults to UTF-8.
  -l, --include-leader   Include leader in line format output (MARC21 only).
                         Defaults to false.
  -o, --output-encoding=<outputEncoding>
                         Character set of the output MARC record(s)
                         eg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.
                         Defaults to UTF-8.
  -p, --include-whitespace-padding
                         Pad subfields with whitespace in line format output
                           (MARC21 only).
                         Defaults to false.
  -V, --version          Print version information and exit.

```

```bash
$ mconv marc_collection.xml
```

```bash
$ cat marc_collection.xml | mconv -
```

## output format

* LINE - line format DANMARC2 or MARC21 variant
* LINE_CONCAT - paste-into-code friendly DANMARC2 or MARC21 line format (default)
* ISO - ISO2709
* JSONL - MARC JSON in newline delimited json format
* MARCXCHANGE - MarcXchange XML
