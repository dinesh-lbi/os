/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.auth;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author maris
 */
public class CBCredencials implements Credentials
{
    
    private final Map<String,String> credProps = new HashMap<>(); 
    public static final String CB_AUTH_TOKEN = "cb.auth.token";
    public static final String CB_SITE_NAME = "cb.site.name";
    private final boolean isLiveCredential;

    @Override
    public Map getCredentials()
    {
        return credProps;
    }
    
    public CBCredencials(String authToken,String siteName,boolean isLive)
    {
        
        credProps.put(CB_AUTH_TOKEN, authToken);
        credProps.put(CB_SITE_NAME,siteName);
        this.isLiveCredential = isLive;
    }

    @Override
    public boolean isLiveCredential() 
    {
        return isLiveCredential;
    }

    @Override
    public boolean isKeyRegenNeeds() 
    {
        return false;
    }

    @Override
    public void reGenCred() 
    {
        //Not needed
    }

    @Override
    public boolean isTestCredential() 
    {
        return !isLiveCredential;
    }
}
