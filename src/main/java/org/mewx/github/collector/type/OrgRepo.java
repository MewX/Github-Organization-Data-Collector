package org.mewx.github.collector.type;

import au.edu.uofa.sei.assignment1.collector.Constants;
import au.edu.uofa.sei.assignment1.collector.db.Conn;
import au.edu.uofa.sei.assignment1.collector.type.BaseKeyRequestType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrgRepo extends BaseKeyRequestType {

    public OrgRepo() {
        super(OrgRepo.class.getSimpleName());
    }

    @Override
    public String constructParam(int page, String orgName) {
        StringBuilder query = new StringBuilder(); // the query without APP ID
        query.append(orgName).append("/repos?per_page=100");
        if (page > 1) {
            // not first page
            query.append("&page=").append(page);
        }
        return query.toString();
    }

    @Override
    public String constructRequestUrl(String param) {
        return "https://api.github.com/orgs/" + param + Constants.APP_ID_FOR_QUERY;
    }

    public ResultSet selectOrgRepos(Conn conn, String paramsLike) throws SQLException {
        final String SELECT = "SELECT * FROM queries WHERE type = ? and params like ? ORDER BY id;"; // make sure it's in order
        PreparedStatement select = conn.getConn().prepareStatement(SELECT);
        select.setString(1, TYPE);
        select.setString(2, paramsLike);
        return select.executeQuery();
    }
}
