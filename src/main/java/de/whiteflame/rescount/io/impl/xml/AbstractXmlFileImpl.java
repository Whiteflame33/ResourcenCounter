package de.whiteflame.rescount.io.impl.xml;

import de.whiteflame.rescount.TimestampGrouper;
import de.whiteflame.rescount.api.io.IFileReader;
import de.whiteflame.rescount.api.io.IFileWriter;
import de.whiteflame.rescount.api.model.GroupedDay;
import de.whiteflame.rescount.api.model.GroupedYear;
import de.whiteflame.rescount.io.impl.xml.model.XmlModel;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelDay;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelHour;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelTime;
import de.whiteflame.rescount.io.impl.xml.model.XmlModelWord;
import org.w3c.dom.Document;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract sealed class AbstractXmlFileImpl implements IFileReader, IFileWriter
                                                 permits XmlVerboseFileImpl, XmlSlimFileImpl {
    public void writeFile(File file, Map<String, List<LocalDateTime>> groupedTimestamps, boolean indent) {
        Set<XmlModelWord> words = new LinkedHashSet<>();

        for (var entry : groupedTimestamps.entrySet()) {
            words.add(getXmlModelWord(entry.getKey(), entry.getValue()));
        }

        XmlModel model = new XmlModel(words);

        try {
            var factory = DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();

            Document doc = builder.newDocument();

            constructDocument(doc, model);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            if (indent) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            }

            try (OutputStream os = Files.newOutputStream(file.toPath())) {
                transformer.transform(new DOMSource(doc), new StreamResult(os));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected abstract void constructDocument(Document doc, XmlModel model);

    protected XmlModelWord getXmlModelWord(String word, List<LocalDateTime> timestamps) {
        List<GroupedDay> groupedDays = TimestampGrouper.groupByDay(timestamps);

        Set<XmlModelDay> modelDays = getXmlModelDays(groupedDays);

        return new XmlModelWord(word, timestamps.size(), modelDays);
    }

    private static Set<XmlModelDay> getXmlModelDays(List<GroupedDay> groupedDays) {
        Set<XmlModelDay> modelDays = new LinkedHashSet<>();

        for (var dayEntry : groupedDays) {
            Set<XmlModelHour> modelHours = new LinkedHashSet<>();

            for (var hourEntry : dayEntry.hours()) {
                Set<XmlModelTime> modelTimes = new LinkedHashSet<>();

                for (var timeEntry : hourEntry.times()) {
                    modelTimes.add(new XmlModelTime(String.valueOf(timeEntry)));
                }

                modelHours.add(new XmlModelHour(String.valueOf(hourEntry.hour()), modelTimes));
            }

            modelDays.add(new XmlModelDay(String.valueOf(dayEntry.day()), modelHours));
        }
        return modelDays;
    }

    protected boolean isType(File file, String rootTag) {
        if (!file.getName().endsWith(getFileType().getFileExtension()))
            return false;

        try {
            var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(file);

            return doc.getDocumentElement().getTagName().equals(rootTag);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
