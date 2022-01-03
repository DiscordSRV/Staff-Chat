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
package com.rezzedup.discordsrv.staffchat.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Consumer;

public class FileIO
{
    private FileIO() { throw new UnsupportedOperationException(); }
    
    public static void write(Path filePath, String contents, Consumer<? super IOException> exceptions)
    {
        try { Files.writeString(filePath, contents); }
        catch (IOException e) { exceptions.accept(e); }
    }
    
    public static void backup(Path existingFilePath, Path backupFilePath, Consumer<? super IOException> exceptions)
    {
        // Nothing to back up.
        if (!Files.isRegularFile(existingFilePath)) { return; }
        
        try
        {
            Path backupDirectory = backupFilePath.getParent();
            if (!Files.isDirectory(backupDirectory)) { Files.createDirectories(backupDirectory); }
            
            String backupFileName = backupFilePath.getFileName().toString();
            int lastDot = backupFileName.lastIndexOf('.');
            String name = (lastDot > 0) ? backupFileName.substring(0, lastDot) : "";
            String extension = (lastDot > 0) ? backupFileName.substring(lastDot) : "";
            
            for (int i = 1 ;; i++)
            {
                String attemptedName = name + ".backup_" + LocalDate.now() + "_" + i + extension;
                Path attemptedBackupFilePath = backupDirectory.resolve(attemptedName);
                
                if (Files.isRegularFile(attemptedBackupFilePath)) { continue; }
                
                Files.move(existingFilePath, attemptedBackupFilePath);
                return;
            }
        }
        catch (IOException e) { exceptions.accept(e); }
    }
    
    public static void backup(Path existingFilePath, Path backupFilePath)
    {
        backup(existingFilePath, backupFilePath, e -> { throw new RuntimeException(e); });
    }
}
