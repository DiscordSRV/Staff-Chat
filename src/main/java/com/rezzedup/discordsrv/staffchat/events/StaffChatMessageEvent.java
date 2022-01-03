/*
 * The MIT License
 * Copyright © 2017-2022 RezzedUp and Contributors
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
package com.rezzedup.discordsrv.staffchat.events;

import com.rezzedup.discordsrv.staffchat.ChatService;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.util.Objects;

public abstract class StaffChatMessageEvent<A, M> extends Event implements Cancellable
{
    private final A author;
    private final M message;
    private String text;
    
    public StaffChatMessageEvent(A author, M message, String text)
    {
        this.author = Objects.requireNonNull(author, "author");
        this.message = Objects.requireNonNull(message, "message");
        this.text = Objects.requireNonNull(text, "text");
    }
    
    public abstract ChatService getSource();
    
    public abstract ChatService getDestination();
    
    public final A getAuthor() { return author; }
    
    public final M getMessage() { return message; }
    
    public final String getText() { return text; }
    
    public final void setText(String text) { this.text = Objects.requireNonNull(text, "text"); }
    
    //
    //  - - - Cancellable boilerplate - - -
    //
    
    private boolean isCancelled = false;
    
    @Override
    public final boolean isCancelled() { return isCancelled; }
    
    @Override
    public final void setCancelled(boolean cancelled) { isCancelled = cancelled; }
}
