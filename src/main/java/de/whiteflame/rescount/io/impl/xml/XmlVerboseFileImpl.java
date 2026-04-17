package de.whiteflame.rescount.io.impl.xml;

import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.io.impl.xml.model.XmlModel;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelDay;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelHour;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelTime;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelWord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class XmlVerboseFileImpl extends AbstractXmlFileImpl {
    private static final String
                    TAG_FILE  = "file",
                    TAG_WORD  = "word",
                    TAG_COUNT = "count",
                    TAG_DAY   = "day",
                    TAG_HOUR  = "hour",
                    TAG_TIME  = "time",
                    ATTRIBUTE_VALUE = "value";

    @Override
    public FileType getFileType() {
        return FileType.XML_VERBOSE;
    }

    @Override
    public void writeFile(File file, Map<String, List<LocalDateTime>> groupedTimestamps) {
        super.writeFile(file, groupedTimestamps, true);
    }

    @Override
    public Map<String, List<LocalDateTime>> readFile(File file) {
        Map<String, List<LocalDateTime>> map = new LinkedHashMap<>();

        try {
            var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList words = doc.getElementsByTagName(TAG_WORD);

            for (int i = 0; i < words.getLength(); i++) {
                Element wordEl = (Element) words.item(i);
                String word = wordEl.getAttribute(ATTRIBUTE_VALUE);

                List<LocalDateTime> timestamps = getLocalDateTimes(wordEl);

                NodeList days = wordEl.getElementsByTagName(TAG_DAY);

                for (int d = 0; d < days.getLength(); d++) {
                    Element dayEl = (Element) days.item(d);
                    String day = dayEl.getAttribute(ATTRIBUTE_VALUE);

                    NodeList hours = dayEl.getElementsByTagName(TAG_HOUR);

                    for (int h = 0; h < hours.getLength(); h++) {
                        Element hourEl = (Element) hours.item(h);
                        String hour = hourEl.getAttribute(ATTRIBUTE_VALUE);

                        NodeList times = hourEl.getElementsByTagName(TAG_TIME);

                        for (int t = 0; t < times.getLength(); t++) {
                            Element timeEl = (Element) times.item(t);
                            String time = timeEl.getAttribute(ATTRIBUTE_VALUE);

                            timestamps.add(
                                    LocalDateTime.parse(
                                            "%s %s:%s".formatted(day, hour, time),
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
                                    )
                            );
                        }
                    }
                }

                map.put(word, timestamps);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    @Override
    public boolean isType(File file) {
        return super.isType(file, TAG_FILE);
    }

    @Override
    protected void constructDocument(Document doc, XmlModel model) {
        Element root = doc.createElement(TAG_FILE);

        for (XmlModelWord m : model.words()) {

            Element word = doc.createElement(TAG_WORD);
            word.setAttribute(ATTRIBUTE_VALUE, m.value());

            Element count = doc.createElement(TAG_COUNT);
            count.setAttribute(ATTRIBUTE_VALUE, String.valueOf(m.count()));
            word.appendChild(count);

            for (XmlModelDay mDay : m.days()) {
                Element day = doc.createElement(TAG_DAY);
                day.setAttribute(ATTRIBUTE_VALUE, mDay.value());

                for (XmlModelHour mHour : mDay.hours()) {
                    Element hour = doc.createElement(TAG_HOUR);
                    hour.setAttribute(ATTRIBUTE_VALUE, mHour.value());

                    for (XmlModelTime mTime : mHour.times()) {
                        Element time = doc.createElement(TAG_TIME);
                        time.setAttribute(ATTRIBUTE_VALUE, mTime.value());
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

    private List<LocalDateTime> getLocalDateTimes(Element wordEl) {
        NodeList counts = wordEl.getElementsByTagName(TAG_COUNT);
        if (counts.getLength() != 1) {
            throw new RuntimeException(
                    "[%s] There has to be exactly one <%s> tag defined inside a <%s>!"
                            .formatted(getFileType(), TAG_COUNT, TAG_WORD)
            );
        }

        String countStr = ((Element) counts.item(0)).getAttribute(ATTRIBUTE_VALUE).trim();
        return new ArrayList<>(Integer.parseInt(countStr));
    }
}
