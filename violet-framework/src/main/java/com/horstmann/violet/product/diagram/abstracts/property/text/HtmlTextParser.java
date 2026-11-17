package com.horstmann.violet.product.diagram.abstracts.property.text;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.html.StyleSheet;

import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * Reads the HTML of a {@link MultiLineString} as lines of styled text. It knows the few tags this HTML is made of, written by
 * {@link MultiLineString} itself or by the text pane of its editor: line breaks, paragraphs, bold, italic, underline, font size
 * and color. The style of the first block (alignment, default size, underline) is the one of the whole text. A parser reads one text.
 */
public class HtmlTextParser
{
    /**
     * @param html the text of a multiline string
     * @return its lines, each as pieces of text of one style; an empty list for an empty line
     */
    public List<List<TextRun>> parse(String html)
    {
        int position = 0;
        while (position < html.length())
        {
            int tagStart = html.indexOf('<', position);
            if (tagStart < 0)
            {
                addText(html.substring(position));
                break;
            }
            addText(html.substring(position, tagStart));
            if (html.startsWith("<!--", tagStart))
            {
                int commentEnd = html.indexOf("-->", tagStart);
                position = commentEnd < 0 ? html.length() : commentEnd + 3;
                continue;
            }
            int tagEnd = html.indexOf('>', tagStart);
            Matcher tag = tagEnd < 0 ? null : TAG.matcher(html.substring(tagStart + 1, tagEnd));
            if (tag == null || !tag.matches())
            {
                // A lone "<" is a character like another
                addText("<");
                position = tagStart + 1;
                continue;
            }
            handleTag(tag.group(2).toLowerCase(), !tag.group(1).isEmpty(), tag.group(3));
            position = tagEnd + 1;
        }
        if (!getCurrentLine().isEmpty())
        {
            endLine();
        }
        return getLines();
    }

    /**
     * @return the alignment of the text read, one of {@link MultiLineString.Justification}, null if the text doesn't tell it
     */
    public Integer getJustification()
    {
        // The body only tells the style of a text without any paragraph yet
        return this.justification != null ? this.justification : this.bodyJustification;
    }

    /**
     * @return the default font size of the text read, null if the text doesn't tell it
     */
    public Float getDefaultSize()
    {
        return this.defaultSize != null ? this.defaultSize : this.bodyDefaultSize;
    }

    /**
     * @return true if the text read tells it is underlined as a whole
     */
    public boolean isUnderlined()
    {
        return this.isUnderlined || this.isBodyUnderlined;
    }

    private void handleTag(String name, boolean isClosing, String attributes)
    {
        if (HIDDEN_TAGS.contains(name))
        {
            this.hiddenDepth = Math.max(0, this.hiddenDepth + (isClosing ? -1 : 1));
            return;
        }
        if (this.hiddenDepth > 0)
        {
            return;
        }
        if ("body".equals(name))
        {
            if (!isClosing)
            {
                this.bodyJustification = readAlignment(attributes);
                this.bodyDefaultSize = readDefaultSize(attributes);
                this.isBodyUnderlined = readUnderline(attributes);
            }
        }
        else if ("br".equals(name))
        {
            endLine();
        }
        else if (BLOCK_TAGS.contains(name))
        {
            if (isClosing)
            {
                closeBlock();
            }
            else
            {
                readBlockStyle(attributes);
                openBlock();
            }
        }
        else if (isClosing)
        {
            closeStyle(name);
        }
        else if (!attributes.endsWith("/"))
        {
            getOpenTags().push(new OpenTag(name, readStyle(name, attributes)));
        }
    }

    /**
     * Keeps the style of the text as a whole, which {@link MultiLineString} writes on its first block: alignment, default
     * font size, underline. The first block that tells one of them wins.
     *
     * @param attributes of an opening block tag
     */
    private void readBlockStyle(String attributes)
    {
        if (this.justification == null)
        {
            this.justification = readAlignment(attributes);
        }
        if (this.defaultSize == null)
        {
            this.defaultSize = readDefaultSize(attributes);
        }
        this.isUnderlined = this.isUnderlined || readUnderline(attributes);
    }

    /**
     * @param attributes of an opening tag
     * @return the font size their style gives, null if none
     */
    private Float readDefaultSize(String attributes)
    {
        String css = readValue(STYLE_ATTRIBUTE, attributes);
        if (css == null)
        {
            return null;
        }
        float size = readCssSize(TextStyle.PLAIN, readValue(CSS_FONT_SIZE, css)).size();
        return size > 0 ? size : null;
    }

