/*
 * Copyright (c) 2017 Chargebee Inc
 * All Rights Reserved.
 */
package com.chargebee.framework.data;

/**
 *
 * @author maris
 */
public class EscapeUtil
{
    
    public static final  char[] SPECIALCHARS_SQL = new char[]{'\'', '%' , '\\' , '\"'};
    public static final String[] REPL_SPECIALCHARS_SQL =  new String[]{"\\\'", "\\%" , "\\\\\\\\" , "\\\""};
    
    
    public static  String replaceCharExcludeNumbersFromCheck(Object str 
                                                             , char[] matchChar, String[] replaceStr)
    {
        if (str == null){ return null;}
        if ((str instanceof Number) || (str instanceof Boolean)){ return String.valueOf(str);}
        return replaceChar(str,matchChar,replaceStr);
    }
    
    public static String escSplCharsInQry(Object str) 
    {
        return replaceCharExcludeNumbersFromCheck(str , 
                                                  SPECIALCHARS_SQL, REPL_SPECIALCHARS_SQL);
    }

    
    public static  String replaceChar(Object str , char[] matchChar, String[] replaceStr)
    {
        if (str == null){ return null;}
        String strToEsc = String.valueOf(str);
        
        StringBuilder sbf = null;
        int length = strToEsc.length();
        int splSize = matchChar.length;
        loop :for (int i=0; i<length; i++)
        {
            char ch = strToEsc.charAt(i);
            for (int j=0; j<splSize; j++)
            {
                if(matchChar[j] == ch)
                {
                    if (sbf == null)
                    {
                        sbf = new StringBuilder(strToEsc.length() + 10);
                        sbf.append(strToEsc.substring(0,i));
                    }
                    sbf.append(replaceStr[j]);
                    continue loop;
                }
            }
            if (sbf != null)
            {
                sbf.append(ch);
            }
        }
        return (sbf != null)? sbf.toString() : strToEsc;
    }
    
}
