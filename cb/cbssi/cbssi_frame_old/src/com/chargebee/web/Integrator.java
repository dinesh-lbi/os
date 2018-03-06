/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.web;

/**
 *
 * @author maris
 */
import com.chargebee.framework.IntegProps;
import com.chargebee.framework.IntegProps.Status;
import com.chargebee.framework.sync.SyncController;
import com.chargebee.logging.KVL;
import com.chargebee.org.json.JSONObject;


/**
 *
 * @author maris
 */
public class Integrator {

    public static void startInteg() throws Exception {
        KVL.start("main", false);
        Integrator integrator = new Integrator();
        integrator.integrate();
    }

    private void integrate() throws Exception {
        try {
            IntegProps.cunstructSyncObjects();
            SyncController syncController = new SyncController(IntegProps.getSyncObject());
            if (IntegProps.getIsInitialSync()) {
                syncController.initialSync();
            } else if (IntegProps.getIsRetrySync()) {
                syncController.retrySync();
            } else {
                syncController.sync();
            }
        } catch (Throwable th) {
            IntegProps.curInst().status = Status.failed;
            IntegProps.curInst().logger().printNLogErr(th);
        } finally {
            JSONObject obj = IntegProps.curInst().syncStats.getSyncMessage();
            IntegProps.getMetaClient().markAsFinished(obj, IntegProps.curInst().syncStats.status);
            System.out.println("-------------- SYNC FINISHED ---------------");
        }
    }

}
