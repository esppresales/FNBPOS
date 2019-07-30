package com.gettingreal.bpos.api.report;

import java.util.Map;

/**
 * Created by ivanfoong on 2/7/14.
 */
public class ServerMonthlyReport {
    public Integer month, year;
    public Double total_amount, average_amount_per_day, average_orders_per_day;
    public Integer total_order_count;
    public Map<Integer, Double> amount_by_day;
    public Map<Integer, Integer> order_count_by_day;
    public Map<String, Integer> product_ordered_count;
}
