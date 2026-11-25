package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.event.MouseWheelEvent;

import com.horstmann.violet.framework.util.KeyModifierUtil;
import com.horstmann.violet.workspace.editorpart.IEditorPart;

public class ZoomByWheelBehavior extends AbstractEditorPartBehavior
{

    private IEditorPart editorPart;

    public ZoomByWheelBehavior(IEditorPart editorPart)
    {
        this.editorPart = editorPart;
    }

    @Override
    public void onMouseWheelMoved(MouseWheelEvent event)
    {
        if (!KeyModifierUtil.isCtrl(event))
        {
            return;
        }
        int scroll = event.getUnitsToScroll();
        if (scroll < 0)
        {
            this.editorPart.changeZoom(1, event.getPoint());
        }
        if (scroll > 0)
        {
            this.editorPart.changeZoom(-1, event.getPoint());
        }
    }

}
