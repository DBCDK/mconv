package dk.dbc.marc;

import dk.dbc.marc.reader.MarcReader;

import java.io.PushbackInputStream;
import java.nio.charset.Charset;

public class MsgPackReader implements MarcReader {
    public MsgPackReader(PushbackInputStream is, Charset encoding) {
    }
}
