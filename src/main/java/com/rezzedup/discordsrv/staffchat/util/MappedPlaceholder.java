/*
 * The MIT License
 * Copyright © 2017-2021 RezzedUp and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
