package org.mewx.github.collector;

import au.edu.uofa.sei.assignment1.collector.db.CommitDb;
import au.edu.uofa.sei.assignment1.collector.db.Conn;
import au.edu.uofa.sei.assignment1.collector.db.PropertyDb;
import au.edu.uofa.sei.assignment1.collector.db.QueryDb;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * This class works for a specific repo
 */
public class RepoWorker {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private final String ORG_NAME, REPO_NAME, REPO_URL;
    private final Conn conn;
    private final QueryDb QUERY_DB;
    private final PropertyDb PROPERTY_DB;

    public RepoWorker(Conn c, String orgName, String repoName, String repoUrl, QueryDb q, PropertyDb p) {
        ORG_NAME = orgName;
        REPO_NAME = repoName;
        REPO_URL = repoUrl;
        conn = c;
        QUERY_DB = q;
        PROPERTY_DB = p;
    }

    /**
     * Reset a repo (delete, re-clone, loop through each commit weekly)
     */
    private void resetRepo() {
        // P.S. no need to clear database related records because the auto-commit is switched off in this process
        // delete local files
        deleteFolder(new File(getLocalFullPathToProject()));
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    private void cloneRepo() throws GitAPIException {
        new File(getLocalBasePath()).mkdirs();
        Git result = Git.cloneRepository()
                .setURI(REPO_URL)
                .setDirectory(new File(getLocalFullPathToProject()))
                .call();
        result.close();
    }

    private String getLocalFullPathToProject() {
        return getLocalFullPathToProject(false);
    }

    private String getLocalFullPathToProject(boolean toGit) {
        return getLocalBasePath() + REPO_NAME + "/" + (toGit ? ".git/" : "");
    }

    private String getLocalBasePath() {
        return Constants.LOCAL_REPO_BASE_DIR;
    }

    private Git openExistingRepo(String pathToDotGit) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        org.eclipse.jgit.lib.Repository repository = builder
                .setGitDir(new File(pathToDotGit))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
        return new Git(repository);
    }

    private void detachBranch(String hash) throws IOException, GitAPIException {
        Git git = openExistingRepo(getLocalFullPathToProject(true));
        git.checkout()
                .setCreateBranch(false)
                .setName(hash)
                .call();
        git.close();
    }

    private void reattachMasterBranch() throws IOException, GitAPIException {
        Git git = openExistingRepo(getLocalFullPathToProject(true));
        String branchName = null;
        for (Ref b : git.branchList().call()) {
            System.err.println("found branch: " + b.getName());
            if (b.getName().contains("refs/heads")) {
                branchName = b.getName();
                break;
            }
        }
        git.checkout()
                .setCreateBranch(false)
                .setName(branchName) // use the first default branch name
                .call();
        git.close();
    }
    private List<CommitDb.Commit> getAllCommits() throws IOException, GitAPIException, SQLException {
        final String gitPath = getLocalFullPathToProject(true);
        System.err.println("Working on " + gitPath);
        Git git = openExistingRepo(gitPath);

        List<CommitDb.Commit> commits = new ArrayList<>();
        for (RevCommit commit : git.log().all().call()) {
            final PersonIdent authorId = commit.getAuthorIdent();
            // P.S. getName() returns the commit hash
            commits.add(new CommitDb.Commit(REPO_NAME, new Timestamp(authorId.getWhen().getTime()), commit.getName(), 0, authorId.getEmailAddress()));
        }


        return commits;
    }

    private List<CommitDb.Commit> filterWantedCommits(List<CommitDb.Commit> fullCommits) {
        List<CommitDb.Commit> filteredCommits = new ArrayList<>();

        // sort ascending
        fullCommits.sort(Comparator.comparing(a -> a.time));

        Calendar baseCalendar = Calendar.getInstance();
        baseCalendar.setTimeInMillis(fullCommits.get(0).time.getTime());
        baseCalendar.set(Calendar.HOUR_OF_DAY, 0);
        baseCalendar.set(Calendar.MINUTE, 0);
        baseCalendar.set(Calendar.SECOND, 0);
        while (baseCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) baseCalendar.add(Calendar.DATE, -1);

        Calendar tempCalendar = Calendar.getInstance();
        for (int i = 0; i < fullCommits.size(); i++) {
            CommitDb.Commit commit = fullCommits.get(i);
            final Date date = new Date(commit.time.getTime());
            tempCalendar.setTime(date);

            if (!baseCalendar.after(tempCalendar)) {
                // commit date is equal to or after base date
                baseCalendar.add(Calendar.DATE, 28); // add 4 weeks
                if (baseCalendar.after(tempCalendar)) {
                    // good, this is what I want, and I will use this commit
                    System.err.println("Selected commit: " + commit.msg + " at " + DATE_FORMAT.format(date));

                    // save to the later list
                    filteredCommits.add(commit);
                } else {
                    // it's been more than `interval` days no new commit
                    i --;
                }
            }
        }

        // number all of them
        for (int i = 0; i < filteredCommits.size(); i ++) {
            filteredCommits.get(i).commitId = i + 1;
        }
        return filteredCommits;
    }

    public void run(String blogUrl) throws GitAPIException, SQLException, IOException, TimeoutException, InterruptedException {
        // reset repo and clone the repo and run the analyser from scratch
        resetRepo();
        cloneRepo();

        // switch off the auto committer
        CommitDb commitDb = new CommitDb(conn);
        conn.getConn().setAutoCommit(false);

        // get all commits from the repo
        List<CommitDb.Commit> commits = filterWantedCommits(getAllCommits());
        for (CommitDb.Commit commit : commits) {
            reattachMasterBranch();
            detachBranch(commit.msg);
            System.err.println("working on commit - " + commit.commitId);

            // run the linguist command and get output
            System.err.println("Executing linguist on: " + getLocalFullPathToProject());
            String linguistOutput = new ProcessExecutor().command("linguist").readOutput(true)
                    .directory(new File(getLocalFullPathToProject()).getAbsoluteFile())
                    .execute().outputUTF8();

            // set message: original hash | blogUrl | raw linguist output
            /*
            # linguist
            98.36%  Python
            0.78%   C
            0.57%   Shell
            0.13%   C++
            0.13%   Perl
            0.03%   PLpgSQL
             */
            commit.msg = String.format("%s | %s | %s", commit.msg, blogUrl, linguistOutput);
            System.err.println("commit msg: " + commit.msg);

            commitDb.insert(commit);
        }

        // switch on the auto committer again
        conn.getConn().commit();
        conn.getConn().setAutoCommit(true);

        // delete the folder
        deleteFolder(new File(getLocalFullPathToProject()));
    }




}
