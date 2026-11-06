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

package com.horstmann.violet.framework.file.naming;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.framework.plugin.PluginRegistry;
import com.horstmann.violet.product.diagram.abstracts.IGraph;

/**
 * This file provides common file services
 * 
 * @author Alexandre de Pellegrin
 * 
 */
@ManagedBean
public class FileNamingService
{

    /**
     * Default constructor
     */
    public FileNamingService()
    {
        ResourceBundleInjector.getInjector().inject(this);
    }


    /**
     * Edits the file path so that it ends in the desired extension.
     * 
     * @param original the file to use as a starting point
     * @param toBeRemoved the extension that is to be removed before adding the desired extension. Use null if nothing needs to be
     *            removed.
     * @param desired the desired extension (e.g. ".png"), or a | separated list of extensions
     * @return original if it already has the desired extension, or a new file with the edited file path
     */
    public String changeFileExtension(String original, String toBeRemoved, String desired)
    {
        if (original == null) return null;
        int n = desired.indexOf('|');
        if (n >= 0) desired = desired.substring(0, n);
        String path = original;
        if (!path.toLowerCase().endsWith(desired.toLowerCase()))
        {
            if (toBeRemoved != null && path.toLowerCase().endsWith(toBeRemoved.toLowerCase())) path = path.substring(0, path
                    .length()
                    - toBeRemoved.length());
            path = path + desired;
        }
        return path;
    }


    /**
     * @return a map of extension filter per diagram type
     */
    private Map<Class<? extends IGraph>, ExtensionFilter> getExtensionFilters()
    {
        Map<Class<? extends IGraph>, ExtensionFilter> result = new LinkedHashMap<Class<? extends IGraph>, ExtensionFilter>();
        List<IDiagramPlugin> diagramPlugins = this.pluginRegistry.getDiagramPlugins();
        Collections.sort(diagramPlugins, new Comparator<IDiagramPlugin>() {
            @Override
            public int compare(IDiagramPlugin o1, IDiagramPlugin o2)
            {
                return o1.getFileExtensionName().compareToIgnoreCase(o2.getFileExtensionName());
            }
        });
        for (IDiagramPlugin aPlugin : diagramPlugins) {
            Class<? extends IGraph> graphClass = aPlugin.getGraphClass();
            String name = aPlugin.getFileExtensionName();
            String extension = aPlugin.getFileExtension();
            ExtensionFilter fileFilter = new ExtensionFilter(name, extension);
            result.put(graphClass, fileFilter);
        }
        ExtensionFilter defaultFileFilter = new ExtensionFilter(this.defaultFileFilterName, this.defaultFileExtension);
        result.put(IGraph.class, defaultFileFilter);
        return result;
    }

    /**
     * @return all kind of extension file filters
     */
    public ExtensionFilter[] getFileFilters()
    {
        Map<Class<? extends IGraph>, ExtensionFilter> filters = getExtensionFilters();
        Collection<ExtensionFilter> values = filters.values();
        return (ExtensionFilter[]) values.toArray(new ExtensionFilter[values.size()]);
    }

    /**
     * @param graph
     * @return the file filter specific to a graph type
     */
    public ExtensionFilter getExtensionFilter(IGraph graph)
    {
        Map<Class<? extends IGraph>, ExtensionFilter> filters = getExtensionFilters();
        ExtensionFilter filter = filters.get(graph.getClass());
        if (filter != null)
        {
            return filter;
        }
        return filters.get(IGraph.class);
    }

    /**
     * @return the extension filters of the images that can be imported
     */
    public ExtensionFilter[] getImageExtensionFilters()
    {
        ExtensionFilter[] filters = new ExtensionFilter[2];
        filters[0] = new ExtensionFilter(this.imageFileType1FilterName, this.imageFileType1Extension);
        filters[1] = new ExtensionFilter(this.imageFileType2FilterName, this.imageFileType2Extension);
        return filters;
    }

    /**
     * @return the extension filter for image export. PNG only: JPEG can't be encoded in the browser
     *         (no native javajpeg library in CheerpJ), and it blurs the lines of a diagram
     */
    public ExtensionFilter getImageExportExtensionFilter()
    {
        return new ExtensionFilter(this.imageFileType2FilterName, this.imageFileType2Extension);
    }

    /**
     * @return the extension filter for pdf export
     */
    public ExtensionFilter getPdfExtensionFilter()
    {
        ExtensionFilter filter = new ExtensionFilter(this.pdfFileTypeFilterName, this.pdfFileTypeExtension);
        return filter;
    }


    @ResourceBundleBean(key="files.pdf.name")
    private String pdfFileTypeFilterName;

    @ResourceBundleBean(key="files.pdf.extension")
    private String pdfFileTypeExtension;

    @ResourceBundleBean(key="files.image.type1.name")
    private String imageFileType1FilterName;
    
    @ResourceBundleBean(key="files.image.type1.extension")
    private String imageFileType1Extension;
    
    @ResourceBundleBean(key="files.image.type2.name")
    private String imageFileType2FilterName;
    
    @ResourceBundleBean(key="files.image.type2.extension")
    private String imageFileType2Extension;

    @ResourceBundleBean(key="files.global.name")
    private String defaultFileFilterName;
    
    @ResourceBundleBean(key="files.violet016.extension")
    private String oldFileExtension;
    
    @ResourceBundleBean(key="files.violet016.name")
    private String oldFileFilterName;
    
    @ResourceBundleBean(key="files.global.extension")
    private String defaultFileExtension;

    @InjectedBean
    private PluginRegistry pluginRegistry;



}
