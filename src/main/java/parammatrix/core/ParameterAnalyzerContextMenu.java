package parammatrix.core;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public final class ParameterAnalyzerContextMenu implements ContextMenuItemsProvider {
    private final ExtensionController controller;

    public ParameterAnalyzerContextMenu(ExtensionController controller) {
        this.controller = controller;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<HttpRequestResponse> selected = new ArrayList<>(event.selectedRequestResponses());
        event.messageEditorRequestResponse().ifPresent(editor -> {
            if (selected.isEmpty()) selected.add(editor.requestResponse());
        });
        if (selected.isEmpty()) return List.of();

        JMenu menu = new JMenu("Parameter Analyzer");
        menu.add(item("Extract Parameters", selected, ExtensionController.Action.EXTRACT));
        menu.add(item("Test Reflections", selected, ExtensionController.Action.TEST));
        menu.add(item("Extract & Test", selected, ExtensionController.Action.EXTRACT_AND_TEST));
        return List.of(menu);
    }

    private JMenuItem item(String label, List<HttpRequestResponse> exchanges,
                           ExtensionController.Action action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(ignored -> exchanges.stream().filter(HttpRequestResponse::hasResponse)
                .forEach(exchange -> controller.submitManual(exchange, action)));
        return item;
    }
}

