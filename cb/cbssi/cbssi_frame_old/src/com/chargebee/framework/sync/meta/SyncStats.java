/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.sync.meta;

import com.chargebee.framework.IntegProps;
import com.chargebee.models.ThirdPartySyncDetail;
import com.chargebee.org.json.JSONArray;
import com.chargebee.org.json.JSONObject;

/**
 *
 * @author maris
 */
public class SyncStats {

    public JSONArray arr = new JSONArray();
    public boolean showProcessedCount = true;
    public ThirdPartySyncDetail.Status status = ThirdPartySyncDetail.Status.SUCCEEDED;

    public class SyncCount {

        Integer processed = 0;
        Integer succeeded = 0;
        Integer failed = 0;
    }

    SyncCount invCount;
    SyncCount cnCount;

    public SyncStats() {
        invCount = new SyncCount();
        cnCount = new SyncCount();
    }

    public void addInvCount() {
        invCount.processed++;
    }

    public void addCnCount() {
        cnCount.processed++;
    }

    public JSONObject getSyncMessage() throws Exception {
        JSONObject obj = new JSONObject();
        JSONArray innerArr = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            innerArr.put(arr.getString(i));
        }
        if (showProcessedCount) {
            innerArr.put(invCount.processed + " invoices processed.");
            innerArr.put(cnCount.processed + " credit notes processed.");
        }
        if (IntegProps.getIsInitialSync() || IntegProps.getIsRetrySync()) {
            JSONObject jobj = new JSONObject(IntegProps.curInst().integConfs.getString("integ.conf.initial_sync_details"));
            JSONObject statObj = new JSONObject();
            statObj.put("invoices", jobj.getJSONArray("invoices"));
            if (jobj.has("creditNotes")) {
                statObj.put("creditNotes", jobj.getJSONArray("creditNotes"));
            }
            if (jobj.has("payments")) {
                statObj.put("payments", jobj.getJSONArray("payments"));
            }
            obj.put("initial_sync_details", statObj);
            obj.put("retry_sync", true);
        }
        obj.put("sync_context_messages", innerArr);
        return obj;
    }

}
