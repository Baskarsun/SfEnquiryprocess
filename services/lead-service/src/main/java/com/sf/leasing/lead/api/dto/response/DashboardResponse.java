package com.sf.leasing.lead.api.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Generic dashboard payload. Each dashboard section is a list of labeled numeric entries.
 */
public class DashboardResponse {

    public String reportType;
    public List<Entry> data;

    public DashboardResponse(String reportType, List<Entry> data) {
        this.reportType = reportType;
        this.data = data;
    }

    public static class Entry {
        public String label;
        public long count;

        public Entry(String label, long count) {
            this.label = label;
            this.count = count;
        }
    }
}
