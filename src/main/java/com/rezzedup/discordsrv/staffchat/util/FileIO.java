package com.rezzedup.discordsrv.staffchat.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class FileIO
{
    private FileIO() { throw new UnsupportedOperationException(); }
    
    public static void write(Path filePath, String contents, ExceptionHandler<? super IOException> exceptions)
    {
        try { Files.writeString(filePath, contents); }
        catch (IOException e) { exceptions.handle(e); }
    }
    
    public static void backup(Path existingFilePath, Path backupFilePath, ExceptionHandler<? super IOException> exceptions)
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
        catch (IOException e) { exceptions.handle(e); }
    }
}
