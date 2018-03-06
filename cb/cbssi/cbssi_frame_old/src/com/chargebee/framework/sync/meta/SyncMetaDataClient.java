/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.sync.meta;

import com.chargebee.app.util.Logger;
import com.chargebee.Environment;
import com.chargebee.Result;
import com.chargebee.framework.IntegProps;
import com.chargebee.models.ThirdPartyConfiguration;
import com.chargebee.models.ThirdPartyEntityMapping;
import com.chargebee.models.ThirdPartyEntityMapping.EntityType;
import com.chargebee.models.ThirdPartySyncDetail;
import com.chargebee.models.Transaction;
import com.chargebee.org.json.JSONArray;
import com.chargebee.org.json.JSONObject;
import java.io.IOException;
import java.sql.Timestamp;

/**
 *
 * @author maris
 */
public class SyncMetaDataClient {

    private String syncId;
    private Environment env;
    private Logger logger;
    private String integration;

    public SyncMetaDataClient(boolean runningSync, String integration) throws Exception {
        env = IntegProps.getCBAPIEnv();
        this.integration = integration;
        syncId = (runningSync) ? createNewSync(integration).id() : null;
        logger = new Logger("cb.domain");

    }
    
    public  String getIntegrationName()
    {
        return integration;
    }

    public Result getResource(String id, EntityType resourceType) throws IOException {
        Result res = ThirdPartyEntityMapping
                .retrieveEntity()
                .entityType(resourceType)
                .entityId(id)
                .integrationName(integration)
                .request(env);
        return res;
    }

    public ThirdPartySyncDetail createNewSync(String integration) throws Exception {
        JSONObject obj = new JSONObject();
        JSONArray arr = new JSONArray();
        arr.put("Started ...");
        obj.put("sync_context_messages", arr);
        ThirdPartySyncDetail tpIntegSyncDetail = ThirdPartySyncDetail.create()
                .thirdPartyConfigurationIntegrationName(integration)
                .context(obj) //
                .request(env).thirdPartySyncDetail();
        return tpIntegSyncDetail;
    }

    public ThirdPartyEntityMapping getTpMapping(String id, EntityType entityType) throws Exception {
        return getResource(id, entityType).thirdPartyEntityMapping();
    }
    
    public String getExternalId(String id,EntityType resourceType) throws Exception 
    {
        return getResource(id, resourceType).thirdPartyEntityMapping().thirdPartyEntityId();
    }   
    
    public String getExternalId(ThirdPartyEntityMapping tem) throws Exception 
    {
        if(tem.status().equals(ThirdPartyEntityMapping.Status.CREATE_FAILED))
        {
            throw new RuntimeException("Depending Object Creation Failed");
        }
        return tem.thirdPartyEntityId();
    }     
    
    public Transaction getTransaction(String id) throws Exception {
        Result res = Transaction.retrieve(id).request(env);
        return res.transaction();
    }

    public void updateMapping(String entityId, ThirdPartyEntityMapping.EntityType entityType,
            JSONObject syncedResource, String externalId) throws IOException, InterruptedException {
        ThirdPartyEntityMapping.updateEntity()
                .entityId(entityId)
                .entityType(entityType)
                .integrationName(integration)
                .oldResource(syncedResource)
                .status(ThirdPartyEntityMapping.Status.SYNCED)
                .thirdPartyEntityId(externalId).request(env);
    }
    
    public void updateToBePicked(String entityId, ThirdPartyEntityMapping.EntityType entityType) throws IOException, InterruptedException {
        ThirdPartyEntityMapping.updateEntity()
                .entityId(entityId)
                .entityType(entityType)
                .integrationName(integration)
                .status(ThirdPartyEntityMapping.Status.TO_BE_PICKED)
                .request(env);
    }    

    public ThirdPartyEntityMapping updateMapping(String entityId, ThirdPartyEntityMapping.EntityType entityType,
            JSONObject syncedResource, JSONObject mappingMeta, String externalId) throws IOException, InterruptedException {
        ThirdPartyEntityMapping.UpdateEntityRequest req = ThirdPartyEntityMapping.updateEntity()
                .entityId(entityId)
                .entityType(entityType)
                .integrationName(integration)
                .oldResource(syncedResource)
                .status(ThirdPartyEntityMapping.Status.SYNCED)
                .thirdPartyEntityId(externalId);
        if (mappingMeta != null) {
            req.mappingMeta(mappingMeta);
        }
        return req.request(env).thirdPartyEntityMapping();
    }
    
    public void updateMappingAsIgnored(String entityId, ThirdPartyEntityMapping.EntityType entityType,
            JSONObject mappingMeta, String externalId) throws IOException, InterruptedException {
        ThirdPartyEntityMapping.UpdateEntityRequest req = ThirdPartyEntityMapping.updateEntity()
                .entityId(entityId)
                .entityType(entityType)
                .integrationName(integration)
                .status(ThirdPartyEntityMapping.Status.IGNORED)
                .thirdPartyEntityId(externalId);
        if (mappingMeta != null) {
            req.mappingMeta(mappingMeta);
        }
        req.request(env);
    }

