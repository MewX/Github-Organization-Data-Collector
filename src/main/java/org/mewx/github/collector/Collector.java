package org.mewx.github.collector;

import au.edu.uofa.sei.assignment1.collector.CollectorCommon;
import au.edu.uofa.sei.assignment1.collector.db.Conn;
import au.edu.uofa.sei.assignment1.collector.db.PropertyDb;

import java.sql.SQLException;

/**
 * This class collect the github organization data one by one:
 * 1. continue & get the last list (if new list, save progress (since=???) to database),
 * 2. loop through the just-got list, find the saved id and continue from the next.
 * 3. cleaning the found next organization ready to have a clean start, get organization info and save.
 * 4. if the organization is valid, get organization repo list.
 * 5. loop through the repo list, find valid repos.
 * 6. clone the repo, run linguist on each commit, save to database, delete the repo, save progress to db.
 */
public class Collector extends CollectorCommon {
    public void run() {
        // get connection
        Conn c = null;
        PropertyDb propertyDb = null;

        // try connecting
        try {
            c = new Conn(Constants.DB_NAME);
            propertyDb = new PropertyDb(c);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }


        // TODO: get list one by one first (5000 * 100)



        try {
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
