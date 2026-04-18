package de.whiteflame.rescount.io;

import de.whiteflame.rescount.config.GlobalConfig;

import java.io.File;

public final class FileConstants {
    private FileConstants() {}

    public static File getTextFile() {
        return GlobalConfig.getDataFile("wissmann.txt");
    }

    public static File getXmlFile() {
        return GlobalConfig.getDataFile("wissmann.xml");
    }

    public static File getDataFile() {
        return GlobalConfig.getDataFile("wissmann.data");
    }

    public static File getCompressedDataFile() {
        return GlobalConfig.getDataFile("wissmann.z.data");
    }
}
