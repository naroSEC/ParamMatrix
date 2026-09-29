package parammatrix.ui;

import parammatrix.core.ActiveTaskQueue;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

final class GlobalQueueBar extends JPanel {
    private final ActiveTaskQueue queue;
    private final JLabel status = new JLabel();
    private final JButton pause = new JButton("Pause Queue");

    GlobalQueueBar(ActiveTaskQueue queue) {
        super(new BorderLayout(12, 0));
        this.queue = queue;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().darker()),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel title = new JLabel("Activity");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        summary.add(title);
        summary.add(status);

        pause.setToolTipText("Pause new queued work; requests already running are allowed to finish");
        pause.addActionListener(ignored -> togglePause());
        JButton clear = new JButton("Clear Queued Tasks");
        clear.setToolTipText("Discard work that has not started; active requests are not interrupted");
        clear.addActionListener(ignored -> {
            int discarded = queue.clearPending();
            updateStatus(discarded == 1 ? "1 task cleared" : discarded + " tasks cleared");
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(pause);
        actions.add(clear);

        add(summary, BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);
        updateStatus(null);
        Timer timer = new Timer(500, ignored -> updateStatus(null));
        timer.start();
    }

    private void togglePause() {
        if (queue.isPaused()) queue.resume();
        else queue.pause();
        updateStatus(null);
    }

    private void updateStatus(String message) {
        pause.setText(queue.isPaused() ? "Resume Queue" : "Pause Queue");
        String state = queue.isPaused() ? "Paused" : "Running";
        String value = state + "  •  Active: " + queue.activeCount()
                + "  •  Queued: " + queue.queuedCount();
        if (message != null) value += "  •  " + message;
        status.setText(value);
    }
}
