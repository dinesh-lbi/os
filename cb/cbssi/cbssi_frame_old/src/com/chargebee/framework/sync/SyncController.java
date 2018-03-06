/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.sync;

/*
 * Copyright (c) 2016 Chargebee Inc
 * All Rights Reserved.
 */

import com.chargebee.ListResult;
import com.chargebee.Result;
import com.chargebee.framework.IntegProps;
import com.chargebee.framework.exception.SyncStopException;
import com.chargebee.framework.util.DateUtils;
import com.chargebee.framework.util.GlobalUtil;
import com.chargebee.framework.web.FormatterUtil;
import com.chargebee.models.CreditNote;
import com.chargebee.models.Invoice;
import com.chargebee.models.Order;
import com.chargebee.models.Order.OrderListRequest;
import com.chargebee.models.ThirdPartyEntityMapping;
import com.chargebee.models.ThirdPartyEntityMapping.EntityType;
import com.chargebee.models.ThirdPartySyncDetail;
import com.chargebee.models.Transaction;
import com.chargebee.org.json.JSONArray;
import com.chargebee.org.json.JSONException;
import com.chargebee.org.json.JSONObject;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author maris
 */
public  class SyncController {

    SyncObject sybObj;

    public SyncController(SyncObject sybObj) {
        this.sybObj = sybObj;
    }

    public void initialSync() throws JSONException, Exception 
    {
//    	Order.list().request().listIterator().next().order();
        JSONObject integConf = IntegProps.curInst().integConfs;
        JSONObject initial_sync_details = new JSONObject(integConf.getString("integ.conf.initial_sync_details"));
        JSONArray initial_sync_invoices = initial_sync_details.getJSONArray("invoices");
        Timestamp syncStartDate = IntegProps.getFirstSyncStartDate(); //Date configured by user from which chargebee should sync data.
        Timestamp currSyncSessionStartTime = syncStartDate;
        Timestamp currSyncSessionEndTime = DateUtils.now(); // Fetch till this time
        IntegProps.printNLog("Initial Sync Started.");
        IntegProps.printNLog("Current Sync Start Time : " + FormatterUtil.dateTime3(currSyncSessionStartTime));
        IntegProps.printNLog("Current Sync End Time : " + FormatterUtil.dateTime3(currSyncSessionEndTime));
        IntegProps.printNLog("");
        sybObj.prerequest();
        List<Invoice> invoices = processInitialSyncInvoices(initial_sync_invoices);
        processInitialSyncInvoicesCredits(invoices, syncStartDate);
        IntegProps.curInst().status = IntegProps.Status.succeeded;
    }
    
    public void retrySync() throws JSONException, Exception 
    {
        JSONObject integConf = IntegProps.curInst().integConfs;
        JSONObject initial_sync_details = new JSONObject(integConf.getString("integ.conf.initial_sync_details"));
        JSONArray retry_sync_invoices = initial_sync_details.optJSONArray("invoices") != null ? initial_sync_details.optJSONArray("invoices") : new JSONArray();
        JSONArray retry_sync_creditNotes = initial_sync_details.optJSONArray("creditnotes") != null ? initial_sync_details.optJSONArray("creditnotes") : new JSONArray();
        List<Invoice> invoices = ToInvList(retry_sync_invoices);
        List<CreditNote> creditnotes = ToCreList(retry_sync_creditNotes);
        initialSyncInvoices(invoices);
        initialSyncCreditNotes(creditnotes);
        IntegProps.curInst().status = IntegProps.Status.succeeded;
    }

    private List ToInvList(JSONArray array) throws JSONException, IOException {
        List<Invoice> list = new ArrayList<Invoice>();
        for (int i = 0; i < array.length(); i++) {
            Result res = getInvResource((String) array.get(i));
            Invoice inv = res.invoice();
            list.add(inv);
        }
        return list;
    }
    
    private List ToCreList(JSONArray array) throws JSONException, IOException {
        List<CreditNote> list = new ArrayList<CreditNote>();
        for (int i = 0; i < array.length(); i++) {
            Result res = getCreditNoteResource((String) array.get(i));
            CreditNote cn = res.creditNote();
            list.add(cn);
        }
        return list;
    }

