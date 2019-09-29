package com.twilio.sync.quickstart.utils;

import com.twilio.sync.SyncClient;

import java.lang.reflect.Method;

public class SyncClientUtils {

    public static void simulateCrash(SyncClient syncClient, Where where) {
        try {
            Method method = SyncClient.class.getDeclaredMethod("simulateCrash", int.class);
            method.setAccessible(true);
            method.invoke(syncClient, where.mValue);
        } catch (Exception e) {
            throw new RuntimeException("Error in simulateCrash", e);
        }
    }

    public enum Where {
        SYNC_CLIENT_CPP(1),
        TS_CLIENT_CPP(2);

        final int mValue;

        Where(int value) {
            mValue = value;
        }
    }
}
