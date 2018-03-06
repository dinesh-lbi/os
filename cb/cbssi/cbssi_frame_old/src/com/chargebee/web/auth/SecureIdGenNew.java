/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.web.auth;


import com.chargebee.framework.env.*;
import com.chargebee.logging.*;
import org.apache.catalina.util.*;
import org.apache.commons.codec.binary.Base64;

public class SecureIdGenNew {

    private static final SessionIdGeneratorBase gen = createGen();//TODO: validate if SessionId generator is multi threaded.- Uses queue et.al so should be.

    private static SessionIdGeneratorBase createGen() {
        SessionIdGeneratorBase g = new StandardSessionIdGenerator();
        g.setSecureRandomClass("java.security.SecureRandom");//TODO: Ensure it uses NativePRNG.
        g.setSessionIdLength(24);
        return g;
    }

    public static String genToken() {
        long start = System.currentTimeMillis();
        byte randomBytes[] = new byte[24];
        gen.getRandomBytes(randomBytes);
        String tok = base64To62(Base64.encodeBase64URLSafeString(randomBytes));
        KVL.callTime("gen_token", start);
        return Env.prefixDev(tok);
    }

    private static String base64To62(String base64) {//Cannot be conv back as c is not encoded.
        StringBuilder buf = new StringBuilder(base64.length() + 10);
        for (int i = 0; i < base64.length(); i++) {
            char ch = base64.charAt(i);
            switch (ch) {
                case '-':
                    buf.append("cd");
                    break;
                case '_':
                    buf.append("cu");
                    break;
                default:
                    buf.append(ch);
            }
        }
        return buf.toString();
    }

}

