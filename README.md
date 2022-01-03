# mconv
mconv is a commandline-tool to read in and parse MARC records from file and
output in either MARC21 or DANMARC2 line-format or ISO2709.

## installation

```bash
$ curl -sL http://mavenrepo.dbc.dk/content/repositories/releases/dk/dbc/mconv/1.1/mconv-1.1.jar -o mconv.jar && unzip -op mconv.jar mconv | bash -s -- --install
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
usage: mconv [-h] [-f {LINE,LINE_CONCAT,ISO,JSONL}] [-i INPUT_ENCODING] [-o OUTPUT_ENCODING] [-l] [-p] IN

Reads in and parses MARC records from file
and supports output in both MARC21 or DANMARC2 line-format and ISO2709

positional arguments:
  IN                     Input file or standard input if given as a dash (-)

optional arguments:
  -h, --help             show this help message and exit
  -f {LINE,LINE_CONCAT,ISO,JSONL}, --format {LINE,LINE_CONCAT,ISO,JSONL}
                         Output format.
                         Defaults to LINE_CONCAT
  -i INPUT_ENCODING, --input-encoding INPUT_ENCODING
                         Character set of the input MARC record(s)
                         eg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.
                         Defaults to UTF-8.
  -o OUTPUT_ENCODING, --output-encoding OUTPUT_ENCODING
                         Character set of the output MARC record(s)
                         eg. LATIN-1, DANMARC2, MARC-8, UTF-8, and more.
                         Defaults to UTF-8.
  -l, --include-leader   Include leader in line format output (MARC21 only).
                         Defaults to false.
  -p, --include-whitespace-padding
                         Pad subfields with whitespace in line format output (MARC21 only).
                         Defaults to false.
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