    public void initialSyncInvoices(List<Invoice> invList) throws Exception {
        IntegProps.printNLog("");
        IntegProps.printNLog("Processing InitialSync Invoice List.");
        IntegProps.printNLog("");
        for (Invoice invoice : invList) {
            try {
                IntegProps.printNLog("Processing invoice - " + invoice.id() + ".");
                BaseObject result = null;
                //result = IntegProps.getObjectFactory().getConversionObject(invoice.id(), ThirdPartyEntityMapping.EntityType.INVOICE, false, ObjectFactory.Sync_Spl_operation.create_new);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                IntegProps.printNLog("Failed Invoice update :" + e.getMessage());
                IntegProps.printNLogErr(e);
            }
        }
    }

    public void initialSyncCreditNotes(List<CreditNote> creditNoteList) {
        IntegProps.printNLog("");
        IntegProps.printNLog("Processing InitialSync CreditNote List.");
        IntegProps.printNLog("");
        for (CreditNote creditNote : creditNoteList) {
            try {
                IntegProps.printNLog("Processing credit note - " + creditNote.id() + ".");
                BaseObject result = null;
                //result = IntegProps.getObjectFactory().getConversionObject(creditNote.id(), ThirdPartyEntityMapping.EntityType.CREDIT_NOTE, false, ObjectFactory.Sync_Spl_operation.create_new);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                IntegProps.printNLog("Exception while Processing credit note not handled" + e);
                IntegProps.printNLogErr(e);
            }
        }
    }

    public void sync() throws Exception 
    {
        Timestamp syncStartDate = IntegProps.getFirstSyncStartDate(); //Date configured by user from which chargebee should sync data.
        Timestamp lastSyncTime = IntegProps.getLastSyncTime(); // Last sync end time

        boolean isFirstSync = lastSyncTime.equals(syncStartDate);

        Timestamp currSyncSessionStartTime = isFirstSync ? lastSyncTime : DateUtils.addMinutes(lastSyncTime, -5);
        Timestamp currSyncSessionEndTime = IntegProps.getSyncEndDate();

        try 
        {
            if(!currSyncSessionEndTime.after(lastSyncTime))
            {
                IntegProps.printNLog("Sync terminated as sync end time configured is reached");
                //throw new CBException("Sync terminated as sync end time configured is reached", null);
                throw new RuntimeException("Sync terminated as sync end time configured is reached", null);
            }
            IntegProps.printNLog("Sync Started.");
            IntegProps.printNLog("Current Sync Start Time : " + FormatterUtil.dateTime3(currSyncSessionStartTime));
            IntegProps.printNLog("Current Sync End Time : " + FormatterUtil.dateTime3(currSyncSessionEndTime));
            IntegProps.printNLog("Last Sync End Time : " + FormatterUtil.dateTime3(lastSyncTime));
            IntegProps.printNLog("");
            
            sybObj.prerequest();
            //cache tax objects
           // IntegProps.curInst().taxFactory = new TaxResourceFactory();
            

            //Invoice code
            handleInvoiceRequest(currSyncSessionStartTime, currSyncSessionEndTime, isFirstSync);

            //handle creditnot request
            handleCreditNoteRequest(currSyncSessionStartTime, currSyncSessionEndTime, isFirstSync);


            lastSyncTime = currSyncSessionEndTime;
            IntegProps.curInst().status = IntegProps.Status.succeeded;
        } catch (SyncStopException e) {
            IntegProps.printNLogErr(e);
            IntegProps.curInst().status = IntegProps.Status.failed;
            IntegProps.curInst().syncStats.status = ThirdPartySyncDetail.Status.FAILED;
            IntegProps.curInst().syncStats.showProcessedCount = false;
            IntegProps.curInst().syncStats.arr.put("Sync Failed. ");
            IntegProps.curInst().syncStats.arr.put(e.getMessage());
        }
        catch (Exception e) 
        {
            IntegProps.printNLogErr(e);
            IntegProps.curInst().status = IntegProps.Status.failed;
            IntegProps.curInst().syncStats.status = ThirdPartySyncDetail.Status.FAILED;
        }
        finally 
        {
            IntegProps.getMetaClient().updateTpConfig(IntegProps.getConfJson(), IntegProps.getAuthJson(), lastSyncTime);
            IntegProps.printNLog("");
            IntegProps.printNLog("Sync Finished.");
        }    
    }
        
