/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.web;

import com.chargebee.framework.IntegProps;
import com.chargebee.framework.IntegProps.Status;
import com.chargebee.framework.util.ErrorUtil;
import java.util.*;
import java.util.concurrent.*;
import com.chargebee.logging.KVL;

public class Invoker 
{
    private static ThreadPoolExecutor pool;

    public static volatile boolean stopped = false;

    public static Map<String, IntegProps> currentProcesses = new ConcurrentHashMap();

    public static void init() {
        int size = Integer.getInteger("threadPool.size", 20);
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(size);
    }

    public static void startInteg()
            throws Exception {
        String domain = IntegProps.getCbSite();
        IntegProps existingProp = currentProcesses.get(domain);
        if (existingProp != null && existingProp.isScheduledOrRunning()) {
            //throw new SyncAlreadyRunningException("", null);
            throw new RuntimeException("", null);
        }
        _startInteg(IntegProps.curInst(), domain);
    }

    private static void _startInteg(IntegProps props, String domain) throws Exception {
        Callable c = props.wrap(() -> {
            try {
                props.status = Status.running;
                Integrator.startInteg();
                props.status = Status.succeeded;
            } catch (Exception ex) {
                props.status = Status.failed;
                props.logger().printNLogErr(ex);
            }
            return null;
        });
        Future future = submitJob("quickbooks_integ", c);//Need to check for rejection.
        props.future = future;
        currentProcesses.put(domain, props);
    }

    public static void shutDown() {
        stopped = true;
        pool.shutdown();
        long timeout = Integer.getInteger("threadPool.timeout", 60);
        try {
            if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.isTerminated()) {
                    System.err.println("Some threads in internal thread pool did not terminate properly");
                }
            }
        } catch (InterruptedException e) {
            ErrorUtil.logError(e);
        }
    }

    public static Future submitJob(String jobType, Callable c) {
        Callable wrap = (Callable) () -> {
            try {
                KVL.start("job", true);
                KVL.put("action_type", jobType);
                return c.call();
            } finally {
                KVL.end();
            }
        };
        return pool.submit(wrap);
    }

}
