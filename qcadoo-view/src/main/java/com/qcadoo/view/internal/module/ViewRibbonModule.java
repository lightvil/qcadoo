package com.qcadoo.view.internal.module;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.w3c.dom.Node;

import com.google.common.base.Preconditions;
import com.qcadoo.plugin.api.Module;
import com.qcadoo.view.api.ribbon.RibbonGroup;
import com.qcadoo.view.internal.api.InternalViewDefinition;
import com.qcadoo.view.internal.api.InternalViewDefinitionService;
import com.qcadoo.view.internal.components.window.WindowComponentPattern;
import com.qcadoo.view.internal.xml.ViewDefinitionParser;
import com.qcadoo.view.internal.xml.ViewExtension;

public class ViewRibbonModule extends Module {

    private final String pluginIdentifier;

    private final InternalViewDefinitionService viewDefinitionService;

    private final ViewDefinitionParser viewDefinitionParser;

    private final ViewExtension viewExtension;

    private Map<WindowComponentPattern, RibbonGroup> addedGroups;

    public ViewRibbonModule(final String pluginIdentifier, final Resource xmlFile,
            final InternalViewDefinitionService viewDefinitionService, final ViewDefinitionParser viewDefinitionParser) {
        this.pluginIdentifier = pluginIdentifier;
        this.viewDefinitionService = viewDefinitionService;
        this.viewDefinitionParser = viewDefinitionParser;
        try {
            viewExtension = viewDefinitionParser.getViewExtensionNode(xmlFile.getInputStream(), "ribbonExtension");
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void enableOnStartup() {
        enable();
    }

    @Override
    public void enable() {
        addedGroups = new HashMap<WindowComponentPattern, RibbonGroup>();

        InternalViewDefinition viewDefinition = viewDefinitionService.getWithoutSession(viewExtension.getPluginName(),
                viewExtension.getViewName());
        Preconditions.checkNotNull(viewDefinition, getErrorMessage("reference to view which not exists", viewExtension));

        for (Node groupNode : viewDefinitionParser.geElementChildren(viewExtension.getExtesionNode())) {

            RibbonGroup group = viewDefinitionParser.parseRibbonGroup(groupNode, viewDefinition);
            group.setExtensionPluginIdentifier(pluginIdentifier);

            WindowComponentPattern window = viewDefinition.getRootWindow();
            Preconditions.checkNotNull(window, getErrorMessage("cannot add ribbon element to view", viewExtension));

            window.getRibbon().addGroup(group);
            addedGroups.put(window, group);
        }
    }

    @Override
    public void disable() {
        for (Map.Entry<WindowComponentPattern, RibbonGroup> addedGroupEntry : addedGroups.entrySet()) {
            addedGroupEntry.getKey().getRibbon().removeGroup(addedGroupEntry.getValue());
        }
    }

    private String getErrorMessage(final String msg, final ViewExtension viewExtension) {
        return "View ribbon extension error [to " + viewExtension.getPluginName() + "-" + viewExtension.getViewName() + "]: "
                + msg;
    }
}
