package com.gettingreal.bpos;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by ivanfoong on 28/3/14.
 */
public class UIDGenerator {
    public static String generateUniqueStringForTags(final String[] tags) {
        StringBuilder sb = new StringBuilder();

        sb.append(new Timestamp(new Date().getTime()));
        sb.append(";");

        for (int i = 0; i < tags.length; i++) {
            sb.append(tags[i]);
            sb.append(";");
        }

        return sb.toString();
    }
}
