package org.mewx.github.collector.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class is used to run an external application and get the output result
 */
public class CmdExecutor {
    private Process cmdProcess;
    private InputStream inputStream; // from command line to screen
    private InputStream errorStream; // from command line to screen
    private OutputStream outputStream; // from keyboard to command line
    private String fullInput, fullError;

    public CmdExecutor() { }

    public CmdExecutor(String cmd) {
        execute(cmd);
    }

    /**
     * Execute an external command
     * @param cmd command line
     * @return false if fails, otherwise true
     */
    public boolean execute(String cmd) {
        System.out.println("Executing: " + cmd);
        try {
            if (cmdProcess != null)
                cmdProcess.destroy();
            cmdProcess = Runtime.getRuntime().exec(cmd);
            outputStream = cmdProcess.getOutputStream();
            errorStream = cmdProcess.getErrorStream();
            inputStream = cmdProcess.getInputStream();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Get full output of a command line application.
     * Note: should be carefully use if the application does not finish execution immediately.
     *
     * TODO: put it in a separate thread: http://stackoverflow.com/questions/17038324/cannot-get-the-getinputstream-from-runtime-getruntime-exec
     *
     * @param force true to force re-read command output
     * @return the full contents.
     */
    public String getFullInput(boolean force) {
        if (fullInput != null && fullInput.length() > 0 && !force) return fullInput;
        fullInput = getFullTextFromInputStream(inputStream);
        return fullInput;
    }

    public String getFullError() {
        if (fullError != null && fullError.length() > 0) return fullError;
        fullError = getFullTextFromInputStream(errorStream);
        return fullError;
    }

    private String getFullTextFromInputStream(InputStream is) {
        StringBuilder sb = new StringBuilder();
        try {
            Thread.sleep(2000);
            while (is.available() > 0)
                sb.append((char) is.read());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
