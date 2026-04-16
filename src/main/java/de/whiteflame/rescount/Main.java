package de.whiteflame.rescount;

import java.awt.Component;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.swing.*;

public class Main {
    public static String WORD_RESOURCE = "Räsorße";

    private static final String USER_PATH = System.getProperty("user.home") + File.separator;
    static final File OLD_FILE_PATH = new File(USER_PATH + "wissmann.txt");
    static final File FILE_PATH = new File(USER_PATH + "wissmann.xml");

    static int counter = 0;

    static Map<String, List<LocalDateTime>> entries = new LinkedHashMap<>();

    public static void main(String[] args) {
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

        frame.setLocationRelativeTo((Component) null);
        frame.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(Main::safe));

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    static void load() {
        if (OLD_FILE_PATH.exists() && !FILE_PATH.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(OLD_FILE_PATH))) {

                List<String> lines = r.lines().toList();
                int count = Integer.parseInt(lines.getFirst().split("count=")[1]);

                List<LocalDateTime> migrated = new ArrayList<>(count);

                for (int i = 1; i < lines.size(); ++i) {
                    if (lines.get(i).isEmpty()) continue;
                    migrated.add(LocalDateTime.parse(lines.get(i)));
                }

                entries.put(WORD_RESOURCE, migrated);

                FILE_PATH.createNewFile();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (FILE_PATH.exists()) {
            try {
                entries.clear();
                entries.putAll(EffWriter.loadFile(FILE_PATH));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void safe() {
        try {
            if (!FILE_PATH.exists())
                FILE_PATH.createNewFile();

            EffWriter.saveFile(FILE_PATH, entries);

            System.out.println("Finished writing");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Finished saving method");
        }
    }
}

