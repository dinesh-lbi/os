/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.i18n;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author maris
 */
public class I18N
{
    public static enum languages
    {
        english,spanish,french;
    }
    
    private Map<String,String> i18nMap = new HashMap<String, String>();
    
    public String getValue(String key)
    {
        //need to get language from integProps
        return getValue(key, languages.english);
    }
    
    public String getValue(String key,languages lang)
    {
        String value =  i18nMap.get(lang.toString()+"_"+key);
        if(value == null)
        {
            value = i18nMap.get(key);
        }
        return value;
    }
    
}