    public void handleCreditNoteRequest(Timestamp currSyncSessionStartTime, Timestamp currSyncSessionEndTime, boolean isFirstSync) throws Exception {
        String offset = null;
        int count = 0;
        do {
            CreditNote.CreditNoteListRequest cnListReq = CreditNote.list();
            if (isFirstSync) {
                cnListReq.date().between(currSyncSessionStartTime, currSyncSessionEndTime);
            } else {
                cnListReq.updatedAt().between(currSyncSessionStartTime, currSyncSessionEndTime);
            }
            ListResult cnList = cnListReq.includeDeleted(Boolean.TRUE) //
                    .limit(100)
                    .offset(offset)//
                    .request(IntegProps.getCBAPIEnv());
            offset = cnList.nextOffset();
            count += cnList.size();
            IntegProps.printNLog("");
            IntegProps.printNLog("Processing Credit Note List.");
            IntegProps.printNLog("");
            for (int i = 0; i < cnList.size(); i++) {
                try {
                    CreditNote cbCN = cnList.get(i).creditNote();
                    IntegProps.printNLog("Processing credit note - " + cbCN.id() + ".");
                    BaseObject result = null;
                    try {
                        result = null;
                        //IntegProps.getObjectFactory().getConversionObject(cbCN.id(), ThirdPartyEntityMapping.EntityType.CREDIT_NOTE, false, ObjectFactory.Sync_Spl_operation.sync_status_alone);
                    } catch (SyncStopException e) {
                        throw e;
                    } catch (Exception ex) {
                        //check for already object created or not
                    }
                    if (result == null) {
                        this.sybObj.syncData(cbCN);
                    }
                } catch (SyncStopException e) {
                    throw e;
                } catch (Exception e) {
                    IntegProps.printNLog("Exception while processing credit note not handled" + e);
                    IntegProps.printNLogErr(e);
                }
            }
            IntegProps.getMetaClient().sendRunningSyncMessage();
            IntegProps.printNLog("Processed credit note count - " + count);
        } while (offset != null);
    }

    public void handleInvoiceRequest(Timestamp currSyncSessionStartTime, Timestamp currSyncSessionEndTime, boolean isFirstSync) throws Exception {
        String offset = null;
        int count = 0;
        do {
            Invoice.InvoiceListRequest invListReq = com.chargebee.models.Invoice.list();
            if (isFirstSync) {
                invListReq.date().between(currSyncSessionStartTime, currSyncSessionEndTime);
            } else {
                invListReq.updatedAt().between(currSyncSessionStartTime, currSyncSessionEndTime);
            }
            ListResult invList = invListReq.status().isNot(Invoice.Status.PENDING) //
                    .includeDeleted(Boolean.TRUE) //
                    .limit(100) //
                    .offset(offset) //
                    .request(IntegProps.getCBAPIEnv());

            offset = invList.nextOffset();
            count += invList.size();

            IntegProps.printNLog("");
            IntegProps.printNLog("Processing Invoice List.");
            IntegProps.printNLog("");

            for (int i = 0; i < invList.size(); i++) {
                try {
                    Invoice inv = invList.get(i).invoice();
                    IntegProps.printNLog("Processing invoice - " + inv.id() + ".");
                    BaseObject result = null;
                    try {
                        result = null;
                        //IntegProps.getObjectFactory().getConversionObject(inv.id(), ThirdPartyEntityMapping.EntityType.INVOICE, false, ObjectFactory.Sync_Spl_operation.sync_status_alone);
                    } catch (SyncStopException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        //check for already object created or not
                    }
                    if (result == null) {
                        this.sybObj.syncData(inv);
                    }
                } catch (SyncStopException e) {
                    throw e;
                } catch (Exception e) {
                    IntegProps.printNLog("Failed Invoice update :" + e.getMessage());
                    IntegProps.printNLogErr(e);
                }
            }
            IntegProps.getMetaClient().sendRunningSyncMessage();
            IntegProps.printNLog("Processed Invoice Count - " + count);
        } while (offset != null);
    }

