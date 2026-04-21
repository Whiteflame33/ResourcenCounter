package de.whiteflame.rescount.ui;

import de.whiteflame.rescount.api.app.IAppService;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;
import de.whiteflame.rescount.api.ui.IAppUi;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SwingAppUiImpl implements IAppUi, IAppService.IAppListener {
    private static final ILogger LOGGER = LoggerFactory.getLogger(SwingAppUiImpl.class);

    private final IAppService service;
    private final String key;
    private final Runnable onSaveAction;

    private JLabel display;

    public SwingAppUiImpl(IAppService service, String key, Runnable onSaveAction) {
        this.service = service;
        this.key = key;
        this.onSaveAction = onSaveAction;

        this.service.addListener(this);
    }

    @Override
    public void show() {
        LOGGER.trace("Creating window");
        JFrame frame = new JFrame();
        frame.setSize(300, 80);

        LOGGER.trace("Creating panel and adding it to window");
        JPanel p = new JPanel();
        frame.add(p);

        p.add(new JLabel(key + ":"));
        LOGGER.trace("Creating label with text '{}'", key + ":");

        display = new JLabel(String.valueOf(service.getCount(key)));
        p.add(display);
        LOGGER.trace("Creating label with text '{}'", display.getText());

        JButton button = new JButton("+1");
        p.add(button);
        LOGGER.trace("Creating button with text '{}'", button.getText());

        button.addActionListener(e -> {
            service.increment(key);
            LOGGER.trace("Button was pressed");
        });

        LOGGER.trace("Displaying window");
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                service.shutdown();
                onSaveAction.run();
                frame.dispose();
                System.exit(0);
            }
        });
    }

    @Override
    public void onUpdate(String key, int value) {
        if (display == null)
            return;

        display.setText(String.valueOf(value));
    }
}
