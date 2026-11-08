package com.horstmann.violet.framework.file.persistence;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;
import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.framework.plugin.PluginRegistry;
import com.horstmann.violet.framework.util.SerializableEnumeration;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.IIdentifiable;
import com.horstmann.violet.product.diagram.abstracts.Id;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.EdgeTransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.node.CropInsets;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.GenericEdge;

@ManagedBean(registeredManually = true)
public class XMLPersistenceService implements IFilePersistenceService {

    @InjectedBean
    private PluginRegistry pluginRegistry;

    private final Map<String, Class<?>> tagTypes = new HashMap<String, Class<?>>();

    public XMLPersistenceService() {
        BeanInjector.getInjector().inject(this);
        initializeTagTypes();
    }

    @Override
    public void write(IGraph graph, java.io.OutputStream out) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph must not be null");
        }

        try {
            StringBuilder xml = new StringBuilder(4096);
            LegacyWriteContext context = new LegacyWriteContext();
            writeElement(xml, getElementDefaultTagName(graph), graph, graph.getClass(), context, 0, true);
            out.write(xml.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write legacy XML", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to serialize graph to legacy XML", e);
        }
    }

    @Override
    public IGraph read(InputStream in) throws IOException {
        String xml = readAll(in);
        if (xml == null || xml.trim().isEmpty()) {
            throw new IOException("Empty XML content");
        }

        Object rootObject = parseXml(xml);
        if (!(rootObject instanceof IGraph)) {
            throw new IOException("Unsupported legacy XML content");
        }

        IGraph graph = (IGraph) rootObject;
        Collection<INode> allNodes = graph.getAllNodes();
        for (INode aNode : allNodes) {
            aNode.setGraph(graph);
        }
        return graph;
    }

    private void initializeTagTypes() {
        List<IDiagramPlugin> plugins = new ArrayList<IDiagramPlugin>();
        if (this.pluginRegistry != null) {
            plugins.addAll(this.pluginRegistry.getDiagramPlugins());
        }
        if (plugins.isEmpty()) {
            ServiceLoader<IDiagramPlugin> discoveredPlugins = ServiceLoader.load(IDiagramPlugin.class,
                    this.getClass().getClassLoader());
            for (IDiagramPlugin discoveredPlugin : discoveredPlugins) {
                plugins.add(discoveredPlugin);
                if (this.pluginRegistry != null) {
                    this.pluginRegistry.register(discoveredPlugin);
                }
            }
        }

        for (IDiagramPlugin aPlugin : plugins) {
            Class<? extends IGraph> graphClass = aPlugin.getGraphClass();
            registerTagType(graphClass);
            try {
                IGraph graph = graphClass.getDeclaredConstructor().newInstance();
                for (INode nodePrototype : graph.getNodePrototypes()) {
                    registerTagType(nodePrototype.getClass());
                }
                for (IEdge edgePrototype : graph.getEdgePrototypes()) {
                    registerTagType(edgePrototype.getClass());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void registerTagType(Class<?> type) {
        this.tagTypes.put(getElementDefaultTagName(type), type);
    }

    private Object parseXml(String xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new java.io.StringReader(xml)));
            Element root = document.getDocumentElement();
            LegacyContext context = new LegacyContext();
            preloadResources(root, context);
            return readElement(root, null, context);
        } catch (Exception e) {
            throw new IOException("Unable to parse legacy XML", e);
        }
    }

    private void writeElement(StringBuilder xml, String elementName, Object value, Type expectedType,
            LegacyWriteContext context, int indentLevel, boolean isRoot)
            throws ReflectiveOperationException, IOException {
        if (value == null) {
            return;
        }

        Class<?> valueClass = value.getClass();

        if (isDuplicatedColorField(elementName, value)) {
            writeColorElement(xml, elementName, (Color) value, indentLevel);
            return;
        }

        if (value instanceof BufferedImage) {
            writeImageReferenceElement(xml, elementName, (BufferedImage) value, context, indentLevel);
            return;
        }

        if (isCompactLocation(elementName, value)) {
            writeLocationElement(xml, elementName, (Point2D) value, indentLevel);
            return;
        }

        if (isCompactPreferredSize(elementName, value)) {
            writePreferredSizeElement(xml, elementName, (Rectangle2D) value, indentLevel);
            return;
        }

        if (isSimpleType(valueClass)) {
            writeSimpleElement(xml, elementName, value, indentLevel);
            return;
        }

        // Nodes can appear multiple times (for example as edge endpoints): write a
        // reference after first serialization.
        String nodeId = null;
        if (value instanceof INode) {
            nodeId = getStableNodeReferenceId((INode) value);
            if (context.serializedNodeIds.contains(nodeId)) {
                writeNodeReferenceElement(xml, elementName, nodeId, indentLevel);
                return;
            }
            context.serializedNodeIds.add(nodeId);
        }

        if (value instanceof Collection<?>) {
            writeCollectionElement(xml, elementName, (Collection<?>) value, expectedType, context, indentLevel);
            return;
        }

        if (valueClass.isArray()) {
            writeArrayElement(xml, elementName, value, expectedType, context, indentLevel);
            return;
        }

        if (value instanceof Point2D) {
            writePointElement(xml, elementName, (Point2D) value, indentLevel);
            return;
        }

        if (value instanceof Rectangle2D.Double) {
            writeRectangleElement(xml, elementName, (Rectangle2D.Double) value, indentLevel);
            return;
        }

        if (value instanceof RoundRectangle2D.Double) {
            writeRoundRectangleElement(xml, elementName, (RoundRectangle2D.Double) value, indentLevel);
            return;
        }

        if (value instanceof Color) {
            writeColorElement(xml, elementName, (Color) value, indentLevel);
            return;
        }

        if (value instanceof CropInsets) {
            writeCropInsetsElement(xml, elementName, (CropInsets) value, indentLevel);
            return;
        }

        if (value instanceof ITransitionPoint) {
            writeTransitionPointElement(xml, elementName, (ITransitionPoint) value, indentLevel);
            return;
        }

        writeObjectElement(xml, elementName, value, valueClass, context, indentLevel, isRoot);
    }

    private void writeSimpleElement(StringBuilder xml, String elementName, Object value, int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);
        if (value instanceof Enum<?>) {
            xml.append(" name=\"").append(escapeXml(((Enum<?>) value).name())).append("\"/>").append('\n');
            return;
        }
        if (value instanceof SerializableEnumeration) {
            xml.append(" name=\"").append(escapeXml(getSerializableEnumerationName((SerializableEnumeration) value)))
                    .append("\"/>").append('\n');
            return;
        }
        xml.append('>').append(escapeXml(String.valueOf(value))).append("</").append(elementName).append('>')
                .append('\n');
    }

    private void writeNodeReferenceElement(StringBuilder xml, String elementName, String referenceId, int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);
        xml.append(" id=\"").append(referenceId).append("\"/>").append('\n');
    }

    private void writeCollectionElement(StringBuilder xml, String elementName, Collection<?> values, Type expectedType,
            LegacyWriteContext context, int indentLevel) throws ReflectiveOperationException, IOException {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);
        if (values.isEmpty()) {
            xml.append("/>").append('\n');
            return;
        }
        xml.append('>').append('\n');
        Class<?> childType = getCollectionType(expectedType);
        for (Object child : values) {
            if (child == null) {
                continue;
            }
            writeElement(xml, getElementDefaultTagName(child), child, childType, context, indentLevel + 1, false);
        }
        indent(xml, indentLevel);
        xml.append("</").append(elementName).append('>').append('\n');
    }

    private void writeArrayElement(StringBuilder xml, String elementName, Object array, Type expectedType,
            LegacyWriteContext context, int indentLevel) throws ReflectiveOperationException, IOException {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);
        int length = java.lang.reflect.Array.getLength(array);
        if (length == 0) {
            xml.append("/>").append('\n');
            return;
        }
        xml.append('>').append('\n');
        Class<?> componentType = getRawClass(expectedType);
        if (componentType != null && componentType.isArray()) {
            componentType = componentType.getComponentType();
        }
        for (int i = 0; i < length; i++) {
            Object child = java.lang.reflect.Array.get(array, i);
            if (child == null) {
                continue;
            }
            if (componentType != null && ITransitionPoint.class.isAssignableFrom(componentType)
                    && child instanceof ITransitionPoint) {
                writeTransitionPointElement(xml, "transitionPoint", (ITransitionPoint) child, indentLevel + 1);
                continue;
            }
            writeElement(xml, getElementDefaultTagName(child), child, componentType, context, indentLevel + 1, false);
        }
        indent(xml, indentLevel);
        xml.append("</").append(elementName).append('>').append('\n');
    }

    private void writePointElement(StringBuilder xml, String elementName, Point2D point, int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);
        xml.append(" x=\"").append(formatDouble(point.getX())).append("\"");
        xml.append(" y=\"").append(formatDouble(point.getY())).append("\"/>").append('\n');
    }

    private void writeLocationElement(StringBuilder xml, String elementName, Point2D point, int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);
        xml.append(" x=\"").append(formatDouble(point.getX())).append("\"");
        xml.append(" y=\"").append(formatDouble(point.getY())).append("\"/>").append('\n');
    }

    private void writeRectangleElement(StringBuilder xml, String elementName, Rectangle2D.Double rectangle,
            int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);
        xml.append(" x=\"").append(formatDouble(rectangle.getX())).append("\"");
        xml.append(" y=\"").append(formatDouble(rectangle.getY())).append("\"");
        xml.append(" width=\"").append(formatDouble(rectangle.getWidth())).append("\"");
        xml.append(" height=\"").append(formatDouble(rectangle.getHeight())).append("\"/>").append('\n');
    }

    private void writePreferredSizeElement(StringBuilder xml, String elementName, Rectangle2D rectangle,
            int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);
        xml.append(" width=\"").append(formatDouble(rectangle.getWidth())).append("\"");
        xml.append(" height=\"").append(formatDouble(rectangle.getHeight())).append("\"/>").append('\n');
    }

    private void writeRoundRectangleElement(StringBuilder xml, String elementName, RoundRectangle2D.Double rectangle,
            int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);
        xml.append(" x=\"").append(formatDouble(rectangle.getX())).append("\"");
        xml.append(" y=\"").append(formatDouble(rectangle.getY())).append("\"");
        xml.append(" width=\"").append(formatDouble(rectangle.getWidth())).append("\"");
        xml.append(" height=\"").append(formatDouble(rectangle.getHeight())).append("\"");
        xml.append(" arcwidth=\"").append(formatDouble(rectangle.getArcWidth())).append("\"");
        xml.append(" archeight=\"").append(formatDouble(rectangle.getArcHeight())).append("\"/>").append('\n');
    }

    private void writeColorElement(StringBuilder xml, String elementName, Color color, int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName)
                .append(" red=\"").append(color.getRed()).append("\"")
                .append(" green=\"").append(color.getGreen()).append("\"")
                .append(" blue=\"").append(color.getBlue()).append("\"")
                .append(" alpha=\"").append(color.getAlpha()).append("\"/>")
                .append('\n');
    }

    private void writeCropInsetsElement(StringBuilder xml, String elementName, CropInsets crop, int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName)
                .append(" top=\"").append(formatDouble(crop.getTop())).append("\"")
                .append(" left=\"").append(formatDouble(crop.getLeft())).append("\"")
                .append(" bottom=\"").append(formatDouble(crop.getBottom())).append("\"")
                .append(" right=\"").append(formatDouble(crop.getRight())).append("\"/>")
                .append('\n');
    }

    private void writeTransitionPointElement(StringBuilder xml, String elementName, ITransitionPoint tp,
            int indentLevel) {
        indent(xml, indentLevel);
        xml.append('<').append(elementName)
                .append(" x=\"").append(formatDouble(tp.getX())).append("\"")
                .append(" y=\"").append(formatDouble(tp.getY())).append("\"/>").append('\n');
    }


    private void writeImageReferenceElement(StringBuilder xml, String elementName, BufferedImage image,
            LegacyWriteContext context, int indentLevel) throws IOException {
        String imageResourceId = context.imageResourceIds.get(image);
        if (imageResourceId == null) {
            imageResourceId = context.nextImageId();
            context.imageResourceIds.put(image, imageResourceId);
            context.imageResources.put(imageResourceId, image);
        }

        indent(xml, indentLevel);
        xml.append('<').append(elementName)
                .append(" id=\"").append(imageResourceId).append("\"/>")
                .append('\n');
    }

    private void writeResourcesElement(StringBuilder xml, LegacyWriteContext context, int indentLevel)
            throws IOException {
        if (context.imageResources.isEmpty()) {
            return;
        }

        indent(xml, indentLevel);
        xml.append("<resources>").append('\n');
        for (Map.Entry<String, BufferedImage> entry : context.imageResources.entrySet()) {
            indent(xml, indentLevel + 1);
            xml.append("<image id=\"").append(entry.getKey()).append("\">");
            xml.append(encodePngImage(entry.getValue()));
            xml.append("</image>").append('\n');
        }
        indent(xml, indentLevel);
        xml.append("</resources>").append('\n');
    }

    private void writeObjectElement(StringBuilder xml, String elementName, Object value, Class<?> valueClass,
            LegacyWriteContext context, int indentLevel, boolean isRoot)
            throws ReflectiveOperationException, IOException {
        indent(xml, indentLevel);
        xml.append('<').append(elementName);

        List<Field> fields = getSerializableFields(valueClass);
        List<Field> attributeFields = new ArrayList<Field>();
        List<Field> childFields = new ArrayList<Field>();
        for (Field field : fields) {
            if (isAttributeField(field)) {
                attributeFields.add(field);
            } else {
                childFields.add(field);
            }
        }

        for (Field field : attributeFields) {
            field.setAccessible(true);
            Object fieldValue = field.get(value);
            if (fieldValue == null) {
                continue;
            }
            xml.append(' ')
                    .append(field.getName())
                    .append("=\"")
                    .append(escapeXml(String.valueOf(fieldValue)))
                    .append("\"");
        }

        if (childFields.isEmpty()) {
            xml.append("/>").append('\n');
            return;
        }

        xml.append('>').append('\n');
        for (Field field : childFields) {
            field.setAccessible(true);
            Object fieldValue = field.get(value);
            if (fieldValue == null) {
                continue;
            }
            if ("parent".equals(field.getName())) {
                continue;
            }
            if ("children".equals(field.getName())
                    && fieldValue instanceof Collection<?>
                    && ((Collection<?>) fieldValue).isEmpty()) {
                continue;
            }
            if ("preferredSize".equals(field.getName())
                    && fieldValue instanceof Rectangle2D
                    && isDefaultPreferredSize((Rectangle2D) fieldValue)) {
                continue;
            }
            if ("cropInsets".equals(field.getName())
                    && fieldValue instanceof CropInsets
                    && ((CropInsets) fieldValue).isEmpty()) {
                continue;
            }
            writeElement(xml, field.getName(), fieldValue, field.getGenericType(), context, indentLevel + 1, false);
        }
        if (isRoot) {
            writeResourcesElement(xml, context, indentLevel + 1);
        }
        indent(xml, indentLevel);
        xml.append("</").append(elementName).append('>').append('\n');
    }

    private static void preloadResources(Element root, LegacyContext context) throws IOException {
        for (Element child : getChildElements(root)) {
            if (!"resources".equals(child.getTagName())) {
                continue;
            }
            for (Element resource : getChildElements(child)) {
                if ("image".equals(resource.getTagName())) {
                    readImage(resource, context);
                }
            }
        }
    }

    private static List<Field> getSerializableFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        collectSerializableFields(type, fields);
        return fields;
    }

    private static void collectSerializableFields(Class<?> type, List<Field> fields) {
        if (type == null || Object.class.equals(type)) {
            return;
        }
        collectSerializableFields(type.getSuperclass(), fields);
        for (Field field : type.getDeclaredFields()) {
            if (!shouldSerializeField(field)) {
                continue;
            }
            fields.add(field);
        }
    }

    private static boolean shouldSerializeField(Field field) {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers)
                && !Modifier.isTransient(modifiers)
                && !field.isSynthetic();
    }

    private static boolean isAttributeField(Field field) {
        return "value".equals(field.getName()) && String.class.equals(field.getType());
    }

    private static boolean isCompactPreferredSize(String elementName, Object value) {
        return "preferredSize".equals(elementName) && value instanceof Rectangle2D;
    }

    private static boolean isDefaultPreferredSize(Rectangle2D rectangle) {
        return rectangle.getWidth() == 0d && rectangle.getHeight() == 0d;
    }

    private static boolean isCompactLocation(String elementName, Object value) {
        return "location".equals(elementName) && value instanceof Point2D;
    }

    private static boolean isDuplicatedColorField(String elementName, Object value) {
        if (!(value instanceof Color)) {
            return false;
        }
        return "backgroundColor".equals(elementName)
                || "textColor".equals(elementName)
                || "borderColor".equals(elementName);
    }

    private static String getElementDefaultTagName(Object value) {
        return getElementDefaultTagName(value.getClass());
    }

    private static String getElementDefaultTagName(Class<?> type) {
        String simpleName = type.getSimpleName();
        if (simpleName.isEmpty()) {
            return simpleName;
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private static String getSerializableEnumerationName(SerializableEnumeration enumeration) {
        String raw = enumeration.toString();
        int lastDot = raw.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < raw.length()) {
            return raw.substring(lastDot + 1);
        }
        return raw;
    }

    private static String getStableNodeReferenceId(INode node) {
        if (node instanceof IIdentifiable) {
            IIdentifiable identifiable = (IIdentifiable) node;
            if (identifiable.getId() != null && identifiable.getId().getValue() != null
                    && !identifiable.getId().getValue().isEmpty()) {
                return identifiable.getId().getValue();
            }
        }
        throw new IllegalStateException("INode must provide a stable non-empty id via IIdentifiable");
    }

    private static String encodePngImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bytes);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } finally {
            bytes.close();
        }
    }

    private static String formatDouble(double value) {
        return Double.toString(value);
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static void indent(StringBuilder xml, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            xml.append("  ");
        }
    }

    private Object readElement(Element element, Type expectedType, LegacyContext context) throws Exception {
        String referenceId = getReferenceAttribute(element);
        if (referenceId != null && !referenceId.isEmpty()) {
            Object referencedObject = context.idMap.get(referenceId);
            if (referencedObject != null) {
                return referencedObject;
            }
            return context.imageIdMap.get(referenceId);
        }

        Class<?> expectedClass = getRawClass(expectedType);
        Class<?> valueClass = resolveValueClass(element, expectedClass);

        if (isSimpleType(valueClass)) {
            return parseSimpleValue(element, valueClass);
        }

        if (Collection.class.isAssignableFrom(valueClass)) {
            Collection<Object> collection = new ArrayList<Object>();
            registerIdIfNeeded(element, collection, context);
            Class<?> childClass = getCollectionType(expectedType);
            for (Element child : getChildElements(element)) {
                Object childValue = readElement(child, childClass, context);
                collection.add(childValue);
            }
            return collection;
        }

        if (valueClass.isArray()) {
            Class<?> componentType = valueClass.getComponentType();
            List<Element> children = getChildElements(element);
            Object array = java.lang.reflect.Array.newInstance(componentType, children.size());
            registerIdIfNeeded(element, array, context);
            for (int i = 0; i < children.size(); i++) {
                Object childValue = readElement(children.get(i), componentType, context);
                java.lang.reflect.Array.set(array, i, childValue);
            }
            return array;
        }

        if (valueClass.equals(Point2D.Double.class)) {
            Point2D.Double point = readPoint2D(element);
            registerIdIfNeeded(element, point, context);
            return point;
        }

        if (valueClass.equals(Rectangle2D.Double.class)) {
            Rectangle2D.Double rectangle = readRectangle2D(element);
            registerIdIfNeeded(element, rectangle, context);
            return rectangle;
        }

        if (valueClass.equals(RoundRectangle2D.Double.class)) {
            RoundRectangle2D.Double rectangle = readRoundRectangle2D(element);
            registerIdIfNeeded(element, rectangle, context);
            return rectangle;
        }

        if (BufferedImage.class.equals(valueClass)) {
            BufferedImage image = readImage(element, context);
            registerIdIfNeeded(element, image, context);
            return image;
        }

        if (ITransitionPoint.class.isAssignableFrom(valueClass)) {
            ITransitionPoint transitionPoint = readTransitionPoint(element);
            registerIdIfNeeded(element, transitionPoint, context);
            return transitionPoint;
        }

        if (valueClass.equals(Color.class)) {
            Color color = readColor(element);
            registerIdIfNeeded(element, color, context);
            return color;
        }

        if (CropInsets.class.isAssignableFrom(valueClass)) {
            CropInsets crop = readCropInsets(element);
            registerIdIfNeeded(element, crop, context);
            return crop;
        }

        Object instance = newInstance(valueClass);
        registerIdIfNeeded(element, instance, context);
        applyAttributes(instance, element, expectedClass);

        for (Element child : getChildElements(element)) {
            Field field = findField(valueClass, child.getTagName());
            if (field == null)
                continue;
            field.setAccessible(true);
            Type fieldType = field.getGenericType();
            Object fieldValue = readElement(child, fieldType, context);
            if (fieldValue != null || !field.getType().isPrimitive()) {
                field.set(instance, fieldValue);
                registerStableIdentityFromField(instance, field.getName(), fieldValue, context);
                if ("children".equals(field.getName()) && fieldValue instanceof Collection<?>
                        && instance instanceof INode) {
                    for (Object childNode : (Collection<?>) fieldValue) {
                        if (childNode instanceof INode) {
                            ((INode) childNode).setParent((INode) instance);
                        }
                    }
                }
            }
        }

        return instance;
    }

    private void applyAttributes(Object instance, Element element, Class<?> expectedClass) throws Exception {
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            Node attribute = element.getAttributes().item(i);
            String name = attribute.getNodeName();
            if ("id".equals(name))
                continue;
            String value = attribute.getNodeValue();

            Field field = findField(instance.getClass(), name);
            if (field != null) {
                field.setAccessible(true);
                field.set(instance, parseValueFromString(field.getType(), value));
                continue;
            }

            if ("name".equals(name) && expectedClass != null) {
                Object enumValue = parseEnumerationFromName(expectedClass, value);
                if (enumValue != null) {
                    // handled at object creation level when needed
                }
            }
        }
    }

    private Object parseSimpleValue(Element element, Class<?> valueClass) throws Exception {
        String nameAttribute = element.getAttribute("name");
        if (nameAttribute != null && !nameAttribute.isEmpty()) {
            Object enumValue = parseEnumerationFromName(valueClass, nameAttribute);
            if (enumValue != null)
                return enumValue;
        }
        return parseValueFromString(valueClass, element.getTextContent());
    }

    private Object parseEnumerationFromName(Class<?> valueClass, String value) throws Exception {
        if (valueClass.isEnum()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object enumValue = Enum.valueOf((Class<? extends Enum>) valueClass, value);
            return enumValue;
        }
        if (SerializableEnumeration.class.isAssignableFrom(valueClass)) {
            Field field = valueClass.getField(value);
            if (Modifier.isStatic(field.getModifiers())) {
                return field.get(null);
            }
        }
        return null;
    }

    private Object parseValueFromString(Class<?> type, String value) {
        if (value == null)
            value = "";
        if (type.equals(String.class))
            return value;
        if (type.equals(int.class) || type.equals(Integer.class))
            return Integer.parseInt(value);
        if (type.equals(long.class) || type.equals(Long.class))
            return Long.parseLong(value);
        if (type.equals(double.class) || type.equals(Double.class))
            return Double.parseDouble(value);
        if (type.equals(float.class) || type.equals(Float.class))
            return Float.parseFloat(value);
        if (type.equals(boolean.class) || type.equals(Boolean.class))
            return Boolean.parseBoolean(value);
        if (type.equals(short.class) || type.equals(Short.class))
            return Short.parseShort(value);
        if (type.equals(byte.class) || type.equals(Byte.class))
            return Byte.parseByte(value);
        if (type.equals(char.class) || type.equals(Character.class))
            return value.isEmpty() ? '\0' : value.charAt(0);
        return value;
    }

    private Object newInstance(Class<?> valueClass) throws Exception {
        Constructor<?> constructor = valueClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private Class<?> resolveValueClass(Element element, Class<?> expectedClass) {
        String tagName = element.getTagName();
        Class<?> fromTagName = resolveTagClass(tagName);
        if (fromTagName != null)
            return fromTagName;

        if (expectedClass != null) {
            if (Image.class.isAssignableFrom(expectedClass)) {
                return BufferedImage.class;
            }

            if (Rectangle2D.class.isAssignableFrom(expectedClass)
                    && !element.getAttribute("width").isEmpty()
                    && !element.getAttribute("height").isEmpty()) {
                if (!element.getAttribute("arcwidth").isEmpty() || !element.getAttribute("archeight").isEmpty()) {
                    return RoundRectangle2D.Double.class;
                }
                return Rectangle2D.Double.class;
            }

            if (Point2D.class.isAssignableFrom(expectedClass)
                    && !element.getAttribute("x").isEmpty()
                    && !element.getAttribute("y").isEmpty()) {
                return Point2D.Double.class;
            }
        }

        if (expectedClass != null)
            return expectedClass;
        return String.class;
    }

    private Class<?> resolveTagClass(String tagName) {
        if ("Point2D.Double".equals(tagName))
            return Point2D.Double.class;
        if ("Rectangle2D.Double".equals(tagName))
            return Rectangle2D.Double.class;
        if ("RoundRectangle2D.Double".equals(tagName))
            return RoundRectangle2D.Double.class;
        if ("Image".equals(tagName))
            return BufferedImage.class;
        if ("transitionPoint".equals(tagName))
            return EdgeTransitionPoint.class;
        // The former note edge is now a generic edge with the same defaults
        if (GenericEdge.LEGACY_NOTE_EDGE_TAG.equals(tagName))
            return GenericEdge.class;

        Class<?> resolved = this.tagTypes.get(tagName);
        if (resolved != null)
            return resolved;
        return null;
    }

    private static List<Element> getChildElements(Element element) {
        List<Element> children = new ArrayList<Element>();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                children.add((Element) node);
            }
        }
        return children;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String getReferenceAttribute(Element element) {
        String id = element.getAttribute("id");
        if (id != null && !id.isEmpty() && getChildElements(element).isEmpty()) {
            return id;
        }
        return null;
    }

    private static Class<?> getRawClass(Type type) {
        if (type instanceof Class<?>)
            return (Class<?>) type;
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            if (raw instanceof Class<?>)
                return (Class<?>) raw;
        }
        return null;
    }

    private static Class<?> getCollectionType(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType) type).getActualTypeArguments();
            if (args.length > 0) {
                Class<?> rawClass = getRawClass(args[0]);
                if (rawClass != null)
                    return rawClass;
            }
        }
        return Object.class;
    }

    private static boolean isSimpleType(Class<?> valueClass) {
        if (valueClass == null)
            return false;
        return valueClass.equals(String.class)
                || valueClass.equals(Integer.class) || valueClass.equals(int.class)
                || valueClass.equals(Long.class) || valueClass.equals(long.class)
                || valueClass.equals(Double.class) || valueClass.equals(double.class)
                || valueClass.equals(Float.class) || valueClass.equals(float.class)
                || valueClass.equals(Boolean.class) || valueClass.equals(boolean.class)
                || valueClass.equals(Short.class) || valueClass.equals(short.class)
                || valueClass.equals(Byte.class) || valueClass.equals(byte.class)
                || valueClass.equals(Character.class) || valueClass.equals(char.class)
                || valueClass.isEnum()
                || SerializableEnumeration.class.isAssignableFrom(valueClass);
    }

    private static void registerIdIfNeeded(Element element, Object value, LegacyContext context) {
        String id = element.getAttribute("id");
        if (id != null && !id.isEmpty()) {
            context.idMap.put(id, value);
        }
    }

    private static void registerStableIdentityFromField(Object instance, String fieldName, Object fieldValue,
            LegacyContext context) {
        if (!(instance instanceof IIdentifiable) || !"id".equals(fieldName) || !(fieldValue instanceof Id)) {
            return;
        }
        Id stableId = (Id) fieldValue;
        if (stableId.getValue() == null || stableId.getValue().isEmpty()) {
            return;
        }
        context.idMap.put(stableId.getValue(), instance);
    }

    private static double parseDoubleAttribute(Element element, String attribute) {
        String value = element.getAttribute(attribute);
        if (value == null || value.isEmpty())
            return 0d;
        return Double.parseDouble(value);
    }

    private static Color readColor(Element element) {
        int red = parseIntAttribute(element, "red", 0);
        int green = parseIntAttribute(element, "green", 0);
        int blue = parseIntAttribute(element, "blue", 0);
        int alpha = parseIntAttribute(element, "alpha", 255);

        return new Color(red, green, blue, alpha);
    }

    private static CropInsets readCropInsets(Element element) {
        double top = parseDoubleAttribute(element, "top");
        double left = parseDoubleAttribute(element, "left");
        double bottom = parseDoubleAttribute(element, "bottom");
        double right = parseDoubleAttribute(element, "right");

        return new CropInsets(top, left, bottom, right);
    }

    private static int parseIntAttribute(Element element, String attribute, int defaultValue) {
        String value = element.getAttribute(attribute);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static Point2D.Double readPoint2D(Element element) {
        String x = element.getAttribute("x");
        String y = element.getAttribute("y");
        if (x.isEmpty() || y.isEmpty()) {
            throw new IllegalArgumentException("Missing x/y attributes for point");
        }
        return new Point2D.Double(Double.parseDouble(x), Double.parseDouble(y));
    }

    private static Rectangle2D.Double readRectangle2D(Element element) {
        String x = element.getAttribute("x");
        String y = element.getAttribute("y");
        String width = element.getAttribute("width");
        String height = element.getAttribute("height");
        if (width.isEmpty() || height.isEmpty()) {
            throw new IllegalArgumentException("Missing width/height attributes for rectangle");
        }
        return new Rectangle2D.Double(
                x.isEmpty() ? 0d : Double.parseDouble(x),
                y.isEmpty() ? 0d : Double.parseDouble(y),
                Double.parseDouble(width),
                Double.parseDouble(height));
    }

    private static RoundRectangle2D.Double readRoundRectangle2D(Element element) {
        String x = element.getAttribute("x");
        String y = element.getAttribute("y");
        String width = element.getAttribute("width");
        String height = element.getAttribute("height");
        String arcWidth = element.getAttribute("arcwidth");
        String arcHeight = element.getAttribute("archeight");
        if (x.isEmpty() || y.isEmpty() || width.isEmpty() || height.isEmpty() || arcWidth.isEmpty()
                || arcHeight.isEmpty()) {
            throw new IllegalArgumentException("Missing attributes for round rectangle");
        }
        return new RoundRectangle2D.Double(
                Double.parseDouble(x),
                Double.parseDouble(y),
                Double.parseDouble(width),
                Double.parseDouble(height),
                Double.parseDouble(arcWidth),
                Double.parseDouble(arcHeight));
    }

    private static BufferedImage readImage(Element element, LegacyContext context) throws IOException {
        String imageId = element.getAttribute("id");
        if (!imageId.isEmpty()) {
            BufferedImage existing = context.imageIdMap.get(imageId);
            if (existing != null) {
                return existing;
            }
        }

        String raw = element.getTextContent();
        String base64 = raw == null ? "" : raw.replaceAll("\\s+", "");
        if (base64.isEmpty()) {
            return null;
        }

        byte[] bytes = Base64.getDecoder().decode(base64);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

        String imgId = element.getAttribute("id");
        if (!imgId.isEmpty() && image != null) {
            context.imageIdMap.put(imgId, image);
        }
        return image;
    }

    private static ITransitionPoint readTransitionPoint(Element element) {
        String xAttribute = element.getAttribute("x");
        String yAttribute = element.getAttribute("y");
        if (xAttribute.isEmpty() || yAttribute.isEmpty()) {
            throw new IllegalArgumentException("Missing x/y attributes for transition point");
        }
        return new EdgeTransitionPoint(Double.parseDouble(xAttribute), Double.parseDouble(yAttribute));
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static class LegacyContext {
        private final Map<String, Object> idMap = new HashMap<String, Object>();

        private final Map<String, BufferedImage> imageIdMap = new HashMap<String, BufferedImage>();
    }

    private static class LegacyWriteContext {
        private final Set<String> serializedNodeIds = new HashSet<String>();

        private final Map<BufferedImage, String> imageResourceIds = new IdentityHashMap<BufferedImage, String>();

        private final Map<String, BufferedImage> imageResources = new LinkedHashMap<String, BufferedImage>();

        private int nextImageId = 1;

        private String nextImageId() {
            return "img-" + this.nextImageId++;
        }
    }
}
