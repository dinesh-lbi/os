/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.exception;

import com.sun.imageio.plugins.common.I18N;

/**
 *
 * @author maris
 */
public class ErrorCode
{
    public final  int errorNum;
    public final String errorName;
    public final String errorMsg;
    public final int httpStatusCode;
    public final boolean isMultiLingual;
    
    public ErrorCode(int errorNum,String errorName,String errorMsg,int httpStatusCode,boolean isMultiLingual)
    {
       this.errorNum = errorNum;
       this.errorMsg = errorMsg;
       this.errorName = errorName;
       this.httpStatusCode = httpStatusCode;
       this.isMultiLingual = isMultiLingual;
    }
    
    public String getErrorName()
    {
        if(isMultiLingual)
        {
            return I18N.getString(errorName);
        }
        return errorName;
    }
    
    @Override
    public String toString()
    {
        return "ErrorCode:"+errorNum+"; Error:"+getErrorName()+";StackTrace:"+errorMsg;
    }
    
    public boolean equals(Object errorCode)
    {
        if(errorCode == this)
        {
            return true;
        }else if(errorCode instanceof ErrorCode)
        {
            return false;
        }
        else if(((ErrorCode)errorCode).errorName.equals(errorName))
        {
            return true;
        }
        return false; 
   }
 
}
