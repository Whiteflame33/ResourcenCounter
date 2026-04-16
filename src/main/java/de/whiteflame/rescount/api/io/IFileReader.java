package de.whiteflame.rescount.api.io;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IFileReader {
    FileType getFileType();

    Map<String, List<LocalDateTime>> readFile(File file);
}
