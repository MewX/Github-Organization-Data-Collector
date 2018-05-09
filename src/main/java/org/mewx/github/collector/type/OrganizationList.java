package org.mewx.github.collector.type;

import au.edu.uofa.sei.assignment1.collector.type.BaseKeyRequestType;

public class OrganizationList extends BaseKeyRequestType {

    public OrganizationList() {
        super(OrganizationList.class.getSimpleName());
    }

    @Override
    public String constructParam(int page, String key) {
        // Todo:
        return null;
    }

    @Override
    public String constructRequestUrl(String key) {
        // Todo:
        return null;
    }
}
