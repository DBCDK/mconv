# mconv
mconv is a commandline-tool to convert marcXchange records into unit test
friendly line formats. Currently only UTF-8 encoded output is supported.

# installation
 
```bash
$ curl -sL http://mavenrepo.dbc.dk/content/repositories/releases/dk/dbc/mconv/1.0.1/mconv-1.0.1.jar -o mconv.jar && unzip -op mconv.jar mconv | bash -s -- --install
```

Keep the installation up-to-date using the selfupdate action
```bash
mconv --selfupdate
```

# usage 
```bash
$ mconv -h
usage: mconv --version
usage: mconv --selfupdate
usage: mconv [-h] [-f {LINE,LINE_CONCAT}] IN

Reads in marcXchange records and outputs them as line format variant

positional arguments:
  IN                     Input file or standard input if given as a dash (-)

optional arguments:
  -h, --help             show this help message and exit
  -f {LINE,LINE_CONCAT}, --format {LINE,LINE_CONCAT}
                         Output format
```

```bash
$ mconv marc_collection.xml
```

```bash
$ cat marc_collection.xml | mconv -
```

# output format

* LINE - standard danmarc2 line format
* LINE_CONCAT - paste into code friendly danmarc2 line format (default)