package de.whiteflame.rescount.config.impl;

import de.whiteflame.rescount.api.config.IConfigBackend;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class XmlConfigBackend implements IConfigBackend {
    private static final ILogger LOGGER = LoggerFactory.getLogger(XmlConfigBackend.class);

    private static final String TAG_ROOT = "configuration",
                                TAG_OPTION = "option",
                                ATTRIBUTE_KEY = "key",
                                ATTRIBUTE_VALUE = "value",
                                DTD_FILE = "configuration.dtd";

    private final File file;
    private final Map<String, String> properties;

    public XmlConfigBackend(File file) {
        this.file = file;
        this.properties = new HashMap<>();
    }

    @Override
    public void load() {
        if (!file.exists()) {
            LOGGER.debug("File {} does not exist. Skipping config loading...", file.getAbsolutePath());
            return;
        }

        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            factory.setIgnoringElementContentWhitespace(true);

            var builder = factory.newDocumentBuilder();

            builder.setEntityResolver((_, systemId) -> {
                if (systemId.contains(DTD_FILE)) {
                    InputStream dtdStream = getClass().getClassLoader().getResourceAsStream(DTD_FILE);
                    if (dtdStream != null) {
                        return new InputSource(dtdStream);
                    } else {
                        LOGGER.error("Could not find {} in resources!", DTD_FILE);
                    }
                }
                return null;
            });

            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    LOGGER.warn("XML Validation Warning: {}", e.getMessage());
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    LOGGER.error("XML Validation Error: {}", e.getMessage());
                    throw e;
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    LOGGER.error("XML Fatal Error: {}", e.getMessage());
                    throw e;
                }
            });

            Document doc = builder.parse(file);

            LOGGER.debug("Begin parsing of XML config file...");

            Element rootElement = doc.getDocumentElement();

            if (!TAG_ROOT.equals(rootElement.getTagName())) {
                throw new RuntimeException("Invalid tag <" + rootElement.getTagName() + "> found in configuration document. Expected <" + TAG_ROOT + ">.");
            }

            NodeList children = rootElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                var node = children.item(i);

                if (node.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                Element child = (Element) node;

                if (!TAG_OPTION.equals(child.getTagName())) {
                    throw new RuntimeException("Invalid tag <" + child.getTagName() +"> found in configuration document.");
                }

                String key = child.getAttribute(ATTRIBUTE_KEY);
                String value = child.getAttribute(ATTRIBUTE_VALUE);

                properties.put(key, value);
            }

            LOGGER.debug("Finished parsing of XML config file...");
        } catch (Exception e) {
            LOGGER.error("Error during configuration parsing", e);
        }
    }

    @Override
    public void save() {
        try {
            if (file.createNewFile()) {
                LOGGER.info("Configuration file {} has been created", file.getAbsolutePath());
            }

            var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();

            LOGGER.trace("Starting to build configuration DOM structure");
            Element root = doc.createElement(TAG_ROOT);

            for (var entry : properties.entrySet()) {
                LOGGER.trace("Storing property key=\"{}\" with value=\"{}\"", entry.getKey(), entry.getValue());

                Element option = doc.createElement(TAG_OPTION);

                option.setAttribute(ATTRIBUTE_KEY, entry.getKey());
                option.setAttribute(ATTRIBUTE_VALUE, entry.getValue());

                root.appendChild(option);
            }

            doc.appendChild(root);
            LOGGER.trace("Finished building configuration DOM structure");

            var transformer = TransformerFactory.newInstance().newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DTD_FILE);

            try (OutputStream os = Files.newOutputStream(file.toPath())) {
                LOGGER.trace("Storing DOM structure to file {}", file.getName());
                transformer.transform(new DOMSource(doc), new StreamResult(os));
            }

            LOGGER.debug("Finished saving of XML file");
        } catch (Exception e) {
            LOGGER.error("Error during configuration saving", e);
        }
    }

    @Override
    public Optional<String> getValue(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    @Override
    public void setValue(String key, String value) {
        properties.put(key, value);
    }
}
