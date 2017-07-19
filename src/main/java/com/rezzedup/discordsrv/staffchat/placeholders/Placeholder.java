package com.rezzedup.discordsrv.staffchat.placeholders;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Placeholder
{
    Pattern PATTERN = Pattern.compile("%.+?%");
    
    static boolean isValid(String result)
    {
        return result != null && !result.isEmpty();
    }
    
    String get(String placeholder);
    
    default String update(String message)
    {
        Matcher matcher = PATTERN.matcher(message);
        
        while (matcher.find())
        {
            String match = matcher.group();
            String value = get(match.replace("%", ""));
            
            if (!isValid(value))
            {
                continue;
            }
            
            message = message.replace(match, value);
        }
        
        return message;
    }
}
