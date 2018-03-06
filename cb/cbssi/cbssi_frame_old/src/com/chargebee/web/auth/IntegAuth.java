/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.web.auth;

/*
 * Copyright (c) 2011 chargebee.com
 * All Rights Reserved.
 */

import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.metrics.*;
import com.amazonaws.services.securitytoken.*;
import com.amazonaws.services.securitytoken.model.*;
import com.amazonaws.services.sns.*;
import com.amazonaws.services.sns.model.*;
import com.chargebee.framework.env.*;
import com.chargebee.framework.util.*;
import com.chargebee.framework.web.*;
import com.chargebee.web.Invoker;
import java.util.*;
import java.util.concurrent.*;
import javax.servlet.http.*;


public class IntegAuth {
    
    public static final Map<String,UserSession> sessions = new ConcurrentHashMap();
    
    
    public static boolean isActiveSession(String token){
            UserSession session = sessions.get(token);
        if(session == null){
            return false;
        }
        if(session.isValid()){
            return true;
        }else{
            sessions.remove(token);
            return false;
        }
    }
    
    public static void init(){
        Thread th = new Thread(() -> {
            while(true){
                if(Invoker.stopped){
                    return;
                }
                try {
                    Thread.sleep(Env.longProp("auth.sessions.clear", TimeUtils.MIN));
                } catch (Exception e) {
                    ErrorUtil.logError(e);
                    return;
                }
                clearSessions();
            }
        });
    }
    
    public static void login(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        
        String awsAccessKey = req.getParameter("aws_access_key");
        String awsSecretKey = req.getParameter("aws_secret_key");
        String user = req.getParameter("aws_user");
        String mfaToken = req.getParameter("aws_mfa_token");
        String loginReason = req.getParameter("login_reason");
        
        
        String awsAccId = Env.reqStrProp("aws.cb-auth.account.id");
        String setupType = Env.reqStrProp("aws.setup.type");
        
        AWSCredentials cred = new BasicAWSCredentials(awsAccessKey,awsSecretKey);
        GetSessionTokenRequest r = new GetSessionTokenRequest();
        r.setDurationSeconds(3600);//Will be overriden at the policy level.
        r.setSerialNumber("arn:aws:iam::" + awsAccId + ":mfa/" + user);
        r.setTokenCode(mfaToken);
        r.setRequestMetricCollector(RequestMetricCollector.NONE);
        
        AWSSecurityTokenServiceClient client = new AWSSecurityTokenServiceClient(cred); 
        GetSessionTokenResult sessionToken;
        try{
            sessionToken = client.getSessionToken(r);        
            Credentials c = sessionToken.getCredentials();
            AmazonSNSClient sns = new AmazonSNSClient(new BasicSessionCredentials(c.getAccessKeyId(), 
                c.getSecretAccessKey(), c.getSessionToken()));
            String loginTopic = "cb-auth-" + setupType + "-integration";
            String subject = "[Admin Login] - quickbooks server - " + CBHttpUtil.getClientIp(req) + " - " + user;
            String message = "Client Ip : " + CBHttpUtil.getClientIp(req)
                    + "\nUser email :" + user
                    + "\nLogin At :" + DateUtils.now()
                    + "\nReason :" + loginReason;
            PublishRequest request = new PublishRequest("arn:aws:sns:us-east-1:" + awsAccId + ":" + loginTopic, message, subject);
            System.out.println("Checking auth... ");
            PublishResult res = sns.publish(request);
        } catch (AmazonServiceException e) {
            throw WebErrorCodes.INVALID_CREDENTIAL.err();
        }
        
        String token = SecureIdGenNew.genToken();
        CBCookieUtil.addSessionCookie("cb_integserver_token", token, req, resp);
        sessions.put(token,new UserSession(user));
        resp.sendRedirect("/");
    }

    public static boolean isActiveSession(HttpServletRequest req) {
        String cookie = CBCookieUtil.getCookie("cb_integserver_token", req);
        return (cookie != null && isActiveSession(cookie));
    }
    
    private static void clearSessions(){
        List<String> sessionsToRemove = new ArrayList<>();
        for (Map.Entry<String, UserSession> entry : sessions.entrySet()) {
            String token = entry.getKey();
            UserSession session = entry.getValue();
            if(!session.isValid()){
               sessionsToRemove.add(token);
            }
        }
        sessions.entrySet().removeAll(sessionsToRemove);
    }

    public static void logout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String cookie = CBCookieUtil.getCookie("cb_integserver_token", req);
        if(cookie != null){
            sessions.remove(cookie);
            CBCookieUtil.deleteCookie("cb_integserver_token", req, resp);
        }
        resp.sendRedirect("/login");
    }

    
    public static class UserSession{
        String userName;
        long sessionCreatedTime;

        public UserSession(String userName) {
            this.userName = userName;
            sessionCreatedTime = System.currentTimeMillis();
        }
         
        public boolean isValid(){
           return TimeUtils.isStillWithinPeriod(sessionCreatedTime, 
                TimeUtils.HOUR * 2); 
        }
    }
}

