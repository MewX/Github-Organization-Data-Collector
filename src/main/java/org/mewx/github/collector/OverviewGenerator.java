package org.mewx.github.collector;

import au.edu.uofa.sei.assignment1.collector.CollectorCommon;
import au.edu.uofa.sei.assignment1.collector.db.Conn;
import org.mewx.github.collector.model.ProjectTechStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * The class used to generate summary data for result overview.
 */
public class OverviewGenerator extends CollectorCommon {

    public void run(String DB_NAME) throws Exception {
        final Conn conn = new Conn(DB_NAME);
        final Map<String, Set<String>> companyTechStackMap = new HashMap<>();

        // define the temp variables
        String lastCompanyName = "";
        Long lastTime2016 = 0L, lastTime2017 = 0L;
        Integer noLang2016 = 0, noLang2017 = 0;

        System.out.println("Company improvement:");
        System.out.println("Org Name\tDiversity 2016\tDiversity 2017");
        final String SELECT = "SELECT * FROM commits ORDER BY id;"; // make sure it's in order
        PreparedStatement select = conn.getConn().prepareStatement(SELECT);
        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            final String projectName = rs.getString("project");
            final Long timestamp = rs.getTimestamp("time").getTime();
            final String message = rs.getString("message");
            ProjectTechStack projectTechStack = new ProjectTechStack(
                    projectName.substring(projectName.indexOf('/') + 1),
                    message.substring(message.lastIndexOf('|') + 1)
            );

            final String companyName = projectName.substring(0, projectName.indexOf('/'));
            if (!lastCompanyName.equalsIgnoreCase(companyName)) {
                if (noLang2017 != 0) {
                    System.out.format("%s\t%d\t%d\n", companyName, noLang2016, noLang2017);
                }

                lastTime2016 = Long.MIN_VALUE;
                lastTime2017 = Long.MIN_VALUE;
                noLang2016 = 0;
                noLang2017 = 0;
            }
            lastCompanyName = companyName; // update last company name
            if (companyTechStackMap.containsKey(companyName)) {
                companyTechStackMap.get(companyName).addAll(projectTechStack.getLanguageSet());
            } else {
                companyTechStackMap.put(companyName, new HashSet<>(projectTechStack.getLanguageSet()));
            }

            final Date tempDate = new Date(timestamp);
            final LocalDate localDate = tempDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (localDate.getYear() <= 2016 && timestamp > lastTime2016) {
                lastTime2016 = timestamp;
                noLang2016 = companyTechStackMap.get(companyName).size();
            } else if (localDate.getYear() == 2017 && timestamp > lastTime2017) {
                lastTime2017 = timestamp;
                noLang2017 = companyTechStackMap.get(companyName).size();
            }
        }
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();


        // make number of languages statistics set
        final Map<Integer, Integer> noLangNoCompanyMap = new TreeMap<>();
        for (String companyName : companyTechStackMap.keySet()) {
            Integer noLang = companyTechStackMap.get(companyName).size();
            noLangNoCompanyMap.put(noLang, noLangNoCompanyMap.getOrDefault(noLang, 0) + 1);
        }
        // output this
        System.out.println("noLang\tnoOrg");
        for (Integer nolang : noLangNoCompanyMap.keySet()) {
            System.out.format("%d\t%d\n", nolang, noLangNoCompanyMap.get(nolang));
        }

    }
}

