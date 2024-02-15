package dk.dbc.marc;

import dk.dbc.marc.reader.MarcReaderException;
import picocli.CommandLine;

public class ExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {
    @Override
    public int handleExecutionException(Exception e, CommandLine commandLine, CommandLine.ParseResult parseResult) {

        String errorMessage = e.getMessage();
        if (e instanceof IllegalArgumentException) {
            // Attempt to unwrap real error message
            final Throwable cause = e.getCause();
            if (cause instanceof MarcReaderException) {
                errorMessage = cause.getCause().getMessage();
            }
        }
        if (errorMessage == null) {
            errorMessage = "Unexpected error";
        }

        // bold red error message
        commandLine.getErr().println(commandLine.getColorScheme().errorText(errorMessage));
        e.printStackTrace();

        return commandLine.getCommandSpec().exitCodeOnExecutionException();
    }
}
