package de.whiteflame.rescount.io;

import java.io.File;

public final class FileConstants {
    private FileConstants() {}

    public static final String
            USER_HOME = System.getProperty("user.home"),
            F_USER_HOME = USER_HOME + File.separator;

    public static final File
            TEXT_FILE = new File(F_USER_HOME+ "wissmann.txt"),
            XML_FILE = new File(F_USER_HOME + "wissmann.xml"),
            DATA_FILE = new File(F_USER_HOME + "wissmann.data"),
            COMPRESSED_DATA_FILE = new File(F_USER_HOME + "wissmann.z.data");
}
