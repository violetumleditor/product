package com.horstmann.violet.product.diagram.abstracts.property.text;

/**
 * Consecutive characters of one line of a multiline string sharing the same style.
 *
 * @param text the characters, without any line break
 * @param style their style
 */
public record TextRun(String text, TextStyle style)
{
}
