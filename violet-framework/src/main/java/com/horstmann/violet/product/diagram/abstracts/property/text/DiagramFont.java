package com.horstmann.violet.product.diagram.abstracts.property.text;

/**
 * The fonts offered for the texts of the diagrams, all bundled with the application. A font is saved in a text by its family
 * name, which must not change.
 */
public enum DiagramFont
{
    /** The font of a text that names none */
    STANDARD("Roboto", "Roboto", 1, true),
    CONDENSED("Oswald", "Oswald", 0.95, false),
    BOOK("Lora", "Lora", 1, true),
    TYPEWRITER("Courier Prime", "CourierPrime", 1, true),
    ROUNDED("Comfortaa", "Comfortaa", 0.95, false),
    HANDWRITTEN("Caveat", "Caveat", 1.3, false);

    private DiagramFont(String family, String resourcePrefix, double scale, boolean hasItalicFaces)
    {
        this.family = family;
        this.resourcePrefix = resourcePrefix;
        this.scale = scale;
        this.hasItalicFaces = hasItalicFaces;
    }

    /**
     * @param families family names separated by commas, as in the face attribute of HTML or in a CSS declaration
     * @return the first font known among them, null if none is
     */
    public static DiagramFont fromFamilies(String families)
    {
        for (String name : families == null ? new String[0] : families.split(","))
        {
            String family = name.replaceAll("[\"']", "").trim();
            for (DiagramFont font : values())
            {
                if (font.family.equalsIgnoreCase(family))
                {
                    return font;
                }
            }
        }
        return null;
    }

    /**
     * @return the name of the font in the texts
     */
    public String getFamily()
    {
        return this.family;
    }

    /**
     * @return what the characters are enlarged by, so that all the fonts look about as large at the same size
     */
    double getScale()
    {
        return this.scale;
    }

    /**
     * @return true if the font has its own italic characters, false if its plain ones must be slanted
     */
    boolean hasItalicFaces()
    {
        return this.hasItalicFaces;
    }

    /**
     * @param isBold true for the bold face
     * @param isItalic true for the italic face
     * @return the resource of the face to read
     */
    String getResource(boolean isBold, boolean isItalic)
    {
        String italic = isItalic && this.hasItalicFaces ? "Italic" : "";
        String style = isBold ? "Bold" + italic : italic.isEmpty() ? "Regular" : italic;
        return "/fonts/" + this.resourcePrefix + "-" + style + ".ttf";
    }

    private final String family;
    private final String resourcePrefix;
    private final double scale;
    private final boolean hasItalicFaces;
}
