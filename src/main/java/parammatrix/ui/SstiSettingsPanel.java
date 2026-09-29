package parammatrix.ui;

import parammatrix.testing.ssti.SstiEngine;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;

public final class SstiSettingsPanel extends JPanel {
    private static final Map<SstiEngine, String> DESCRIPTIONS = descriptions();
    private final Map<SstiEngine, JCheckBox> selections = new EnumMap<>(SstiEngine.class);
    private final JLabel selectionStatus = new JLabel();

    public SstiSettingsPanel() {
        super(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(18, 20, 18, 20));
        add(header(), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        updateSelectionStatus();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("SSTI Engines");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 7f));
        JLabel subtitle = new JLabel(
                "Choose the template engines and parameter policy for future SSTI test modules.");
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        copy.add(title);
        copy.add(subtitle);

        JPanel actions = new JPanel();
        JButton selectAll = new JButton("Select all");
        JButton clear = new JButton("Clear");
        selectAll.addActionListener(ignored -> selectAll(true));
        clear.addActionListener(ignored -> selectAll(false));
        actions.add(selectAll);
        actions.add(clear);
        panel.add(copy, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    private JPanel content() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        for (SstiEngine engine : SstiEngine.values()) {
            grid.add(engineCard(engine));
        }
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setBorder(BorderFactory.createDashedBorder(null));
        JLabel more = new JLabel("Additional providers can be registered through SstiPayloadProvider",
                SwingConstants.CENTER);
        placeholder.add(more, BorderLayout.CENTER);
        grid.add(placeholder);
        content.add(grid);
        content.add(Box.createVerticalStrut(14));
        content.add(policyPanel());
        return content;
    }

    private JPanel engineCard(SstiEngine engine) {
        JPanel card = new JPanel(new BorderLayout(10, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(), new EmptyBorder(12, 12, 12, 12)));
        JCheckBox selected = new JCheckBox(displayName(engine));
        selected.setFont(selected.getFont().deriveFont(Font.BOLD,
                selected.getFont().getSize2D() + 1f));
        selected.setSelected(engine == SstiEngine.GENERIC);
        selected.addActionListener(ignored -> updateSelectionStatus());
        selections.put(engine, selected);

        JTextArea description = new JTextArea(DESCRIPTIONS.get(engine));
        description.setEditable(false);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFocusable(false);
        description.setRows(2);
        card.add(selected, BorderLayout.NORTH);
        card.add(description, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(330, 92));
        return card;
    }

    private JPanel policyPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Test policy"), new EmptyBorder(8, 10, 10, 10)));
        JPanel scope = new JPanel();
        scope.setLayout(new BoxLayout(scope, BoxLayout.Y_AXIS));
        ButtonGroup group = new ButtonGroup();
        JRadioButton all = new JRadioButton("Test all discovered parameters", true);
        JRadioButton reflected = new JRadioButton("Test only reflected parameters");
        group.add(all);
        group.add(reflected);
        scope.add(all);
        scope.add(reflected);

        JPanel methods = new JPanel();
        methods.setLayout(new BoxLayout(methods, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Detection methods");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        methods.add(title);
        methods.add(new JLabel("Arithmetic evaluation  •  Error based"));
        methods.add(new JLabel("Syntax behavior  •  Engine fingerprinting"));
        panel.add(scope, BorderLayout.WEST);
        panel.add(methods, BorderLayout.CENTER);
        panel.add(selectionStatus, BorderLayout.EAST);
        return panel;
    }

    private JPanel footer() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));
        JLabel notice = new JLabel(
                "SSTI payload execution is not enabled in this release. Selections are a configuration preview.");
        notice.setBorder(new EmptyBorder(7, 10, 7, 10));
        panel.add(notice);
        return panel;
    }

    private void selectAll(boolean selected) {
        selections.values().forEach(checkBox -> checkBox.setSelected(selected));
        updateSelectionStatus();
    }

    private void updateSelectionStatus() {
        long count = selections.values().stream().filter(JCheckBox::isSelected).count();
        selectionStatus.setText(count + " engine" + (count == 1 ? "" : "s") + " selected");
    }

    private static String displayName(SstiEngine engine) {
        return switch (engine) {
            case GENERIC -> "Generic";
            case JINJA2 -> "Jinja2";
            case TWIG -> "Twig";
            case FREEMARKER -> "FreeMarker";
            case VELOCITY -> "Velocity";
            case THYMELEAF -> "Thymeleaf";
            case SMARTY -> "Smarty";
        };
    }

    private static Map<SstiEngine, String> descriptions() {
        Map<SstiEngine, String> values = new EnumMap<>(SstiEngine.class);
        values.put(SstiEngine.GENERIC, "Engine-neutral probes for evaluation and syntax changes.");
        values.put(SstiEngine.JINJA2, "Python/Jinja expression behavior and error signatures.");
        values.put(SstiEngine.TWIG, "PHP/Twig arithmetic, filters, and syntax behavior.");
        values.put(SstiEngine.FREEMARKER, "Java FreeMarker expressions and engine fingerprints.");
        values.put(SstiEngine.VELOCITY, "Apache Velocity references, directives, and evaluation.");
        values.put(SstiEngine.THYMELEAF, "Thymeleaf standard and Spring expression patterns.");
        values.put(SstiEngine.SMARTY, "PHP Smarty delimiters, modifiers, and evaluation behavior.");
        return values;
    }
}
