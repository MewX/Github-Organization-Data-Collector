package org.mewx.github.collector.type;

import au.edu.uofa.sei.assignment1.collector.Constants;
import au.edu.uofa.sei.assignment1.collector.db.QueryDb;
import au.edu.uofa.sei.assignment1.collector.type.BaseKeyRequestType;

import java.sql.SQLException;
import java.util.Map;

public class OrganizationList extends BaseKeyRequestType {

    public OrganizationList() {
        super(OrganizationList.class.getSimpleName());
    }

    /**
     * in the request patter, the key is `since` in this case
     * @param page not used in this case
     * @param since since an id, but this is will not be included in the next list (like id > since)
     * @return the constructed params
     */
    @Override
    public String constructParam(int page, String since) {
        final Integer s = Integer.valueOf(since);
        StringBuilder query = new StringBuilder();
        query.append("organizations?per_page=100");
        if (s > 0) {
            // not on the first page
            query.append("&since=").append(since);
        }
        return query.toString();
    }

    @Override
    public String constructRequestUrl(String param) {
        return "https://api.github.com/" + param + Constants.APP_ID_FOR_QUERY;
    }

    /**
     * @param since the last repo id from previous list is passed to this function and the type is string
     * @param prev previous request result mapper
     * @param db the query db
     * @return the current request result mapper
     */
    @Override
    public Map<String, String> collect(String since, Map<String, String> prev, QueryDb db) throws SQLException {
        return makeRequest(0, since, prev, db);
    }

}
