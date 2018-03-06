/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.sync;



import com.chargebee.framework.IntegProps;
import com.chargebee.framework.exception.SyncStopException;
import com.chargebee.framework.exception.SyncStoppedErrorCodes;
import com.chargebee.models.ThirdPartyEntityMapping;
import com.chargebee.org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
/**
 *
 * @author maris
 */
public class SyncMetaObject
{   
    
    public ThirdPartyEntityMapping tpMapping;
    
    public ThirdPartyEntityMapping.EntityType type;
    
    public ThirdPartyEntityMapping.Status status;
    
    public SyncDependentObject depSyncObject = null;
    
    public JSONObject oldResourceObj;
    
    public String tpId = null;
    
    public String id;
    
    public String errorMsg;
    
    boolean partiallySynced = false;
    
    boolean forceSync = false;
    
    private boolean isMismatched = false;
    
    public static final Map<ThirdPartyEntityMapping.EntityType,ThirdPartyEntityMapping.FailedDependentEntityType> ENTITY_FAILEDENTITY ;
    
    static
    {
        ENTITY_FAILEDENTITY = new HashMap<>();
        ENTITY_FAILEDENTITY.put(ThirdPartyEntityMapping.EntityType.PLAN, ThirdPartyEntityMapping.FailedDependentEntityType.PLAN);
        ENTITY_FAILEDENTITY.put(ThirdPartyEntityMapping.EntityType.ADDON, ThirdPartyEntityMapping.FailedDependentEntityType.ADDON);
        ENTITY_FAILEDENTITY.put(ThirdPartyEntityMapping.EntityType.INVOICE, ThirdPartyEntityMapping.FailedDependentEntityType.INVOICE);
        ENTITY_FAILEDENTITY.put(ThirdPartyEntityMapping.EntityType.CREDIT_NOTE, ThirdPartyEntityMapping.FailedDependentEntityType.CREDIT_NOTE);
        ENTITY_FAILEDENTITY.put(ThirdPartyEntityMapping.EntityType.CUSTOMER, ThirdPartyEntityMapping.FailedDependentEntityType.CUSTOMER);
        ENTITY_FAILEDENTITY.put(ThirdPartyEntityMapping.EntityType.TRANSACTION, ThirdPartyEntityMapping.FailedDependentEntityType.TRANSACTION);
        
    }

    public SyncMetaObject(String id,ThirdPartyEntityMapping.EntityType type) throws Exception
    {
        if(IntegProps.isStatusStopped())
        {
            throw new SyncStopException(SyncStoppedErrorCodes.MetaDataError, null,true);
        }
        
        this.id = id;
        this.type = type;
        ThirdPartyEntityMapping tpMap = null;
        //Handle delete data
        try
        {
            tpMap = IntegProps.getMetaClient().getTpMapping(id, type);
        }
        catch(Exception e)
        {
            IntegProps.curInst().logger().printNLogErr(e);
        }
        if(tpMap !=null )
        {
            tpMapping = tpMap;
            if(tpMap.status()!=null && tpMap.status().equals(ThirdPartyEntityMapping.Status.IGNORED))
            {
                errorMsg = "Updation failed as this object was marked as ignored in chargebee.";
                updateFails();
                throw new RuntimeException("Object is in ignored status",null);
            }
            if(tpMap.thirdPartyEntityId()!=null)
            {
                oldResourceObj = tpMap.oldResource();
                tpId = tpMap.thirdPartyEntityId();
                if(tpMap.status()!=null && tpMap.status().equals(ThirdPartyEntityMapping.Status.PARTIALLY_SYNCED))
                {
                    partiallySynced = true;
                }
                if(tpMap.status()!=null && tpMap.status().equals(ThirdPartyEntityMapping.Status.FORCE_SYNC))
                {
                    forceSync = true;
                }                
            }
        }
    }
    
    public String getMetaMapDataForParameter(String param) throws Exception
    {
        String res = null;
        if(tpMapping.mappingMeta() != null && tpMapping.mappingMeta().has(param))
        {
            res = tpMapping.mappingMeta().getString(param);
        }
        return res;
    }
    
    public boolean isMismatched()
    {
        return isMismatched;
    }
    
    public void setMismatched(boolean bool)
    {
        isMismatched = bool;
    }
    
    public boolean isSynced()
    {
        return status.equals(ThirdPartyEntityMapping.Status.SYNCED);
    }
    