    /**
     * @param attributes of an opening tag
     * @return true if their style underlines the text
     */
    private boolean readUnderline(String attributes)
    {
        String css = readValue(STYLE_ATTRIBUTE, attributes);
        String decoration = css == null ? null : readValue(CSS_TEXT_DECORATION, css);
        return decoration != null && decoration.toLowerCase().contains("underline");
    }

    /**
     * @param attributes of an opening tag
     * @return the alignment they give, one of {@link MultiLineString.Justification}, null if none
     */
    private Integer readAlignment(String attributes)
    {
        String css = readValue(STYLE_ATTRIBUTE, attributes);
        String alignment = css == null ? null : readValue(CSS_TEXT_ALIGN, css);
        if (alignment == null)
        {
            alignment = readValue(ALIGN_ATTRIBUTE, attributes);
        }
        if ("left".equalsIgnoreCase(alignment))
        {
            return MultiLineString.Justification.LEFT;
        }
        if ("center".equalsIgnoreCase(alignment))
        {
            return MultiLineString.Justification.CENTER;
        }
        if ("right".equalsIgnoreCase(alignment))
        {
            return MultiLineString.Justification.RIGHT;
        }
        return null;
    }

    private void openBlock()
    {
        if (!getCurrentLine().isEmpty())
        {
            endLine();
        }
        this.pendingSpace = false;
        getBlockStarts().push(getLines().size());
    }

    /**
     * Ends the line of the block. An empty block is an empty line, as the text pane writes one paragraph per line.
     */
    private void closeBlock()
    {
        Integer blockStart = getBlockStarts().poll();
        boolean isEmptyBlock = blockStart != null && blockStart == getLines().size();
        if (!getCurrentLine().isEmpty() || isEmptyBlock)
        {
            endLine();
        }
        this.pendingSpace = false;
    }

    private void endLine()
    {
        getLines().add(getCurrentLine());
        this.currentLine = null;
        this.pendingSpace = false;
    }

    private void closeStyle(String name)
    {
        for (OpenTag openTag : getOpenTags())
        {
            if (openTag.name().equals(name))
            {
                Iterator<OpenTag> tags = getOpenTags().iterator();
                while (tags.next() != openTag)
                {
                    tags.remove();
                }
                tags.remove();
                return;
            }
        }
    }

    private TextStyle getCurrentStyle()
    {
        OpenTag innerTag = getOpenTags().peek();
        return innerTag == null ? TextStyle.PLAIN : innerTag.style();
    }

    /**
     * @param name of an opening tag
     * @param attributes of this tag
     * @return the style of the text inside this tag
     */
    private TextStyle readStyle(String name, String attributes)
    {
        TextStyle style = getCurrentStyle();
        switch (name)
        {
        case "b":
        case "strong":
            style = style.withBold(true);
            break;
        case "i":
        case "em":
            style = style.withItalic(true);
            break;
        case "u":
            style = style.withUnderlined(true);
            break;
        case "font":
            style = readColor(style, readValue(COLOR_ATTRIBUTE, attributes));
            style = readFontTagSize(style, readValue(SIZE_ATTRIBUTE, attributes));
            break;
        default:
            break;
        }
        String css = readValue(STYLE_ATTRIBUTE, attributes);
        if (css == null)
        {
            return style;
        }
        style = readColor(style, readValue(CSS_COLOR, css));
        style = readCssSize(style, readValue(CSS_FONT_SIZE, css));
        String weight = readValue(CSS_FONT_WEIGHT, css);
        if (weight != null)
        {
            style = style.withBold(weight.matches("(?i)bold(er)?|[6-9]00"));
        }
        String slant = readValue(CSS_FONT_STYLE, css);
        if (slant != null)
        {
            style = style.withItalic(slant.matches("(?i)italic|oblique"));
        }
        return style;
    }

