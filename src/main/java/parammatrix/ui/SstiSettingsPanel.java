package parammatrix.ui;

import parammatrix.testing.ssti.SstiEngine;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public final class SstiSettingsPanel extends JPanel {
    public SstiSettingsPanel() {
        super(new BorderLayout());
        JPanel engines = new JPanel(new GridLayout(0, 2));
        engines.setBorder(BorderFactory.createTitledBorder("SSTI Engines (Phase 6 skeleton)"));
        for (SstiEngine engine : SstiEngine.values()) {
            JCheckBox box = new JCheckBox(engine.name());
            box.setEnabled(false);
            engines.add(box);
        }
        add(new JLabel("Payload execution is intentionally not implemented in this release."),
                BorderLayout.NORTH);
        add(engines, BorderLayout.CENTER);
    }
}

