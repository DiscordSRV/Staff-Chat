package com.rezzedup.discordsrv.staffchat.util;

import pl.tlinkowski.annotation.basic.NullOr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappedPlaceholder
{
    public static Pattern PATTERN = Pattern.compile("%.+?%");
    
    protected final Map<String, Supplier<?>> placeholders = new HashMap<>();
    
    public String get(String placeholder)
    {
        if (Strings.isEmptyOrNull(placeholder)) { return ""; }
        
        Supplier<?> supplier = placeholders.get(placeholder.toLowerCase());
        
        if (supplier == null) { return ""; }
        
        @NullOr Object result = null;
        
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
        Objects.requireNonNull(placeholders, "placeholders");
        if (placeholders.length <= 0) { throw new IllegalArgumentException("Empty placeholders array"); }
        return new Putter(Arrays.asList(placeholders));
    }
    
    public void inherit(MappedPlaceholder from) { placeholders.putAll(from.placeholders); }
    
    public class Putter
    {
        private final List<String> aliases;
        
        private Putter(List<String> aliases) { this.aliases = aliases; }
        
        public void to(Supplier<?> supplier)
        {
            Objects.requireNonNull(supplier, "supplier");
            
            for (String alias : aliases)
            {
                if (Strings.isEmptyOrNull(alias)) { continue; }
                
                String placeholder = alias.toLowerCase();
                MappedPlaceholder.this.placeholders.put(placeholder, supplier);
            }
        }
    }
}