    /**
     * @param pattern of an attribute or of a CSS declaration, its value in the first group that matches
     * @param source the attributes of a tag, or its CSS
     * @return the value, null if absent
     */
    private String readValue(Pattern pattern, String source)
    {
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find())
        {
            return null;
        }
        for (int i = 1; i <= matcher.groupCount(); i++)
        {
            if (matcher.group(i) != null)
            {
                return matcher.group(i).trim();
            }
        }
        return null;
    }

    private TextStyle readColor(TextStyle style, String value)
    {
        Color color = value == null ? null : getColorParser().stringToColor(value);
        return color == null ? style : style.withColor(color);
    }

    /**
     * @param style to complete
     * @param value of the size attribute of a font tag: one of the 7 HTML sizes, relative to the size 3 when signed
     * @return the style with this size
     */
    private TextStyle readFontTagSize(TextStyle style, String value)
    {
        if (value == null || !value.matches("[+-]?\\d{1,3}"))
        {
            return style;
        }
        boolean isRelative = value.startsWith("+") || value.startsWith("-");
        int index = Integer.parseInt(value.replace("+", "")) + (isRelative ? 3 : 0);
        return style.withSize(HTML_FONT_SIZES[Math.max(1, Math.min(HTML_FONT_SIZES.length, index)) - 1]);
    }

    /**
     * @param style to complete
     * @param value of a font-size declaration, in points or in pixels
     * @return the style with this size
     */
    private TextStyle readCssSize(TextStyle style, String value)
    {
        Matcher size = value == null ? null : CSS_LENGTH.matcher(value);
        if (size == null || !size.matches())
        {
            return style;
        }
        float number = Float.parseFloat(size.group(1));
        // The texts were drawn by Swing so far, where a pixel is 1.3 point
        return style.withSize("px".equalsIgnoreCase(size.group(2)) ? number * 1.3f : number);
    }

    /**
     * Adds the text found between two tags to the current line. The line breaks of the HTML source, with the indentation that
     * follows them, count as one space inside a line and as nothing at its ends. Other spaces are all kept, as typed. The
     * indentation may come after a tag that follows the line break (the text pane writes "&lt;font&gt;\n&lt;/font&gt;    &lt;/p&gt;"):
     * kept, these spaces would widen the line and shift it when centered.
     */
    private void addText(String source)
    {
        if (this.hiddenDepth > 0 || source.isEmpty())
        {
            return;
        }
        String withoutSourceBreaks = SOURCE_LINE_BREAK.matcher(source.replace('\t', ' ')).replaceAll("\n");
        if (this.isAfterSourceLineBreak)
        {
            withoutSourceBreaks = withoutSourceBreaks.replaceFirst("^ +", "");
        }
        if (!withoutSourceBreaks.isEmpty())
        {
            this.isAfterSourceLineBreak = withoutSourceBreaks.endsWith("\n");
        }
        String text = decodeEntities(withoutSourceBreaks);
        StringBuilder characters = new StringBuilder();
        for (int i = 0; i < text.length(); i++)
        {
            char character = text.charAt(i);
            if (character == '\n')
            {
                this.pendingSpace = this.pendingSpace || characters.length() > 0 || !getCurrentLine().isEmpty();
                continue;
            }
            if (this.pendingSpace)
            {
                characters.append(' ');
                this.pendingSpace = false;
            }
            characters.append(character);
        }
        if (characters.length() == 0)
        {
            return;
        }
        List<TextRun> line = getCurrentLine();
        TextStyle style = getCurrentStyle();
        TextRun lastRun = line.isEmpty() ? null : line.get(line.size() - 1);
        if (lastRun != null && lastRun.style().equals(style))
        {
            line.set(line.size() - 1, new TextRun(lastRun.text() + characters, style));
        }
        else
        {
            line.add(new TextRun(characters.toString(), style));
        }
    }

    private String decodeEntities(String text)
    {
        if (text.indexOf('&') < 0)
        {
            return text;
        }
        Matcher entity = ENTITY.matcher(text);
        StringBuilder decoded = new StringBuilder();
        while (entity.find())
        {
            entity.appendReplacement(decoded, Matcher.quoteReplacement(decodeEntity(entity.group(), entity.group(1))));
        }
        entity.appendTail(decoded);
        return decoded.toString();
    }

    /**
     * @param entity as written, returned as is when unknown
     * @param name its name, or # and its code
     * @return the character
     */
    private String decodeEntity(String entity, String name)
    {
        switch (name)
        {
        case "amp":
            return "&";
        case "lt":
            return "<";
        case "gt":
            return ">";
        case "quot":
            return "\"";
        case "apos":
            return "'";
        case "nbsp":
            return " ";
        default:
            break;
        }
        if (!name.startsWith("#"))
        {
            return entity;
        }
        try
        {
            boolean isHexadecimal = name.startsWith("#x") || name.startsWith("#X");
            int code = isHexadecimal ? Integer.parseInt(name.substring(2), 16) : Integer.parseInt(name.substring(1));
            if (code == NO_BREAK_SPACE)
            {
                return " ";
            }
            return Character.isValidCodePoint(code) ? new String(Character.toChars(code)) : entity;
        }
        catch (NumberFormatException e)
        {
            return entity;
        }
    }

    private List<List<TextRun>> getLines()
    {
        if (this.lines == null)
        {
            this.lines = new ArrayList<List<TextRun>>();
        }
        return this.lines;
    }

    private List<TextRun> getCurrentLine()
    {
        if (this.currentLine == null)
        {
            this.currentLine = new ArrayList<TextRun>();
        }
        return this.currentLine;
    }

    private Deque<OpenTag> getOpenTags()
    {
        if (this.openTags == null)
        {
            this.openTags = new ArrayDeque<OpenTag>();
        }
        return this.openTags;
    }

    private Deque<Integer> getBlockStarts()
    {
        if (this.blockStarts == null)
        {
            this.blockStarts = new ArrayDeque<Integer>();
        }
        return this.blockStarts;
    }

    /**
     * @return what turns an HTML color (#rrggbb, name, rgb()) into a color: only a parser, nothing is drawn with it
     */
    private StyleSheet getColorParser()
    {
        if (this.colorParser == null)
        {
            this.colorParser = new StyleSheet();
        }
        return this.colorParser;
    }

    /**
     * An inline tag met and not closed yet.
     *
     * @param name of the tag
     * @param style of the text inside it
     */
    private record OpenTag(String name, TextStyle style)
    {
    }

    /** Content of a tag: "/" when closing, name, attributes */
    private static final Pattern TAG = Pattern.compile("(/?)\\s*([a-zA-Z][a-zA-Z0-9]*)(.*)", Pattern.DOTALL);
    private static final Pattern COLOR_ATTRIBUTE = attribute("color");
    private static final Pattern SIZE_ATTRIBUTE = attribute("size");
    private static final Pattern STYLE_ATTRIBUTE = attribute("style");
    private static final Pattern ALIGN_ATTRIBUTE = attribute("align");
    private static final Pattern CSS_TEXT_ALIGN = declaration("text-align");
    private static final Pattern CSS_TEXT_DECORATION = declaration("text-decoration");
    private static final Pattern CSS_COLOR = declaration("color");
    private static final Pattern CSS_FONT_SIZE = declaration("font-size");
    private static final Pattern CSS_FONT_WEIGHT = declaration("font-weight");
    private static final Pattern CSS_FONT_STYLE = declaration("font-style");
    private static final Pattern CSS_LENGTH = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(pt|px)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOURCE_LINE_BREAK = Pattern.compile(" *[\\r\\n][ \\r\\n]*");
    private static final Pattern ENTITY = Pattern.compile("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z]{2,8});");
    private static final int NO_BREAK_SPACE = 0xA0;

    /** Tags whose content is not part of the text */
    private static final Set<String> HIDDEN_TAGS = Set.of("head", "title", "style", "script");
    /** Tags that start on a new line and end theirs */
    private static final Set<String> BLOCK_TAGS = Set.of("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "tr", "pre",
            "blockquote");
    /** The 7 sizes of the font tag, as the text pane of the editor writes them */
    private static final int[] HTML_FONT_SIZES = { 8, 10, 12, 14, 18, 24, 36 };

    private static Pattern attribute(String name)
    {
        return Pattern.compile("(?<![\\w-])" + name + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"']+))",
                Pattern.CASE_INSENSITIVE);
    }

    private static Pattern declaration(String name)
    {
        return Pattern.compile("(?:^|;)\\s*" + name + "\\s*:\\s*([^;]+)", Pattern.CASE_INSENSITIVE);
    }

    private List<List<TextRun>> lines;
    private List<TextRun> currentLine;
    private Deque<OpenTag> openTags;
    private Deque<Integer> blockStarts;
    private StyleSheet colorParser;
    private Integer justification;
    private Integer bodyJustification;
    private Float defaultSize;
    private Float bodyDefaultSize;
    private boolean isUnderlined;
    private boolean isBodyUnderlined;
    private boolean pendingSpace;
    /** True when the last text read ends with a line break of the HTML source: the spaces that follow are its indentation */
    private boolean isAfterSourceLineBreak;
    private int hiddenDepth;
}
