package org.mewx.github.collector;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        // https://api.github.com/organizations?per_page=100&client_id=Iv1.27ed2f902acf887a&client_secret=d162f756007a1977a05da6fc50efa85fb15da326
        StringBuffer dbName = new StringBuffer();
        LongOpt[] longOpts = new LongOpt[] {
                new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
                new LongOpt("collect", LongOpt.OPTIONAL_ARGUMENT, dbName, 'c'),
                new LongOpt("parse", LongOpt.OPTIONAL_ARGUMENT, dbName, 'p'),
                new LongOpt("analyse", LongOpt.OPTIONAL_ARGUMENT, dbName, 'a')
        };

        Getopt g = new Getopt("java -jar this.jar", args, "c::p::a::h", longOpts); // e.g. ... -crepo.db
        g.setOpterr(false);
        int c;
        String arg;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h':
                    System.err.println("java -jar this.jar -<c|p|a>DB.NAME | <h>");
                    break;
                case 'c':
                    arg = g.getOptarg();
                    System.err.println("collecting: " + (arg == null ? "null" : arg));
                    new Collector().run(arg);
                    break;
                case 'p':
                    arg = g.getOptarg();
                    System.err.println("parse: " + (arg == null ? "null" : arg));
                    break;
                case 'a':
                    arg = g.getOptarg();
                    System.err.println("analyse: " + (arg == null ? "null" : arg));
                    break;

                case ':':
                    System.out.println("You need an argument for option " + (char)g.getOptopt());
                    break;
                case '?':
                    System.out.println("The option '" + (char)g.getOptopt() + "' is not valid");
                    break;
                default:
                    System.out.println("getopt() returned " + c);
                    break;
            }
        }

    }
}
