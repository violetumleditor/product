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

package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.PropertyEditorSupport;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.naming.FileNamingService;
import com.horstmann.violet.framework.file.persistence.IFileReader;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;

/**
 * A PropertyEditor for {@link java.awt.Image} fields that allows the user to
 * browse the filesystem and pick an image file.
 * <p>
 * Only the pixel data ({@link java.awt.Image}) is stored once a file has been
 * chosen; the source file path is intentionally discarded immediately after
 * loading so that diagrams are self-contained and portable.
 * </p>
 * <p>
 * Displays a small thumbnail of the current image and a "Browse…" button.
 * </p>
 */
public class ImageEditor extends PropertyEditorSupport
{
    private static final int THUMB_W = 100;
    private static final int THUMB_H =  75;

    @InjectedBean
    private IFileChooserService fileChooserService;

    @InjectedBean
    private FileNamingService fileNamingService;

    public ImageEditor()
    {
        super();
        BeanInjector.getInjector().inject(this);
    }

    @Override
    public boolean supportsCustomEditor()
    {
        return true;
    }

    @Override
    public java.awt.Component getCustomEditor()
    {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        
        // ── Thumbnail ──────────────────────────────────────────────────────────
        final JLabel thumbnailLabel = new JLabel();
        thumbnailLabel.setPreferredSize(new Dimension(THUMB_W, THUMB_H));
        thumbnailLabel.setHorizontalAlignment(SwingConstants.CENTER);
        thumbnailLabel.setBorder(BorderFactory.createEtchedBorder());
        refreshThumbnail(thumbnailLabel);

        // ── Browse button ──────────────────────────────────────────────────────
        JButton browseButton = new JButton("Browse\u2026");
        browseButton.addActionListener(e -> {
            try
            {
                ExtensionFilter[] filters = fileNamingService.getImageExtensionFilters();
                IFileReader fileReader = fileChooserService.chooseAndGetFileReader(filters);
                if (fileReader == null) return;
                IFile selectedFile = fileReader.getFileDefinition();
                if (selectedFile == null) return;

                String fullPath = selectedFile.getDirectory() + File.separator + selectedFile.getFilename();
                // Load pixels – path is NOT kept (intentional). Not new ImageIcon(fullPath): it caches
                // by path, so another image with the same name would give the previous pixels.
                ImageIcon loaded = new ImageIcon(Toolkit.getDefaultToolkit().createImage(fullPath));
                Image img = loaded.getImage();

                setValue(img);             // updates the bean property
                refreshThumbnail(thumbnailLabel);
                firePropertyChange();      // notifies CustomPropertyEditor listener
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        });

        panel.add(thumbnailLabel, BorderLayout.CENTER);
        panel.add(browseButton,   BorderLayout.SOUTH);
        return panel;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void refreshThumbnail(JLabel label)
    {
        Image img = (Image) getValue();
        if (img == null)
        {
            label.setIcon(null);
            label.setText("No image");
            return;
        }
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w > 0 && h > 0)
        {
            double scale = Math.min((double) THUMB_W / w, (double) THUMB_H / h);
            int tw = Math.max(1, (int) (w * scale));
            int th = Math.max(1, (int) (h * scale));
            BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = thumb.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(img, 0, 0, tw, th, null);
            g2.dispose();
            label.setIcon(new ImageIcon(thumb));
            label.setText(null);
        }
        else
        {
            label.setIcon(null);
            label.setText("(loading\u2026)");
        }
    }
}
