package org.mewx.github.collector;

import au.edu.uofa.sei.assignment1.collector.CollectorCommon;
import au.edu.uofa.sei.assignment1.collector.LightNetwork;
import au.edu.uofa.sei.assignment1.collector.db.Conn;
import au.edu.uofa.sei.assignment1.collector.db.PropertyDb;
import au.edu.uofa.sei.assignment1.collector.db.QueryDb;
import com.google.gson.*;
import org.mewx.github.collector.type.OrganizationList;
import org.mewx.github.collector.util.MailSender;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class collect the github organization data one by one:
 * 1. continue & get the last list (if new list, save progress (since=???) to database),
 * 2. loop through the just-got list, find the saved id and continue from the next.
 * 3. cleaning the found next organization ready to have a clean start, get organization info and save.
 * 4. if the organization is valid, get organization repo list.
 * 5. loop through the repo list, find valid repos.
 * 6. clone the repo, run linguist on each commit (per week, from Monday 8:00 UTC),
 * 7. save to database, delete the repo, save progress to db.
 */
public class Collector extends CollectorCommon {

    public void run() throws SQLException {
        run(Constants.DB_NAME);
    }

    public void run(String dbName) throws SQLException {
        // get connection
        Conn c = new Conn(dbName);
        PropertyDb propertyDb = new PropertyDb(c);
        QueryDb queryDb = new QueryDb(c);
        Map<String, String> prevReq = new HashMap<>();

        // get list one by one first (5000 * 100)
        while (true) {
            String lastOrgListJson = getLastQueriedOrganizationContent(queryDb, propertyDb);
            if (lastOrgListJson == null) {
                // for the first request
                prevReq = new OrganizationList().collect("0", prevReq, queryDb);
                lastOrgListJson = prevReq.get(LightNetwork.HEADER_CONTENT);
            }

            List<JsonObject> orgList = extractOrganization(lastOrgListJson);
            if (orgList.size() != 0) {
                MailSender.send(Constants.MAIL_NAME, Constants.MAIL_SUBJECT, "got empty org list: " + lastOrgListJson);
                System.exit(-2);
            }

            String lastOrgId = propertyDb.get(Constants.PROP_LAST_FINISHED_ORG_ID);
            int idxNextOrgFromTheList = 0; // by default, it's not even started yet
            if (lastOrgId != null) {
                idxNextOrgFromTheList = findIndexInTheList(orgList, lastOrgId);
            }

            // start working on current organization
            while (idxNextOrgFromTheList < orgList.size()) {
                // for each organization
                OrgWorker orgWorker = new OrgWorker(orgList.get(idxNextOrgFromTheList).getAsJsonPrimitive("login").getAsString(), queryDb, propertyDb);
                prevReq = orgWorker.startOrContinue(prevReq);

                // update property record
                propertyDb.put(Constants.PROP_LAST_FINISHED_ORG_ID,
                        orgList.get(idxNextOrgFromTheList).getAsJsonPrimitive("id").getAsString());
                idxNextOrgFromTheList ++;
            }

            // get new list
            prevReq = new OrganizationList().collect(orgList.get(orgList.size() - 1).getAsJsonPrimitive("id").getAsString(), prevReq, queryDb);

            // exit condition
            ResultSet resultSet = queryDb.select(new OrganizationList().TYPE);
            if (resultSet.last() && resultSet.getRow() > Constants.NUMBER_OF_ORG_PAGES) break;
        }

        c.close();
    }

    private String getLastQueriedOrganizationContent(QueryDb queryDb, PropertyDb propertyDb) throws SQLException {
        ResultSet set = queryDb.select(new OrganizationList().TYPE);
        return set.last() ? set.getString("content") : null;
    }

    private List<JsonObject> extractOrganization(String content) {
        List<JsonObject> ret = new ArrayList<>();
        try {
            Gson gson = new GsonBuilder().create();
            for (JsonElement ele : gson.fromJson(content, JsonArray.class)) {
                ret.add(ele.getAsJsonObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("JSON format error in content: " + content);
        }
        return ret;
    }

    private int findIndexInTheList(List<JsonObject> jsonList, String id) {
        if (id == null) return 0;

        int i = 0;
        for (; i < jsonList.size(); i++) {
            JsonObject obj = jsonList.get(i);
            if (obj.getAsJsonPrimitive("id").getAsString().equals(id)) {
                return i;
            }
        }

        if (i == jsonList.size()) return -1; // finished this list
        else return 0; // not in this list, so guessing it's not started yet
    }
}
