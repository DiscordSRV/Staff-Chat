package com.rezzedup.discordsrv.staffchat.placeholders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MappedPlaceholder implements Placeholder
{
    protected final Map<String, Supplier> placeholders = new HashMap<>();
    
    @Override
    public String get(String placeholder)
    {
        if (placeholder == null || placeholder.isEmpty())
        {
            return "";
        }
        
        Supplier supplier = placeholders.get(placeholder.toLowerCase());
        
        if (supplier == null)
        {
            return "";
        }
        
        Object result = null;
        
        try
        {
            result = supplier.get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return (result == null) ? "" : String.valueOf(result);
    }
    
    public Putter map(String ... placeholders)
    {
        if (placeholders == null || placeholders.length <= 0)
        {
            throw new IllegalArgumentException("Invalid placeholders: null or empty");
        }
        return new Putter(Arrays.asList(placeholders));
    }
    
    public void inherit(MappedPlaceholder from)
    {
        placeholders.putAll(from.placeholders);
    }
    
    public class Putter
    {
        private final List<String> aliases;
        
        private Putter(List<String> aliases)
        {
            this.aliases = aliases;
        }
        
        public void to(Supplier supplier)
        {
            MappedPlaceholder instance = MappedPlaceholder.this;
            
            if (supplier == null)
            {
                return;
            }
            
            for (String alias : aliases)
            {
                if (alias == null || alias.isEmpty())
                {
                    continue;
                }
                
                String placeholder = alias.toLowerCase();
                
                instance.placeholders.put(placeholder, supplier);
            }
        }
    }
}
