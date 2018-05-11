package org.mewx.github.collector.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHelper {
    public static String toString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        // FIXME - exception not full
        e.printStackTrace(pw);
        return pw.toString();
    }
}
