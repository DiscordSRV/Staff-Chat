package com.rezzedup.discordsrv.staffchat.util;

import pl.tlinkowski.annotation.basic.NullOr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class Aggregates
{
    private Aggregates() { throw new UnsupportedOperationException(); }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Result {}
    
    public static class MatchRules
    {
        private final Set<String> all = new HashSet<>();
        private final Set<String> any = new HashSet<>();
        private final Set<String> not = new HashSet<>();
        
        private MatchRules() {}
        
        public MatchRules all(String ... required)
        {
            Collections.addAll(all, required);
            return this;
        }
        
        public MatchRules any(String ... optional)
        {
            Collections.addAll(any, optional);
            return this;
        }
        
        public MatchRules not(String ... excluded)
        {
            Collections.addAll(not, excluded);
            return this;
        }
        
        public boolean matches(String name)
        {
            return (all.isEmpty() || all.stream().allMatch(name::contains))
                && (any.isEmpty() || any.stream().anyMatch(name::contains))
                && (not.isEmpty() || not.stream().noneMatch(name::contains));
        }
    }
    
    public static MatchRules matching()
    {
        return new MatchRules();
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Optional<T> cast(Class<? extends T> type, Object object)
    {
        return (type.isAssignableFrom(object.getClass())) ? Optional.of((T) object) : Optional.empty();
    }
    
    private static <T, C extends Collection<T>> C collect(Class<?> clazz, Class<? extends T> type, Supplier<C> constructor, MatchRules rules)
    {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(constructor, "constructor");
        Objects.requireNonNull(rules, "rules");
        
        C collection = Objects.requireNonNull(constructor.get(), "constructor returned null");
        
        for (Field field : clazz.getDeclaredFields())
        {
            if (!Modifier.isStatic(field.getModifiers())) { continue; }
            if (!rules.matches(field.getName())) { continue; }
            if (field.isAnnotationPresent(Result.class)) { continue; }
            
            field.setAccessible(true);
            
            try
            {
                @NullOr Object value = field.get(null);
                if (value == null) { continue; }
                
                if (value instanceof Collection)
                {
                    ((Collection<?>) value).stream()
                        .flatMap(element -> cast(type, element).stream())
                        .forEach(collection::add);
                }
                else
                {
                    cast(type, value).ifPresent(collection::add);
                }
            }
            catch (IllegalAccessException e) { e.printStackTrace(); }
        }
        
        return collection;
    }
    
    public static <T> Set<T> set(Class<?> clazz, Class<? extends T> type, Supplier<Set<T>> constructor, MatchRules rules)
    {
        return Collections.unmodifiableSet(collect(clazz, type, constructor, rules));
    }
    
    public static <T> Set<T> set(Class<?> clazz, Class<? extends T> type, MatchRules rules)
    {
        return set(clazz, type, HashSet::new, rules);
    }
}
