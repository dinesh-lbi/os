/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.exception;

import com.google.api.client.http.HttpStatusCodes;

/**
 *
 * @author maris
 */
public class SyncStoppedErrorCodes
{
    public static ErrorCode MetaDataError = new ErrorCode(1000, "SYNC_META_DATA_EXCEPTION", "Sync stopped due to communication failuare occurs.", HttpStatusCodes.STATUS_CODE_OK, false);
}
