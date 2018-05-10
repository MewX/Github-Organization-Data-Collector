package org.mewx.github.collector;

import au.edu.uofa.sei.assignment1.collector.db.PropertyDb;
import au.edu.uofa.sei.assignment1.collector.db.QueryDb;
import com.google.gson.*;
import org.mewx.github.collector.type.OrgRepo;
import org.mewx.github.collector.util.MailSender;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class deal with a specified organization
 */
public class OrgWorker {
    private static SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'");
    private final String ORG_NAME;
    private final QueryDb QUERY_DB;
    private final PropertyDb PROPERTY_DB;

    public OrgWorker(String orgName, QueryDb q, PropertyDb p) {
        ORG_NAME = orgName;
        QUERY_DB = q;
        PROPERTY_DB = p;
    }

    /**
     * using saved system property, find previous progress and continue.
     * this function also detects whether the organization account is valid or not.
     */
    public Map<String, String> startOrContinue(Map<String, String> prev) throws SQLException {
        // TODO: check account is company or organization

        // collect all repo lists
        prev = new OrgRepo().collect(ORG_NAME, prev, QUERY_DB);
        List<String> repoNames = getValidRepo(); // checker part 1

        // checker part 2
        if (repoNames.size() < Constants.MIN_NUMBER_OF_VALID_REPOS) {
            System.err.format("Not meeting the min valid repo requirement: (%d/%d) valid for organization %s\n",
                    repoNames.size(), Constants.MIN_NUMBER_OF_VALID_REPOS, ORG_NAME);
            return prev;
        }

        String lastRepoName = PROPERTY_DB.get(Constants.PROP_LAST_FINISHED_REPO_NAME);
        if (lastRepoName == null) {
            // TODO:
        }

        return prev;
    }

    private List<String> getValidRepo() throws SQLException {
        List<String> repoNames = new ArrayList<>();
        ResultSet rs = QUERY_DB.select(new OrgRepo().TYPE);
        while (rs.next()) {
            String json = rs.getString("content");

            // use gson
            Gson gson = new GsonBuilder().create();
            for (JsonElement ele : gson.fromJson(json, JsonArray.class)) {
                JsonObject repo = ele.getAsJsonObject();
                if (validRepo(repo)) repoNames.add(repo.getAsJsonPrimitive("full_name").getAsString());
            }
        }
        return repoNames;
    }

    /**
     * parse the orgrepo list element object
     */
    private boolean validRepo(JsonObject object) {
        // test date range
        try {
            Calendar createDate = Calendar.getInstance();
            createDate.setTime(TIME_FORMATTER.parse(object.getAsJsonPrimitive("created_at").getAsString()));

            Calendar updateDate = Calendar.getInstance();
            updateDate.setTime(TIME_FORMATTER.parse(object.getAsJsonPrimitive("updated_at").getAsString()));

            createDate.add(Calendar.DATE, Constants.DAYS_BETWEEN_FIRST_AND_LAST_COMMIT);
            if (createDate.after(updateDate)) return false;
        } catch (ParseException e) {
            e.printStackTrace();
            MailSender.send("Don't know why the data cannot be found: " + object.toString());
            return false;
        }

        // test number of commits
        // TODO: collect repo detail data first


        return true;
    }

}
