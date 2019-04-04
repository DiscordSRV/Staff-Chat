package com.rezzedup.discordsrv.staffchat.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappedPlaceholder
{
    public static Pattern PATTERN = Pattern.compile("%.+?%");
    
    protected final Map<String, Supplier> placeholders = new HashMap<>();
    
    public String get(String placeholder)
    {
        if (Strings.isEmptyOrNull(placeholder)) { return ""; }
        
        Supplier supplier = placeholders.get(placeholder.toLowerCase());
        
        if (supplier == null) { return ""; }
        
        Object result = null;
        
        try { result = supplier.get(); }
        catch (Exception e) { e.printStackTrace(); }
        
        return (result == null) ? "" : String.valueOf(result);
    }
    
    public String update(String message)
    {
        Matcher matcher = PATTERN.matcher(message);
        
        while (matcher.find())
        {
            String match = matcher.group();
            String value = get(match.replace("%", ""));
            
            if (Strings.isEmptyOrNull(value)) { continue; }
            
            message = message.replace(match, value);
        }
        
        return message;
    }
    
    public Putter map(String ... placeholders)
    {
        if (placeholders == null || placeholders.length <= 0)
        {
            throw new IllegalArgumentException("Invalid placeholders: null or empty");
        }
        return new Putter(Arrays.asList(placeholders));
    }
    
    public void inherit(MappedPlaceholder from) { placeholders.putAll(from.placeholders); }
    
    public class Putter
    {
        private final List<String> aliases;
        
        private Putter(List<String> aliases) { this.aliases = aliases; }
        
        public void to(Supplier supplier)
        {
            MappedPlaceholder instance = MappedPlaceholder.this;
            
            if (supplier == null) { return; }
            
            for (String alias : aliases)
            {
                if (Strings.isEmptyOrNull(alias)) { continue; }
                
                String placeholder = alias.toLowerCase();
                instance.placeholders.put(placeholder, supplier);
            }
        }
    }
}