    public void handleFailuareRequest() throws Exception {
        boolean first_cn = false;
        String offset = null;
        List<ThirdPartyEntityMapping> forceSyncTpms = getForceSyncInvTpms();
        // Failures List
        do {
            ListResult failureList = com.chargebee.models.ThirdPartyEntityMapping.list() //
                    .integrationName("quickbooks") //
                    .limit(100) //
                    .offset(offset)//
                    .request(IntegProps.getCBAPIEnv());
            offset = failureList.nextOffset();

            List<ThirdPartyEntityMapping> list = sortNFilterFailureList(failureList);
            if(offset == null){
                list.addAll(forceSyncTpms);
            }
            IntegProps.printNLog("Processing Failure List.");
            IntegProps.printNLog("");
            for (ThirdPartyEntityMapping tpm : list) {
                try {
                    String id = tpm.entityId();
                    ThirdPartyEntityMapping.EntityType eType = tpm.entityType();
                    IntegProps.printNLog("Processing " + eType.name() + " - " + id + ".");
                    if (!first_cn && eType.equals(ThirdPartyEntityMapping.EntityType.CREDIT_NOTE)) {
                        IntegProps.getBatchExecutor().setFinished();
                        first_cn = true;
                    }
                    BaseObject result = null;
                    try {
                        result = null;
                        //IntegProps.getObjectFactory().getConversionObject(id, eType, false, ObjectFactory.Sync_Spl_operation.sync_status_alone);
                    } catch (SyncStopException e) {
                        throw e;
                    } catch (Exception e) {

                    }
                    if (result == null) {
                        this.sybObj.syncData(tpm);
                    }
                } catch (SyncStopException e) {
                    throw e;
                } catch (Exception ex) {
                    IntegProps.printNLog("Failed Failuare update :" + ex.getMessage());
                    IntegProps.printNLogErr(ex);
                }
            }
            IntegProps.getMetaClient().sendRunningSyncMessage();
        } while (offset != null);
        removeForcSyncTpms();
    }

    private List<ThirdPartyEntityMapping> sortNFilterFailureList(ListResult res) {
        List<ThirdPartyEntityMapping> list = new ArrayList<>();
        // Skipping Customers from failue list for multicurrency enabled sites,
        // as we should send currency to be used for customer from invoice or Credit note object.
        if (!IntegProps.isMultiCurrencyEnabledInCB()) {
            filterNAdd(res, list, ThirdPartyEntityMapping.EntityType.CUSTOMER);
        }
        filterNAdd(res, list, ThirdPartyEntityMapping.EntityType.PLAN);
        filterNAdd(res, list, ThirdPartyEntityMapping.EntityType.ADDON);
        filterNAdd(res, list, ThirdPartyEntityMapping.EntityType.INVOICE);
        filterNAdd(res, list, ThirdPartyEntityMapping.EntityType.TRANSACTION);
        filterNAdd(res, list, ThirdPartyEntityMapping.EntityType.CREDIT_NOTE);
        return list;
    }

    private void filterNAdd(ListResult res, List<ThirdPartyEntityMapping> list, //
            ThirdPartyEntityMapping.EntityType eType) {
        for (ListResult.Entry e : res) {
            ThirdPartyEntityMapping tpm = e.thirdPartyEntityMapping();
            if (tpm.entityType().equals(eType)) {
                list.add(tpm);
            }
        }
    }

    public Result getInvResource(String id) throws IOException {
        Result res = Invoice.retrieve(id)
                .request(IntegProps.getCBAPIEnv());
        return res;
    }
    
    public Result getCreditNoteResource(String id) throws IOException{
        Result res = CreditNote.retrieve(id)
                .request(IntegProps.getCBAPIEnv());
        return res;
    }
    
    public Result getTransactionResource(String id) throws IOException{
        Result res = Transaction.retrieve(id)
                .request(IntegProps.getCBAPIEnv());
        return res;
    }


    private List<ThirdPartyEntityMapping> getForceSyncInvTpms() {
         return IntegProps.getForceSyncTpms().stream().map(id -> {
            try {
                return IntegProps.getMetaClient().getTpMapping(id, EntityType.INVOICE);
            } catch (Exception e) {
                throw GlobalUtil.asRtExp(e);
            }
        }).collect(Collectors.toList());
    }
    
    private void removeForcSyncTpms() throws Exception{
        JSONObject conf = IntegProps.getConfJson();
        conf.remove("integ.conf.force_sync_invs");
        IntegProps.getMetaClient().updateConfJson(conf);
    }
    
    public List<Invoice> processInitialSyncInvoices(JSONArray invIdList) throws JSONException 
    {
        List<Invoice> invoices = new ArrayList<>();
        for (int i = 0; i < invIdList.length(); i++) {
            try {
                Result  res = getInvResource((String) invIdList.get(i));
                Invoice inv = res.invoice();
                invoices.add(inv);
            } catch (Exception e) {
                IntegProps.printNLogErr(e);

            }
        }
        return invoices;
    }

    public void processInitialSyncInvoicesCredits(List<Invoice> invList, Timestamp syncStartDate) throws JSONException 
    {
        //
    }

       
}

