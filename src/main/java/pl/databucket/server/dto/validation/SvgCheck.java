package pl.databucket.server.dto.validation;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An icon is SVG markup shown to every member of the project, so only what draws a picture is accepted: no scripts,
 * links, styles, foreign content or external resources. Either a whole {@code <svg>} element or, as icons were stored
 * before, only its content (drawn in a 24x24 box). The frontend sanitizes icons again when it shows them.
 */
public final class SvgCheck {

    public static final int MAX_LENGTH = 32 * 1024;

    private static final Set<String> ELEMENTS = Set.of(
            "svg", "g", "defs", "symbol", "use", "title", "desc", "metadata",
            "path", "circle", "ellipse", "line", "polyline", "polygon", "rect",
            "text", "tspan", "textpath",
            "lineargradient", "radialgradient", "stop", "pattern", "clippath", "mask", "marker", "filter");

    // url(...) that doesn't point into the icon itself (url(#gradient))
    private static final Pattern EXTERNAL_URL = Pattern.compile("url\\(\\s*['\"]?(?!#)", Pattern.CASE_INSENSITIVE);

    private SvgCheck() {
    }

    /**
     * @return null when the icon is fine (or empty), otherwise what is wrong with it
     */
    public static String problem(String svg) {
        if (svg == null || svg.isBlank())
            return null;
        if (svg.length() > MAX_LENGTH)
            return "The icon is larger than " + (MAX_LENGTH / 1024) + " KB";

        String markup = svg.strip();
        if (!markup.regionMatches(true, 0, "<svg", 0, 4))
            markup = "<svg>" + markup + "</svg>";

        Element root;
        try {
            root = builder().parse(new InputSource(new StringReader(markup))).getDocumentElement();
        } catch (Exception e) {
            return "The icon is not valid SVG";
        }
        if (!"svg".equals(name(root)))
            return "The icon is not an SVG element";
        return problemOf(root);
    }

    private static String problemOf(Element element) {
        String name = name(element);
        if (!ELEMENTS.contains(name) && !name.startsWith("fe"))
            return "The icon contains a forbidden element: " + element.getTagName();

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName().toLowerCase(Locale.ROOT);
            String value = attribute.getNodeValue();
            String compact = value.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
            if (attributeName.startsWith("on"))
                return "The icon contains a forbidden attribute: " + attribute.getNodeName();
            if ((attributeName.equals("href") || attributeName.endsWith(":href")) && !value.strip().startsWith("#"))
                return "The icon refers to an external resource: " + attribute.getNodeName();
            if (compact.contains("javascript:") || EXTERNAL_URL.matcher(value).find())
                return "The icon refers to an external resource: " + attribute.getNodeName();
        }

        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Element childElement) {
                String problem = problemOf(childElement);
                if (problem != null)
                    return problem;
            }
        return null;
    }

    // without a namespace prefix: svg:path is a path
    private static String name(Element element) {
        String name = element.getTagName();
        return name.substring(name.indexOf(':') + 1).toLowerCase(Locale.ROOT);
    }

    private static DocumentBuilder builder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // no DOCTYPE, so no entities (XXE, entity expansion)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(null);
        return builder;
    }
}
