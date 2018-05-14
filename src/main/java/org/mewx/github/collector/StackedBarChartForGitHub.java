package org.mewx.github.collector;

import au.edu.uofa.sei.assignment1.collector.CollectorCommon;
import au.edu.uofa.sei.assignment1.collector.db.Conn;
import org.mewx.github.collector.model.ProjectTechStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * this class generates the dataset required d3.js to draw the GitHub organization tech stacks overtime
 */
public class StackedBarChartForGitHub extends CollectorCommon {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
    private static String ALL_NAME = "[ALL]";

    public void run(String DB_NAME) throws Exception {
        final Conn conn = new Conn(DB_NAME);
        TreeMap<Long, SortedMap<String, ProjectTechStack>> graph = new TreeMap<>();

        final String SELECT = "SELECT * FROM commits WHERE project like ? ORDER BY id;"; // make sure it's in order
        PreparedStatement select = conn.getConn().prepareStatement(SELECT);
        select.setString(1, "thoughtbot/%");
        //select.setString(1, "rails/%");
        //select.setString(1, "github/%");
        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            final String projectName = rs.getString("project");
            final Long timestamp = rs.getTimestamp("time").getTime();
            final String message = rs.getString("message");
            ProjectTechStack projectTechStack = new ProjectTechStack(
                    projectName.substring(projectName.indexOf('/') + 1),
                    message.substring(message.lastIndexOf('|') + 1)
            );

            // add all records to the big graph
            if (graph.containsKey(timestamp)) {
                graph.get(timestamp).put(projectName, projectTechStack);
            } else {
                SortedMap<String, ProjectTechStack> temp = new TreeMap<>();
                temp.put(projectName, projectTechStack);
                graph.put(timestamp, temp);
            }
        }

        // project carries previous state to a new month
        List<Long> timestamps = new ArrayList<>(graph.keySet());
        for (int i = 1; i < timestamps.size(); i++) {
            // add missing projects from [i - 1] to [i]
            SortedMap<String, ProjectTechStack> prev = graph.get(timestamps.get(i - 1));
            for (String projectName : prev.keySet()) {
                SortedMap<String, ProjectTechStack> temp = graph.get(timestamps.get(i));
                if (!temp.containsKey(projectName)) {
                    // add previous project to current one
                    ProjectTechStack p = new ProjectTechStack(projectName, "");
                    p.add(prev.get(projectName));
                    temp.put(projectName, p); // hard copy
                }
            }
        }

        // merge all lists into one element
        for (Long time : graph.keySet()) {
            SortedMap<String, ProjectTechStack> map = graph.get(time);
            ProjectTechStack projectTechStack = new ProjectTechStack(ALL_NAME, "");
            for (String projectName : map.keySet()) {
                projectTechStack.add(map.get(projectName));
            }
            map.clear();
            map.put(ALL_NAME, projectTechStack);
        }

        // get the max set of languages
        SortedSet<String> maxLanguageSet = new TreeSet<>();
        for (Long time : graph.keySet()) {
            for (String projectName : graph.get(time).keySet()) {
                ProjectTechStack projectTechStack = graph.get(time).get(projectName);
                maxLanguageSet.addAll(projectTechStack.languages.keySet());
            }
        }

        // unify all time slots
        for (Long time : graph.keySet()) {
            for (String projectName : graph.get(time).keySet()) {
                ProjectTechStack projectTechStack = graph.get(time).get(projectName);
                for (String lang : maxLanguageSet) {
                    if (!projectTechStack.languages.containsKey(lang)) {
                        projectTechStack.languages.put(lang, 0.0);
                    }
                }
            }
        }

        // TODO: sqrt(*)

        // output the table
        System.out.print("Date");
        for (String lang : maxLanguageSet) {
            System.out.print("," + lang);
        }
        System.out.println();

        // TODO: may add empty times by getting intervals
        for (Long time : graph.keySet()) {
            String date = sdf.format(new Date(time));
            System.out.print(date);

            SortedMap<String, Double> languages = graph.get(time).get(ALL_NAME).languages;
            for (String lang : languages.keySet()) {
                    System.out.format(",%.4f", languages.get(lang));
            }
            System.out.println();
        }

        // conn.close(); // in auto commit mode
    }
}
