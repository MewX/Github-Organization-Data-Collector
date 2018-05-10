package org.mewx.github.collector;

public class Constants {
    // use your app information
    public static final String GITHUB_CLIENT_ID = "Iv1.27ed2f902acf887a";
    public static final String GITHUB_CLIENT_SECRET = "d162f756007a1977a05da6fc50efa85fb15da326";

    // local info
    public static final String MAIL_NAME = "xiayuanzhong@gmail.com";
    public static final String MAIL_SUBJECT = "Job running failed";
    public static final String DB_NAME = "github-org.db";
    public static final int NUMBER_OF_ORG_PAGES = 10; // each page contains 100 pages
    public static final int MIN_NUMBER_OF_VALID_REPOS = 5;
    public static final int DAYS_BETWEEN_FIRST_AND_LAST_COMMIT = 30 * 6; // 6 months

    public static final String PROP_LAST_FINISHED_ORG_ID = "LAST_FINISHED_ORG_ID"; // the last finished organization id
    public static final String PROP_LAST_FINISHED_REPO_NAME = "LAST_FINISHED_REPO_NAME"; // the last finished repo id
}
