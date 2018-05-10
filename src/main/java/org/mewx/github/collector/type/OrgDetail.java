package org.mewx.github.collector.type;

import au.edu.uofa.sei.assignment1.collector.Constants;
import au.edu.uofa.sei.assignment1.collector.db.QueryDb;
import au.edu.uofa.sei.assignment1.collector.type.BaseKeyRequestType;

import java.sql.SQLException;
import java.util.Map;

/**
 * {
 *  "login": "engineyard",
 *  "id": 81,
 *  "url": "https://api.github.com/orgs/engineyard",
 *  "repos_url": "https://api.github.com/orgs/engineyard/repos",
 *  "events_url": "https://api.github.com/orgs/engineyard/events",
 *  "hooks_url": "https://api.github.com/orgs/engineyard/hooks",
 *  "issues_url": "https://api.github.com/orgs/engineyard/issues",
 *  "members_url": "https://api.github.com/orgs/engineyard/members{/member}",
 *  "public_members_url": "https://api.github.com/orgs/engineyard/public_members{/member}",
 *  "avatar_url": "https://avatars1.githubusercontent.com/u/81?v=4",
 *  "description": "",
 *  "name": "Engine Yard, Inc.",
 *  "company": null,
 *  "blog": "https://www.engineyard.com",
 *  "location": "San Francisco, CA",
 *  "email": "",
 *  "has_organization_projects": true,
 *  "has_repository_projects": true,
 *  "public_repos": 283,
 *  "public_gists": 25,
 *  "followers": 0,
 *  "following": 0,
 *  "html_url": "https://github.com/engineyard",
 *  "created_at": "2008-01-29T09:51:30Z",
 *  "updated_at": "2017-11-15T20:24:07Z",
 *  "type": "Organization"
 * }
 */
public class OrgDetail extends BaseKeyRequestType {

    public OrgDetail() {
        super(OrgDetail.class.getSimpleName());
    }

    @Override
    public String constructParam(int page, String orgName) {
        return "orgs/" + orgName;
    }

    @Override
    public String constructRequestUrl(String param) {
        return "https://api.github.com/" + param + "?" + Constants.APP_ID_FOR_QUERY;
    }

    @Override
    public Map<String, String> collect(String orgName, Map<String, String> prev, QueryDb db) throws SQLException {
        return makeRequest(0, orgName, prev, db);
    }

}
