package com.rezzedup.discordsrv.staffchat.util;

import com.google.gson.reflect.TypeToken;
import pl.tlinkowski.annotation.basic.NullOr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class Aggregates
{
    private Aggregates() { throw new UnsupportedOperationException(); }
    
    public static final MatchRules ALL = matching().any();
    
    public static MatchRules matching()
    {
        return new MatchRules();
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Optional<T> cast(TypeToken<? extends T> type, Object object)
    {
        return (type.getRawType().isAssignableFrom(object.getClass())) ? Optional.of((T) object) : Optional.empty();
    }
    
    private static <T, C extends Collection<T>> C collect(Class<?> clazz, TypeToken<? extends T> type, Supplier<C> constructor, MatchRules rules)
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
    
    public static <T> Set<T> set(Class<?> clazz, TypeToken<? extends T> type, Supplier<Set<T>> constructor, MatchRules rules)
    {
        return Collections.unmodifiableSet(collect(clazz, type, constructor, rules));
    }
    
    public static <T> Set<T> set(Class<?> clazz, TypeToken<? extends T> type, MatchRules rules)
    {
        return set(clazz, type, HashSet::new, rules);
    }
    
    public static <T> Set<T> set(Class<?> clazz, TypeToken<? extends T> type)
    {
        return set(clazz, type, ALL);
    }
    
    public static <T> List<T> list(Class<?> clazz, TypeToken<? extends T> type, Supplier<List<T>> constructor, MatchRules rules)
    {
        return Collections.unmodifiableList(collect(clazz, type, constructor, rules));
    }
    
    public static <T> List<T> list(Class<?> clazz, TypeToken<? extends T> type, MatchRules rules)
    {
        return list(clazz, type, ArrayList::new, rules);
    }
    
    public static <T> List<T> list(Class<?> clazz, TypeToken<? extends T> type)
    {
        return list(clazz, type, ALL);
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Result {}
    
    public static class MatchRules
    {
        private final Set<String> all;
        private final Set<String> any;
        private final Set<String> not;
    
        private MatchRules(Set<String> all, Set<String> any, Set<String> not)
        {
            this.all = Set.copyOf(all);
            this.any = Set.copyOf(any);
            this.not = Set.copyOf(not);
        }
        
        private MatchRules()
        {
            this(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
        }
        
        public MatchRules all(String ... required)
        {
            Set<String> allModified = new HashSet<>(all);
            Collections.addAll(allModified, required);
            return new MatchRules(allModified, any, not);
        }
        
        public MatchRules any(String ... optional)
        {
            Set<String> anyModified = new HashSet<>(any);
            Collections.addAll(anyModified, optional);
            return new MatchRules(all, anyModified, not);
        }
        
        public MatchRules not(String ... excluded)
        {
            Set<String> notModified = new HashSet<>(not);
            Collections.addAll(notModified, excluded);
            return new MatchRules(all, any, notModified);
        }
        
        public boolean matches(String name)
        {
            return (all.isEmpty() || all.stream().allMatch(name::contains))
                && (any.isEmpty() || any.stream().anyMatch(name::contains))
                && (not.isEmpty() || not.stream().noneMatch(name::contains));
        }
    }
}
