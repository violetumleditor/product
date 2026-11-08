/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.product.diagram.classes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.horstmann.violet.framework.util.StringFilterOutputStream;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.product.diagram.classes.edges.ClassRelationshipEdge;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.classes.nodes.InterfaceNode;
import com.horstmann.violet.product.diagram.classes.nodes.PackageNode;
import com.horstmann.violet.product.diagram.common.DiagramLinkNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.product.diagram.common.PointNode;

/**
 * This class provides file format services
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public class FileCompatiliblyServiceToDelete
{

    /**
     * This filter guarantees compatibility for Violet 0.16 file format
     * 
     * @param in raw input stream
     * @return converted input stream
     */
    public static InputStream convertFromViolet016(InputStream in)
    {

        Map<String, String> replaceMap = new Hashtable<String, String>();

        // fix framework elements
        replaceMap.put("com.horstmann.violet.BentStyle", BentStyle.class.getName());
        replaceMap.put("com.horstmann.violet.LineStyle", LineStyle.class.getName());
        replaceMap.put("com.horstmann.violet.ArrowHead", ArrowHead.class.getName());

        // fix common elements package
        replaceMap.put("com.horstmann.violet.DiagramLinkNode", DiagramLinkNode.class.getName());
        replaceMap.put("com.horstmann.violet.NoteEdge", GenericEdge.class.getName());
        replaceMap.put("com.horstmann.violet.NoteNode", NoteNode.class.getName());
        replaceMap.put("com.horstmann.violet.PointNode", PointNode.class.getName());

        // fix class diagram package
        replaceMap.put("com.horstmann.violet.ClassDiagramGraph", ClassDiagramGraph.class.getName());
        replaceMap.put("com.horstmann.violet.ClassNode", ClassNode.class.getName());
        replaceMap.put("com.horstmann.violet.ClassRelationshipEdge", ClassRelationshipEdge.class.getName());
        replaceMap.put("com.horstmann.violet.InterfaceNode", InterfaceNode.class.getName());
        replaceMap.put("com.horstmann.violet.PackageNode", PackageNode.class.getName());

        String original = getInputStreamContent(in);
        String replaced = replaceAll(original, replaceMap);
        try
        {
            return new ByteArrayInputStream(replaced.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException ex)
        {
            ex.printStackTrace();
            return new ByteArrayInputStream(replaced.getBytes());
        }
    }

    /**
     * Converts inputStream to String
     * 
     * @param in stream
     * @return string
     */
    private static String getInputStreamContent(InputStream in)
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(in, "UTF-8");
            StringBuffer buffer = new StringBuffer();
            int len = 1024;
            char buf[] = new char[len];
            int numRead;
            while ((numRead = isr.read(buf, 0, len)) != -1)
            {
                buffer.append(buf, 0, numRead);
            }
            isr.close();
            return buffer.toString();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Filters a string and replaces all key courrences issed from the map by its valye
     * 
     * @param input string
     * @param replaceMap key = searchedString / value = replaceString
     * @return filtered string
     */
    private static String replaceAll(String input, Map<String, String> replaceMap)
    {
        Set<String> set = replaceMap.keySet();
        for (Iterator<String> iter = set.iterator(); iter.hasNext();)
        {
            String searchedStr = iter.next();
            String replaceStr = replaceMap.get(searchedStr);
            input = input.replaceAll(searchedStr, replaceStr);
        }
        return input;
    }

    /**
     * This filter guarantees compatibility for Violet 0.16 file format
     * 
     * @param out raw output stream
     * @return converted output stream
     */
    public static OutputStream convertToViolet016(OutputStream out)
    {

        Map<String, String> replaceMap = new Hashtable<String, String>();

        // fix framework elements
        replaceMap.put(BentStyle.class.getName(), "com.horstmann.violet.BentStyle");
        replaceMap.put(LineStyle.class.getName(), "com.horstmann.violet.LineStyle");
        replaceMap.put(ArrowHead.class.getName(), "com.horstmann.violet.ArrowHead");

        // fix common elements package
        replaceMap.put(DiagramLinkNode.class.getName(), "com.horstmann.violet.DiagramLinkNode");
        replaceMap.put(GenericEdge.class.getName(), "com.horstmann.violet.NoteEdge");
        replaceMap.put(NoteNode.class.getName(), "com.horstmann.violet.NoteNode");
        replaceMap.put(PointNode.class.getName(), "com.horstmann.violet.PointNode");

        // fix class diagram package
        replaceMap.put(ClassDiagramGraph.class.getName(), "com.horstmann.violet.ClassDiagramGraph");
        replaceMap.put(ClassNode.class.getName(), "com.horstmann.violet.ClassNode");
        replaceMap.put(ClassRelationshipEdge.class.getName(), "com.horstmann.violet.ClassRelationshipEdge");
        replaceMap.put(InterfaceNode.class.getName(), "com.horstmann.violet.InterfaceNode");
        replaceMap.put(PackageNode.class.getName(), "com.horstmann.violet.PackageNode");

        StringFilterOutputStream filteredOutputStream = new StringFilterOutputStream(out, replaceMap);
        return filteredOutputStream;

    }

}
