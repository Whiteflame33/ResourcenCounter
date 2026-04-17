package de.whiteflame.rescount.api.io;

public enum FileType {
    TEXT("txt"),
    XML_VERBOSE("xml"),
    XML_SLIM("xml"),
    BYTE_1("data"),
    BYTE_2("dataz"),
    UNKNOWN(null);

    private final String fileExtension;

    FileType(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
