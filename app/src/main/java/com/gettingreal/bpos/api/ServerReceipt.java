package com.gettingreal.bpos.api;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by ivanfoong on 16/6/14.
 */
public class ServerReceipt {
    public Long id;
    public BigDecimal paid_amount, discount_amount, final_amount;
    public Date paid_at;
    public String discount_description;
    public Long[] order_ids;
    public ServerOrder[] orders;
}
