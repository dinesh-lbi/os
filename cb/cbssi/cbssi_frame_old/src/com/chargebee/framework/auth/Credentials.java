/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.auth;

import java.util.Map;

/**
 *
 * @author maris
 */
public interface Credentials 
{
    public Map getCredentials();
    public boolean isLiveCredential();
    public boolean isTestCredential();
    public boolean isKeyRegenNeeds();
    public void reGenCred();
}
