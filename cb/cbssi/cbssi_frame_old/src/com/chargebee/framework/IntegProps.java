/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework;

import com.chargebee.framework.sync.meta.SyncMetaDataClient;
import com.chargebee.*;
import com.chargebee.app.util.Logger;
import com.chargebee.framework.data.connector.batch.BatchSyncExecutor;
import com.chargebee.framework.sync.SyncObject;
import com.chargebee.framework.sync.meta.SyncStats;
import com.chargebee.framework.util.DateUtils;
import com.chargebee.org.json.JSONArray;
import com.chargebee.org.json.JSONException;
import com.chargebee.org.json.JSONObject;
import com.google.common.base.Strings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 *
 * @author maris
 */
public class IntegProps {

    private static final ThreadLocal<IntegProps> curInst = new ThreadLocal();

    public enum Status {

        scheduled, needs_auth, running, succeeded, failed, stopped;
    }

    public static boolean isServer = false;

    private Logger logger;
    
    public final JSONObject integConfs;
    
    public final JSONObject integAuth;
    
    public final JSONObject cbProps;
        
    private SyncDestination integration;

    public Status status = Status.scheduled;

    public Future future;
    
    public Timestamp syncEndTime;
   
    public String integCompanyVersion = null;
    
    public SyncObject syncObj = null;
    
    public SyncMetaDataClient metaClient = null;
    
    public BatchSyncExecutor batchExe = null;
    
    private final boolean isInitialSync;
    
    private final boolean isRetrySync;    
    
    public SyncStats syncStats;
    
    public JSONArray activeFields;
        
    public static void cunstructSyncObjects() throws Exception
    {
        
    }
    
    public static JSONArray getActiveFieldsList() throws JSONException
    {
        if(curInst().activeFields != null)
        {
            return curInst().activeFields;
        }
        else 
        {
            IntegProps xp = curInst.get();
            if(xp.cbProps.has("integ.conf.chosen_fields"))
            {
                return xp.cbProps.getJSONArray("integ.conf.chosen_fields");
            }
        }
        return null;  
    }

    
    public static SyncObject getSyncObject()
    {
        return curInst().syncObj;
    }   
    
    public static BatchSyncExecutor getBatchExecutor()
    {
        return curInst().batchExe;
    }
    
    public static String getCbBaseCurrencyCode() throws JSONException {
        return getCBCurrencyConfig().getString("base_currency");
    }

    public static JSONObject getCBCurrencyConfig() throws JSONException{
        String currencyConfStr = Strings.emptyToNull(getValue("cb.currency_config"));
        return (currencyConfStr == null) ? null : new JSONObject(currencyConfStr);
    }
    
    public static JSONArray getCbSubscribedCurrencies() throws JSONException{
        return getCBCurrencyConfig().getJSONArray("subscribed_currencies");
    }
    
    public static SyncMetaDataClient getMetaClient() throws Exception 
    {
        return (curInst().metaClient !=null? curInst().metaClient : new SyncMetaDataClient(false, "quickbooks"));
    }
    
    public static enum SyncDestination 
    {
        quickbooks,xero,salesforce,slack,shipstation;
    }

    public IntegProps(String integConfsStr, String integAuthStr, String metaPropsStr, String syncType) throws Exception {
        this.integConfs = integConfsStr == null ? new JSONObject() : new JSONObject(integConfsStr);
        this.integAuth = integAuthStr == null ? new JSONObject() : new JSONObject(integAuthStr);
        this.cbProps = metaPropsStr == null ? new JSONObject() : new JSONObject(metaPropsStr);
        this.isInitialSync = syncType == null ? false : syncType.equals("initial_sync");
        this.isRetrySync = syncType == null ? false : syncType.equals("retry_sync");        
    }

    public static String getConsumerKey() {
        return getValue("cb.consumer_key");
    }

    public static String getConsumerKeySecret() {
        return getValue("cb.consumer_secret");
    }

    public static String getAccessToken() {
        return getValue("integ.auth.access.token");
    }

    public static String getAccessTokenSecret() {
        return getValue("integ.auth.access.token.secret");
    }
    
    public static Timestamp getAccessTokeCreationDate() {
        String date = getValue("integ.auth.creation_date");
        return date == null ? null : Timestamp.valueOf(date);
    }
    
    public static Timestamp getNextReAuthenticationTime(){
        String date = getValue("integ.conf.next_reauth_time");
        return date == null ? null : Timestamp.valueOf(date);
    }
    
    public static void setStatusAsStopped() {
        IntegProps.curInst().status = Status.stopped;
    }

    public static boolean isStatusStopped(){
        return IntegProps.curInst().status == Status.stopped;
    }
    
    public static String getCompanyId() {
        return getValue("integ.conf.companyid");
    }
    
    /**
     * Start date configured by the customer while integrating quickbooks.
     */
    public static Timestamp getFirstSyncStartDate() {
        String date = getValue("integ.conf.sync.startdate");
        return Timestamp.valueOf(date);
    }
    
