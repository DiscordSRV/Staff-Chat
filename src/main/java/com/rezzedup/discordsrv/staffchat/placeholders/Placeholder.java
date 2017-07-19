package com.rezzedup.discordsrv.staffchat.placeholders;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Placeholder
{
    Pattern PATTERN = Pattern.compile("%.+?%");
    
    String get(String placeholder);
    
    default String update(String message)
    {
        Matcher matcher = PATTERN.matcher(message);
        
        while (matcher.find())
        {
            String match = matcher.group();
            String value = get(match.replace("%", ""));
            
            if (value == null || value.isEmpty())
            {
                continue;
            }
            
            message = message.replace(match, value);
        }
        
        return message;
    }
}
