package de.whiteflame.rescount.io.impl.xml;

import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.io.impl.xml.model.XmlModel;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelDay;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelHour;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelWord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class XmlSlimFileImpl extends AbstractXmlFileImpl {
    private static final String
                    TAG_FILE  = "f",
                    TAG_WORD  = "w",
                    TAG_DAY   = "d",
                    TAG_HOUR  = "h",
                    ATTRIBUTE_VALUE = "v",
                    ATTRIBUTE_COUNT = "c";

    @Override
    public FileType getFileType() {
        return FileType.XML_VERBOSE;
    }

    @Override
    public void writeFile(File file, Map<String, List<LocalDateTime>> groupedTimestamps) {
        super.writeFile(file, groupedTimestamps, false);
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
                String count = wordEl.getAttribute(ATTRIBUTE_COUNT);

                List<LocalDateTime> timestamps = new ArrayList<>(Integer.parseInt(count));

                NodeList days = wordEl.getElementsByTagName(TAG_DAY);

                for (int d = 0; d < days.getLength(); d++) {
                    Element dayEl = (Element) days.item(d);
                    String day = dayEl.getAttribute(ATTRIBUTE_VALUE).trim();
                    day = day.substring(0, 4) + "-" + day.substring(4, 6) + "-" + day.substring(6);

                    NodeList hours = dayEl.getElementsByTagName(TAG_HOUR);

                    for (int h = 0; h < hours.getLength(); h++) {
                        Element hourEl = (Element) hours.item(h);
                        String hour = hourEl.getAttribute(ATTRIBUTE_VALUE);

                        String content = hourEl.getTextContent().trim();

                        String[] parts = content.trim().split(",");

                        Integer prev = null;

                        for (int t = 0; t < parts.length; t++) {
                            var time = parts[t].trim();

                            int value;
                            int parsedTime = Integer.parseInt(time);

                            if (t == 0)
                                value = parsedTime;
                            else
                                value = prev + parsedTime;

                            prev = value;

                            String padded = String.format("%07d", value);
                            String formatted =
                                            padded.substring(0, 2) + ":" +
                                            padded.substring(2, 4) + ":" +
                                            padded.substring(4);

                            timestamps.add(
                                    LocalDateTime.parse(
                                            "%s %s:%s".formatted(day, hour, formatted),
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
            word.setAttribute(ATTRIBUTE_COUNT, String.valueOf(m.count()));


            for (XmlModelDay mDay : m.days()) {
                Element day = doc.createElement(TAG_DAY);
                day.setAttribute(ATTRIBUTE_VALUE, mDay.value().replace("-", ""));

                for (XmlModelHour mHour : mDay.hours()) {
                    Element hour = doc.createElement(TAG_HOUR);
                    hour.setAttribute(ATTRIBUTE_VALUE, mHour.value());

                    StringBuilder timeBuilder = new StringBuilder();

                    var it = mHour.times().iterator();

                    Integer prev = null;

                    while (it.hasNext()) {
                        var timeValue = it.next().value();

                        timeValue = timeValue.replace(":", "");
                        int current = Integer.parseInt(timeValue);

                        if (prev == null) {
                            timeBuilder.append(current);
                        } else {
                            int delta = current - prev;
                            timeBuilder.append(delta);
                        }

                        prev = current;

                        if (it.hasNext())
                            timeBuilder.append(',');
                    }

                    hour.setTextContent(timeBuilder.toString());

                    day.appendChild(hour);
                }

                word.appendChild(day);
            }

            root.appendChild(word);
        }

        doc.appendChild(root);
    }
}
