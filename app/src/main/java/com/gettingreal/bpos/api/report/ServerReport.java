package com.gettingreal.bpos.api.report;

import java.util.Map;

/**
 * Created by ivanfoong on 2/7/14.
 */
public class ServerReport {
    public Map<String, ServerEODReport> eod;
    public Map<String, ServerMonthlyReport> monthly;
}
