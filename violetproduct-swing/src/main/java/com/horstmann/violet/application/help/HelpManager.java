package com.horstmann.violet.application.help;

import java.awt.Toolkit;
import java.text.MessageFormat;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import com.horstmann.violet.application.cheerpj.CheerpJInterfaceService;
import com.horstmann.violet.application.menu.MenuFactory;
import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.util.BrowserLauncher;
import com.horstmann.violet.framework.util.ClipboardPipe;

@ResourceBundleBean(resourceReference = MenuFactory.class)
public class HelpManager
{
    private static HelpManager instance;
    
    private HelpManager() {
        ResourceBundleInjector.getInjector().inject(this);
    }

    public static HelpManager getInstance() {
        if (instance == null) {
            instance = new HelpManager();
        }
        return instance;
    }
    




    /**
     * Opens online help
     */
    public void openUserGuide()
    {
        // Try using the JS bridge first; call openUrl directly and fall back
        // to the desktop launcher if it fails. This attempts the native bridge
        // even if availability check earlier returned false due to timing.
        try {
            CheerpJInterfaceService.openUrl(this.userGuideURL);
            return;
        } catch (Exception ex) {
            // fall back to desktop behavior
        }
        boolean isOK = BrowserLauncher.openURL(this.userGuideURL);
        if (!isOK)
        {
            showErrorMessage(this.userGuideURL);
        }

    }

    /**
     * Goes to homepage
     */
    public void openHomepage()
    {
        // Try using the JS bridge first; call openUrl directly and fall back
        // to the desktop launcher if it fails. This attempts the native bridge
        // even if availability check earlier returned false due to timing.
        try {
            CheerpJInterfaceService.openUrl(this.homePageURL);
            return;
        } catch (Exception ex) {
            // fall back to desktop behavior
        }
        boolean isOK = BrowserLauncher.openURL(this.homePageURL);
        if (!isOK)
        {
            showErrorMessage(this.homePageURL);
        }

    }

    /**
     * Launch web browser or copy url to clipoard if failed
     * 
     * @param url
     */
    private void showErrorMessage(String url)
    {
        String message = MessageFormat.format(this.errorMessageTemplate, new Object[]
        {
            url
        });
        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(message);
        optionPane.setIcon(this.errorImageIcon);
        ClipboardPipe pipe = new ClipboardPipe(url);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(pipe, null);
        getDialogFactory().showDialog(optionPane, this.errorDialogBoxTitle, true);
    }

    private DialogFactory getDialogFactory()
    {
        if (this.dialogFactory == null)
        {
            BeanInjector.getInjector().inject(this);
        }
        return this.dialogFactory;
    }

    @InjectedBean
    private DialogFactory dialogFactory;

    @ResourceBundleBean(key = "help.userguide.url")
    private String userGuideURL;
    
    @ResourceBundleBean(key = "help.homepage.url")
    private String homePageURL;
    
    @ResourceBundleBean(key = "dialog.open_url_failed.ok")
    private String errorMessageTemplate;
    
    @ResourceBundleBean(key = "dialog.open_url_failed.title")
    private String errorDialogBoxTitle;
    
    @ResourceBundleBean(key = "dialog.open_url_failed.icon")
    private ImageIcon errorImageIcon;
    
}