        public static Timestamp getSyncEndDate() {
        String date = getValue("integ.conf.sync.enddate");
        if(date==null){
            curInst().syncEndTime = DateUtils.now();
            return DateUtils.now();
        }
        Timestamp t = DateUtils.now();
        try
        {
            t=Timestamp.valueOf(date);
            curInst().syncEndTime = t;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return t;
    }
        
    public static boolean getIsInitialSync() {
        return curInst().isInitialSync;
    }
    
    public static boolean getIsRetrySync(){
        return curInst().isRetrySync;
    }        

    public static void updateAuthJson(String accessToken, String accessTokenSecret, Timestamp date) throws Exception {
        curInst().integAuth.put("integ.auth.access.token", accessToken);
        curInst().integAuth.put("integ.auth.access.token.secret", accessTokenSecret);
        curInst().integAuth.put("integ.auth.creation_date", date);
        curInst().integAuth.put("integ.auth.next_reauth_time", DateUtils.addDays(date, 150));
        IntegProps.getMetaClient().updateAuthJson(curInst().integAuth);
    }
    
    public static void updateAuthJson(Timestamp nextReAuthDate) throws JSONException, IOException, Exception{
        curInst().integAuth.put("integ.auth.next_reauth_time", nextReAuthDate);
        IntegProps.getMetaClient().updateAuthJson(curInst().integAuth);
    }

    public boolean isScheduledOrRunning() {
        return status.equals(Status.scheduled) || status.equals(Status.running);
    }

    public boolean isFinished() {
        return status.equals(Status.succeeded) || status.equals(Status.failed) || status.equals(Status.stopped);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

   // public static DataConnector getQBClient() {
   //     return curInst().integClient;
   // }

    public static IntegProps curInst() {
        return curInst.get();
    }


    private static String getValue(String key) 
    {
        IntegProps xp = curInst.get();
        String value = null;
        if (xp == null) {
            throw new RuntimeException("Props not yet configured");
        }
        if (key.startsWith("cb.")) {
            value = xp.cbProps.optString(key);
        } else if (key.startsWith("integ.conf.")) {
            value = xp.integConfs.optString(key);
        } else if (key.startsWith("integ.auth.")) {
            value = xp.integAuth.optString(key);
        }
        else
        {
            throw new RuntimeException("Not a expected key.");
        }
        
        if(value.isEmpty())
        {
            return null;
        }
        return value ;   
    }
    
    public String site() throws Exception {
        return cbProps.getString("cb.domain");
    }

    public Logger logger() throws Exception {
        if (logger == null) {
            logger = new Logger(site());
        }
        return logger;
    }

    public Callable wrap(Callable c) {
        Callable wrap = (Callable) () -> {
            //KVL.put("domain", site());
            if (curInst.get() != null) {
                throw new RuntimeException("Already Integ Props set");
            }
            try {
                curInst.set(this);
                Object result = c.call();
                return result;
            } finally {
                curInst.remove();
            }
        };
        return wrap;
    }

    /**
     * @return the cbsite
     */
    public static String getCbSite() {
        return getValue("cb.domain");
    }

    /**
     * @return the cbAPikey
     */
    private static String getCbAPikey() {
        return getValue("cb.api_key");
    }

    public static String getApiCallBackURL() {
        return getValue("cb.api_call_back_url");
    }

    public static String getCbCountryCode() {
        return getValue("cb.country_code");
    }

    public static String getCbEnv() {
        return getValue("cb.environment");
    }
    
    public static boolean isMultiCurrencyEnabledInCB() {
        return Boolean.valueOf(getValue("cb.multi_currency_enabled"));
    }

    public static String getBaseCurrencyInCB() {
        return getValue("cb.base_currency");
    }

    public static Environment getCBAPIEnv() {
        return new Environment(getCbSite(), getCbAPikey());
    }

    public static JSONObject getAuthJson() {
        return curInst().integAuth;
    }

    public static JSONObject getConfJson() {
        return curInst().integConfs;
    }


    public static Timestamp getLastSyncTime() {
            Long mills = Long.valueOf(getValue("integ.conf.lastsynctime"));
        return new Timestamp(mills);
    }
    
    public static Timestamp getSyncEndTime(){
        if( curInst().syncEndTime != null ){
            return curInst().syncEndTime;
        }
        return getSyncEndDate();
    }
    
    public static JSONObject getSyncTimeDets() throws JSONException {
        String syncTimeDet = Strings.emptyToNull(getValue("integ.conf.synctime.details"));
        return (syncTimeDet == null) ? null : new JSONObject(Strings.emptyToNull(getValue("integ.conf.synctime.details")));
    }
    
    public static Logger getLogger() throws Exception {
        IntegProps inst = curInst();
        return inst.logger();
    }

    public static void printNLog(String str) {
        try {
            getLogger().printNLog(str);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void printNLogErr(Exception e) {
        try {
            getLogger().printNLogErr(e);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void printNLogErr(Throwable th) {
        try {
            getLogger().printNLogErr(th);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public static JSONObject getConfGatewayDetails()throws Exception{
         return curInst().cbProps.optJSONObject("cb.gateway_credentials");
     }

    public static List<String> getForceSyncTpms() {
        JSONObject integConf = IntegProps.getConfJson();
        String ids = integConf.optString("integ.conf.force_sync_invs", null);
        if (ids == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(ids.split(",")).stream().map(String::trim).collect(Collectors.toList());
    }
}
