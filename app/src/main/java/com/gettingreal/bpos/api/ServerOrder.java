package com.gettingreal.bpos.api;

import java.util.Date;

/**
 * Created by ivanfoong on 10/6/14.
 */
public class ServerOrder {
    public Long id, receipt_id;
    public String table_uid, ordering_mode;
    public Date order_at;
    public ServerOrderItem[] order_items;
}