    public void updateMetaMapping(String entityId, ThirdPartyEntityMapping.EntityType entityType, //
            JSONObject oldResource, JSONObject mappingMeta, String externalId) throws IOException, InterruptedException {
        ThirdPartyEntityMapping.UpdateEntityRequest req = ThirdPartyEntityMapping.updateEntity()
                .entityId(entityId)
                .entityType(entityType)
                .integrationName(integration)
                .oldResource(oldResource) 
                .status(ThirdPartyEntityMapping.Status.SYNCED)
                .thirdPartyEntityId(externalId);
        if (mappingMeta != null) {
            req.mappingMeta(mappingMeta);
        }
        req.request(env);
    }
    
    public void updateFailureMetaMapping(String entityId, ThirdPartyEntityMapping.EntityType entityType, //
            String errorMessage, JSONObject mappingMeta, String externalId) throws IOException, InterruptedException {
        ThirdPartyEntityMapping.UpdateEntityRequest req = ThirdPartyEntityMapping.updateEntity()
                .entityId(entityId)
                .entityType(entityType)
                .integrationName(integration)
                .errorMessage(errorMessage) 
                .status(ThirdPartyEntityMapping.Status.UPDATE_FAILED)
                .thirdPartyEntityId(externalId);
        if (mappingMeta != null) {
            req.mappingMeta(mappingMeta);
        }
        req.request(env);
    }

    public void sendErrorMessage(String entityId, ThirdPartyEntityMapping.EntityType entityType,
            String errorMessage, ThirdPartyEntityMapping.Status errorStatus, String tpId)
            throws IOException {
        ThirdPartyEntityMapping.updateEntity()
                .entityId(entityId)
                .entityType(entityType)
                .integrationName(integration)
                .thirdPartyEntityId(tpId) //
                .status(errorStatus)
                .errorMessage(errorMessage).request(env);
    }

    public void sendSkippedMessage(String entityId, ThirdPartyEntityMapping.EntityType entityType,
            String skipMessage, String failedParentId,
            ThirdPartyEntityMapping.FailedDependentEntityType failedParentEntityType)
            throws IOException {
        ThirdPartyEntityMapping.updateEntity()
                .entityId(entityId)
                .entityType(entityType)
                .integrationName(integration)
                .failedDependentEntityId(failedParentId)
                .failedDependentEntityType(failedParentEntityType)
                .status(ThirdPartyEntityMapping.Status.CREATE_FAILED)
                .errorMessage(skipMessage).request(env);
    }
    
    private void sendSyncStatus(JSONObject contextJson, String syncId, ThirdPartySyncDetail.Status status) throws IOException {
        ThirdPartySyncDetail.update(syncId)
                .status(status)
                .context(contextJson).request(env);
    }

    public void sendRunningSyncMessage() throws Exception {
        ThirdPartySyncDetail.update(syncId) //
                .status(ThirdPartySyncDetail.Status.RUNNING) //
                .context(new JSONObject("{\"test\":\"value\"}")).request(env);
                        //IntegProps.curInst().syncStats.getSyncMessage()).request(env);
    }

    public void markAsFinished(JSONObject contextJson, ThirdPartySyncDetail.Status status) throws Exception {
        sendSyncStatus(contextJson, syncId, status);
    }

    public void updateTpConfig(JSONObject propJson, JSONObject authJson, Timestamp lastSyncTime) throws IOException {
        ThirdPartyConfiguration.UpdateRequest req = ThirdPartyConfiguration.update()
                .configJson(propJson)
                .authJson(authJson)
                .integrationName(integration);
        if (lastSyncTime != null) {
            req.lastSyncAt(lastSyncTime);
        }
        req.request(env);
    }

    public void updateAuthJson(JSONObject authJson) throws IOException {
        ThirdPartyConfiguration.update()
                .authJson(authJson)
                .integrationName(integration)
                .request(env);
    }

    public void updateConfJson(JSONObject confJson) throws IOException {
        ThirdPartyConfiguration.update()
                .configJson(confJson)
                .integrationName(integration)
                .request(env);
    }

    public void sendContext(JSONObject contextJson) throws Exception {
        sendSyncStatus(contextJson, syncId, ThirdPartySyncDetail.Status.RUNNING);
    }

    public void sendContext(String message) throws Exception {
        JSONObject contextJSon = new JSONObject();
        JSONArray messages = new JSONArray();
        messages.put(message);
        contextJSon.put("sync_context_messages", messages);
        sendSyncStatus(contextJSon, syncId, ThirdPartySyncDetail.Status.STARTED);
    }

    public void updateReferenceInMetaMapping(
            String referenceId, String tpEntityId)throws Exception{
        ThirdPartyEntityMapping tpm = ThirdPartyEntityMapping.retrieveEntity()
                .entityId(tpEntityId).entityType(ThirdPartyEntityMapping.EntityType.TRANSACTION)
                .integrationName(integration).request(IntegProps.curInst().getCBAPIEnv()).
                thirdPartyEntityMapping();
        com.chargebee.org.json.JSONObject mappingMeta = tpm.mappingMeta();
        if( mappingMeta != null ){
            mappingMeta.put("txn_integ_reference", referenceId);            
        }else{
            mappingMeta = new com.chargebee.org.json.JSONObject().
                    put("txn_integ_reference", referenceId);
        }
        updateMapping(tpm.entityId(), 
                tpm.entityType(),tpm.oldResource(), 
                mappingMeta, tpm.thirdPartyEntityId());
    }
}