package de.whiteflame.rescount.api.io;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IFileWriter {
    FileType getFileType();

   void writeFile(File file, Map<String, List<LocalDateTime>> groupedTimestamps);
}
