package com.gettingreal.bpos.helper;

import android.content.Context;

import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSSurcharge;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by ivanfoong on 2/4/14.
 */
public class PaymentHelper {
    public static BigDecimal calculateTotalForOrder(final Context aContext, final ArrayList<POSOrderItem> aOrderItems) {
        BigDecimal subTotal = calculateSubTotalForOrder(aContext, aOrderItems);
        BigDecimal total = calculateTotalForSubTotal(aContext, subTotal);

        return total;
    }

    public static BigDecimal calculateSubTotalForOrder(final Context aContext, final ArrayList<POSOrderItem> aOrderItems) {
        BigDecimal subtotal = BigDecimal.valueOf(0.0);

        for (POSOrderItem orderItem : aOrderItems) {
            POSProduct product = POSProduct.getProduct(aContext, orderItem.getProductUid());
            if (product != null) {
                BigDecimal productTotal = BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(orderItem.getQuantityOrdered()));
                subtotal = subtotal.add(productTotal);
            }
        }

        return subtotal;
    }

    public static BigDecimal calculateTotalForSubTotal(final Context aContext, final BigDecimal subTotal) {
        BigDecimal total = subTotal;

        for (POSSurcharge surcharge : POSSurcharge.getAllSurcharges(aContext)) {

            BigDecimal totalPercentage = BigDecimal.ONE.add(BigDecimal.valueOf(surcharge.getPercentage()).divide(BigDecimal.valueOf(100.0)));
            total = total.multiply(totalPercentage);
        }

        return total;
    }
}
