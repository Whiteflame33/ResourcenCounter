package de.whiteflame.rescount;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EffWriter {

    private static final DateTimeFormatter dtf =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    private static final String
            TAG_FILE = "file",
            TAG_WORD = "word",
            TAG_COUNT = "count",
            TAG_DAY = "day",
            TAG_HOUR = "hour",
            TAG_TIME = "time";

    private static final String ATTR_VALUE = "value";

    // =========================================================
    // SAVE
    // =========================================================

    public static void saveFile(File file, Map<String, List<LocalDateTime>> wordMap) throws Exception {

        Set<ModelWord> words = new LinkedHashSet<>();

        for (var entry : wordMap.entrySet()) {
            words.add(constructModel(entry.getKey(), entry.getValue()));
        }

        Model model = new Model(words);

        var factory = DocumentBuilderFactory.newInstance();
        var builder = factory.newDocumentBuilder();

        Document doc = builder.newDocument();

        constructDocument(doc, model);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            transformer.transform(new DOMSource(doc), new StreamResult(os));
        }
    }

    private static void constructDocument(Document doc, Model model) {
        Element root = doc.createElement(TAG_FILE);

        for (ModelWord m : model.words()) {

            Element word = doc.createElement(TAG_WORD);
            word.setAttribute(ATTR_VALUE, m.word());

            Element count = doc.createElement(TAG_COUNT);
            count.setAttribute(ATTR_VALUE, String.valueOf(m.count()));
            word.appendChild(count);

            for (ModelDay mDay : m.days()) {
                Element day = doc.createElement(TAG_DAY);
                day.setAttribute(ATTR_VALUE, mDay.value());

                for (ModelHour mHour : mDay.hours()) {
                    Element hour = doc.createElement(TAG_HOUR);
                    hour.setAttribute(ATTR_VALUE, mHour.value());

                    for (ModelTime mTime : mHour.times()) {
                        Element time = doc.createElement(TAG_TIME);
                        time.setAttribute(ATTR_VALUE, mTime.value());
                        hour.appendChild(time);
                    }

                    day.appendChild(hour);
                }

                word.appendChild(day);
            }

            root.appendChild(word);
        }

        doc.appendChild(root);
    }

    // =========================================================
    // LOAD
    // =========================================================

    public static Map<String, List<LocalDateTime>> loadFile(File file) throws Exception {

        var factory = DocumentBuilderFactory.newInstance();
        var builder = factory.newDocumentBuilder();

        Document doc = builder.parse(file);

        Map<String, List<LocalDateTime>> result = new LinkedHashMap<>();

        NodeList wordList = doc.getElementsByTagName(TAG_WORD);

        for (int w = 0; w < wordList.getLength(); w++) {

            Element wordEl = (Element) wordList.item(w);
            String wordValue = wordEl.getAttribute(ATTR_VALUE);

            List<LocalDateTime> timestamps = new ArrayList<>();

            NodeList dayList = wordEl.getElementsByTagName(TAG_DAY);

            for (int i = 0; i < dayList.getLength(); i++) {
                Element dayEl = (Element) dayList.item(i);
                String dayValue = dayEl.getAttribute(ATTR_VALUE);

                NodeList hourList = dayEl.getElementsByTagName(TAG_HOUR);

                for (int j = 0; j < hourList.getLength(); j++) {
                    Element hourEl = (Element) hourList.item(j);
                    String hourValue = hourEl.getAttribute(ATTR_VALUE);

                    NodeList timeList = hourEl.getElementsByTagName(TAG_TIME);

                    for (int k = 0; k < timeList.getLength(); k++) {
                        String timeValue = ((Element) timeList.item(k)).getAttribute(ATTR_VALUE);

                        timestamps.add(
                                LocalDateTime.parse(
                                        "%s %s:%s".formatted(dayValue, hourValue, timeValue),
                                        dtf
                                )
                        );
                    }
                }
            }

            result.put(wordValue, timestamps);
        }

        return result;
    }

    // =========================================================
    // MODEL CONSTRUCTION
    // =========================================================

    private static ModelWord constructModel(String word, List<LocalDateTime> timestamps) {

        Map<String, Map<String, Set<String>>> grouped = new LinkedHashMap<>();

        for (var timestamp : timestamps) {
            String day = timestamp.toLocalDate().toString();
            String hour = "%02d".formatted(timestamp.getHour());
            String time = "%02d:%02d:%03d".formatted(
                    timestamp.getMinute(),
                    timestamp.getSecond(),
                    timestamp.getNano() / 1_000_000
            );

            grouped
                    .computeIfAbsent(day, d -> new LinkedHashMap<>())
                    .computeIfAbsent(hour, h -> new LinkedHashSet<>())
                    .add(time);
        }

        Set<ModelDay> modelDays = new LinkedHashSet<>();

        for (var dayEntry : grouped.entrySet()) {

            Set<ModelHour> modelHours = new LinkedHashSet<>();

            for (var hourEntry : dayEntry.getValue().entrySet()) {

                Set<ModelTime> modelTimes = new LinkedHashSet<>();

                for (String timeEntry : hourEntry.getValue()) {
                    modelTimes.add(new ModelTime(timeEntry));
                }

                modelHours.add(new ModelHour(hourEntry.getKey(), modelTimes));
            }

            modelDays.add(new ModelDay(dayEntry.getKey(), modelHours));
        }

        return new ModelWord(word, timestamps.size(), modelDays);
    }

    // =========================================================
    // MODEL
    // =========================================================

    record Model(Set<ModelWord> words) {}

    record ModelWord(String word, int count, Set<ModelDay> days) {}

    record ModelDay(String value, Set<ModelHour> hours) {}

    record ModelHour(String value, Set<ModelTime> times) {}

    record ModelTime(String value) {}
}
