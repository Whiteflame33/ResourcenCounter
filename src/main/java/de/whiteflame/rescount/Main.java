package de.whiteflame.rescount;

import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.io.FileConstants;
import de.whiteflame.rescount.io.FileHandler;
import de.whiteflame.rescount.io.impl.TextFileImpl;
import de.whiteflame.rescount.io.impl.xml.XmlSlimFileImpl;
import de.whiteflame.rescount.io.impl.xml.XmlVerboseFileImpl;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.swing.*;

public class Main {
    public static final String WORD_RESOURCE = "Räsorße";

    static int counter = 0;
    static Map<String, List<LocalDateTime>> entries = new LinkedHashMap<>();

    static FileHandler fileHandler = new FileHandler();

    static {
        fileHandler.registerReader(FileType.TEXT, new TextFileImpl());
        fileHandler.registerReader(FileType.XML_VERBOSE, new XmlVerboseFileImpl());
        fileHandler.registerReader(FileType.XML_SLIM, new XmlSlimFileImpl());

        fileHandler.registerWriter(FileType.TEXT, new TextFileImpl());
        fileHandler.registerWriter(FileType.XML_VERBOSE, new XmlVerboseFileImpl());
        fileHandler.registerWriter(FileType.XML_SLIM, new XmlSlimFileImpl());
    }

    static void main() {
        load();

        counter = entries.getOrDefault(WORD_RESOURCE, List.of()).size();

        JFrame frame = new JFrame();
        frame.setSize(300, 80);

        JPanel p = new JPanel();
        frame.add(p);

        p.add(new JLabel(WORD_RESOURCE + ":"));

        JLabel disp = new JLabel(String.valueOf(counter));
        p.add(disp);

        JButton a = new JButton("+1");
        p.add(a);

        a.addActionListener(e -> {
            ++counter;

            entries
                    .computeIfAbsent(WORD_RESOURCE, k -> new ArrayList<>())
                    .add(LocalDateTime.now());

            disp.setText(String.valueOf(counter));
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(Main::safe));

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    static void load() {
        Map<String, List<LocalDateTime>> loaded = null;

        if (FileConstants.TEXT_FILE.exists()) {
            loaded = fileHandler.load(FileConstants.TEXT_FILE);
            FileConstants.TEXT_FILE.delete();
        } else if (FileConstants.XML_FILE.exists()) {
            loaded = fileHandler.load(FileConstants.XML_FILE);
        }

        if (loaded != null) {
            entries.clear();
            entries.putAll(loaded);
        }
    }

    static void safe() {
        try {
            fileHandler.save(entries, FileConstants.XML_FILE, FileType.XML_SLIM);
            System.out.println("Finished writing!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

