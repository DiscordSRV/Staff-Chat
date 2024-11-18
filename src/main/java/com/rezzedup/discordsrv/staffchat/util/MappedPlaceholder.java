/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class MappedPlaceholder {
	public static Pattern PATTERN = Pattern.compile("%(.+?)%");
	
	protected final Map<String, Supplier<?>> placeholders = new HashMap<>();
	
	public String get(@NullOr String placeholder) {
		if (Strings.isEmptyOrNull(placeholder)) {
			return "";
		}
		
		Supplier<?> supplier = placeholders.get(placeholder.toLowerCase(Locale.ROOT));
		if (supplier == null) {
			return "";
		}
		
		@NullOr Object result = null;
		try {
			result = supplier.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return (result == null) ? "" : String.valueOf(result);
	}
	
	private static String escape(String literal) {
		return literal.replace("\\", "\\\\").replace("$", "\\$");
	}
	
	public String update(String message) {
		return PATTERN.matcher(message).replaceAll(mr -> {
			String value = get(mr.group(1));
			return escape((value.isEmpty()) ? mr.group() : value);
		});
	}
	
	public Putter map(String... placeholders) {
		Objects.requireNonNull(placeholders, "placeholders");
		if (placeholders.length <= 0) {
			throw new IllegalArgumentException("Empty placeholders array");
		}
		return new Putter(placeholders);
	}
	
	public void inherit(MappedPlaceholder from) {
		placeholders.putAll(from.placeholders);
	}
	
	public class Putter {
		private final String[] aliases;
		
		private Putter(String[] aliases) {
			this.aliases = aliases;
		}
		
		public void to(Supplier<?> supplier) {
			Objects.requireNonNull(supplier, "supplier");
			
			for (String alias : aliases) {
				if (Strings.isEmptyOrNull(alias)) {
					continue;
				}
				placeholders.put(alias.toLowerCase(Locale.ROOT), supplier);
			}
		}
	}
}
