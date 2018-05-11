package org.mewx.github.collector;

import au.edu.uofa.sei.assignment1.collector.LightNetwork;
import au.edu.uofa.sei.assignment1.collector.db.Conn;
import au.edu.uofa.sei.assignment1.collector.db.PropertyDb;
import au.edu.uofa.sei.assignment1.collector.db.QueryDb;
import com.google.gson.*;
import org.mewx.github.collector.type.OrgDetail;
import org.mewx.github.collector.type.OrgRepo;
import org.mewx.github.collector.util.ExceptionHelper;
import org.mewx.github.collector.util.MailSender;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class deal with a specified organization
 */
public class OrgWorker {
    public enum ORG_TYPE {
        ORGANIZATION,
        COMOPANY
    }

    private static SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'");
    private final Conn conn;
    private final String ORG_NAME;
    private final QueryDb QUERY_DB;
    private final PropertyDb PROPERTY_DB;

    public OrgWorker(Conn c, String orgName, QueryDb q, PropertyDb p) {
        conn = c;
        ORG_NAME = orgName;
        QUERY_DB = q;
        PROPERTY_DB = p;
    }

    /**
     * using saved system property, find previous progress and continue.
     * this function also detects whether the organization account is valid or not.
     */
    public Map<String, String> startOrContinue(Map<String, String> prev) throws SQLException {
        // check account is company or organization based on `blog` url
        prev = new OrgDetail().collect(ORG_NAME, prev, QUERY_DB);
        ORG_TYPE orgType = getOrgType(prev.get(LightNetwork.HEADER_CONTENT));
        if (orgType == null) {
            // unwanted organization
            System.err.println("Current organization does not provide the website: " + ORG_NAME);
            return prev;
        }

        // collect all repo lists
        String lastRepoName = PROPERTY_DB.get(Constants.PROP_LAST_FINISHED_REPO_NAME);
        prev = new OrgRepo().collect(ORG_NAME, prev, QUERY_DB);
        Map<String, String> repoNames = getRemainingValidRepo(lastRepoName); // checker part 1

        // checker part 2
        if (repoNames.size() < Constants.MIN_NUMBER_OF_VALID_REPOS) {
            System.err.format("Not meeting the min valid repo requirement: (%d/%d) valid for organization %s\n",
                    repoNames.size(), Constants.MIN_NUMBER_OF_VALID_REPOS, ORG_NAME);
            return prev;
        }

        // for each repo
        for (String repoName : repoNames.keySet()) {
            try {
                RepoWorker repoWorker = new RepoWorker(conn, ORG_NAME, repoName, repoNames.get(repoName), QUERY_DB, PROPERTY_DB);
                repoWorker.run(orgType);

            } catch (Exception e) {
                e.printStackTrace();
                MailSender.send("Exception: " + ExceptionHelper.toString(e) + "\n when dealing with repo: " + repoName);
            }
        }

        return prev;
    }

    private Map<String, String> getRemainingValidRepo(String lastRepoName) throws SQLException {
        boolean foundLastRepoName = lastRepoName == null || lastRepoName.length()  == 0; // unset last repo name, true

        Map<String, String> repoNames = new HashMap<>();
        ResultSet rs = QUERY_DB.select(new OrgRepo().TYPE);
        while (rs.next()) {
            String json = rs.getString("content");

            // use gson
            Gson gson = new GsonBuilder().create();
            try {
                for (JsonElement ele : gson.fromJson(json, JsonArray.class)) {
                    JsonObject repo = ele.getAsJsonObject();
                    String currentRepoName = repo.getAsJsonPrimitive("full_name").getAsString();
                    String currentRepoGitUrl = repo.getAsJsonPrimitive("git_url").getAsString();

                    // find the last one
                    if (!foundLastRepoName) {
                        if (currentRepoName.equals(lastRepoName)) foundLastRepoName = true;
                        // skip the last one as well
                        break;
                    }

                    // only valid repo after the last one
                    if (validRepo(repo)) repoNames.put(currentRepoName, currentRepoGitUrl);
                }
            } catch (Exception e) {
                e.printStackTrace();
                MailSender.send("Exception: " + ExceptionHelper.toString(e) + "\nBad json content: " + json);
            }
        }
        return repoNames;
    }

    /**
     * parse the orgrepo list element object
     */
    private boolean validRepo(JsonObject object) {
        try {
            // test date range
            Calendar createDate = Calendar.getInstance();
            createDate.setTime(TIME_FORMATTER.parse(object.getAsJsonPrimitive("created_at").getAsString()));

            Calendar updateDate = Calendar.getInstance();
            updateDate.setTime(TIME_FORMATTER.parse(object.getAsJsonPrimitive("updated_at").getAsString()));

            createDate.add(Calendar.DATE, Constants.DAYS_BETWEEN_FIRST_AND_LAST_COMMIT);
            if (createDate.after(updateDate)) return false;

            // test number of stars
            int noStars = object.getAsJsonPrimitive("stargazers_count").getAsInt();
            if (noStars < Constants.MIN_NUMBER_OF_STARS) return false;
        } catch (ParseException e) {
            e.printStackTrace();
            MailSender.send("Exception:" + ExceptionHelper.toString(e) + "\nDon't know why the data cannot be found: " + object.toString());
            return false;
        }
        return true;
    }

    /**
     * get the organization type of the working on organization
     * @param orgDetailJson the input json fetched from org detail request
     * @return nullable ORG_TYPE
     */
    private ORG_TYPE getOrgType(String orgDetailJson) {
        if (orgDetailJson == null || orgDetailJson.length() < 2) return null;

        // org detail string
        Gson gson = new GsonBuilder().create();
        try {
            JsonObject obj = gson.fromJson(orgDetailJson, JsonObject.class);
            String blog = null;
            if (obj.has("blog"))
                blog = obj.getAsJsonPrimitive("blog").getAsString().trim();

            if (blog == null || blog.length() == 0) {
                return null; // not provided
            } else if (blog.contains(".org")) {
                return ORG_TYPE.ORGANIZATION;
            } else {
                return ORG_TYPE.COMOPANY; // provided and allowed custom domain name
            }
        } catch (Exception e) {
            e.printStackTrace();
            MailSender.send("Exception: " + ExceptionHelper.toString(e) + "\nBad json content: " + orgDetailJson);
            return null;
        }
    }

}