    public void setResourceObj(JSONObject resourceObj)
    {
        this.oldResourceObj = resourceObj;
    }
    
    public void update() throws Exception
    {
        IntegProps.getMetaClient().updateMapping(id, type, oldResourceObj, tpId);        
    }
    
    public void setDependentFailed(String failedId,ThirdPartyEntityMapping.EntityType etype,String error)
    {
        this.errorMsg = error;
        this.depSyncObject = new SyncDependentObject(failedId, ENTITY_FAILEDENTITY.get(etype));
    }
    
    public void setAdditionalDependingFailedInfo(String failedId,ThirdPartyEntityMapping.EntityType etype,String error)
    {
        this.errorMsg = this.errorMsg + "depending Failed : id = "+id+" entity type="+etype+" error = "+error;
    }
    
    public final void updateFails() throws Exception
    {
        if (errorMsg == null) {
            errorMsg = "CB Internal Error.";
        }
        IntegProps.getMetaClient().sendErrorMessage(id, type, errorMsg, ThirdPartyEntityMapping.Status.UPDATE_FAILED, tpId);
    }
    
    public void createFails() throws Exception {
        if (errorMsg == null) {
            errorMsg = "CB Internal Error.";
        }
        IntegProps.getMetaClient().sendErrorMessage(id, type, errorMsg, ThirdPartyEntityMapping.Status.CREATE_FAILED, tpId);
    }   
    
    public void updateMismatch() throws Exception {
        if (errorMsg == null) {
            errorMsg = "CB Internal Error.";
        }
        IntegProps.getMetaClient().sendErrorMessage(id, type, errorMsg, ThirdPartyEntityMapping.Status.MISMATCH, tpId);
    }     
    
    public String getTPId()
    {
        return tpId;
    }
    
    public void updateDependentFails() throws Exception
    {
        IntegProps.getMetaClient().sendSkippedMessage(id, type, errorMsg, depSyncObject.depFailedId, depSyncObject.dependentType);
    }
    
    public void setTPId(String tpId)
    {
        this.tpId = tpId;
    } 
    
    public boolean isNew()
    {
        return oldResourceObj == null && !partiallySynced;
    }
    
    public boolean isPartiallySynced()
    {
        return partiallySynced;
    }
    
    public boolean isForceSync()
    {
        return forceSync;
    }    

    public void updateMeta(JSONObject source, String key, String val) throws Exception {
        JSONObject mapping = tpMapping.mappingMeta();
        if(mapping == null){
            mapping = new JSONObject();
        }
        mapping.put(key, val);
        tpMapping = IntegProps.getMetaClient().updateMapping(tpMapping.entityId(), tpMapping.entityType(), source, mapping, tpId);
    }
   
    public static class SyncDependentObject
    {
        public String depFailedId;
        public ThirdPartyEntityMapping.FailedDependentEntityType dependentType;
        
        public SyncDependentObject(String depFailedId,ThirdPartyEntityMapping.FailedDependentEntityType dependentType)
        {
           this.depFailedId = depFailedId;
           this.dependentType = dependentType;
        }    
    }
   
    public SyncMetaObject(ThirdPartyEntityMapping tpm) throws Exception
    {
        if(IntegProps.isStatusStopped())
        {
            throw new RuntimeException("Sync has been force stopped", null);
        }
        if(tpm==null)
        {
            throw new SyncStopException(SyncStoppedErrorCodes.MetaDataError, null, true);
        }
        this.id = tpm.entityId();
        this.type = tpm.entityType();
        ThirdPartyEntityMapping tpMap = tpm;
        //Handle delete data

        tpMapping = tpMap;
        if(tpMap.status()!=null && tpMap.status().equals(ThirdPartyEntityMapping.Status.IGNORED))
        {
            throw new RuntimeException("Object in ignore status");
        }
        if(tpMap.thirdPartyEntityId()!=null)
        {
            oldResourceObj = tpMap.oldResource();
            tpId = tpMap.thirdPartyEntityId();
            if(tpMap.status()!=null && tpMap.status().equals(ThirdPartyEntityMapping.Status.PARTIALLY_SYNCED))
            {
                partiallySynced = true;
            }
            else if(tpMap.status()!=null && tpMap.status().equals(ThirdPartyEntityMapping.Status.FORCE_SYNC))
            {
                forceSync = true;
            }
        }
        
    }    
    
    
}
