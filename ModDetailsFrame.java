package org.example.modrinth;

import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;

public class ModDetailsFrame extends JFrame {

    private static final Color COLOR_DARK_GREEN = new Color(0x073B3A);
    private static final Color COLOR_ORANGE     = new Color(0xF57C00);
    private static final Color COLOR_TEXT       = Color.WHITE;

    private final String projectId;
    private final String modsFolderPath;

    private JLabel iconLabel;
    private JLabel nameLabel;
    private JTextArea descArea;
    private JButton versionsButton;

    public ModDetailsFrame(String projectId, String modsFolderPath) {
        super("Mod-Details");
        this.projectId = projectId;
        this.modsFolderPath = modsFolderPath;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 500);

        initComponents();
        loadData();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        // Hintergrund anpassen, falls du es zum restlichen Theme angleichen willst
        getContentPane().setBackground(COLOR_DARK_GREEN);

        setLayout(new BorderLayout(10, 10));

        // Oberer Bereich: Icon + Name
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setOpaque(false);

        iconLabel = new JLabel("Icon...");
        iconLabel.setPreferredSize(new Dimension(128, 128));

        nameLabel = new JLabel("Mod-Name");
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 24f));
        nameLabel.setForeground(COLOR_TEXT);

        topPanel.add(iconLabel);
        topPanel.add(nameLabel);

        add(topPanel, BorderLayout.NORTH);

        // Beschreibung in der Mitte
        descArea = new JTextArea("Beschreibung...");
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setForeground(COLOR_TEXT);
        descArea.setBackground(COLOR_DARK_GREEN.darker());

        JScrollPane scroll = new JScrollPane(descArea);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(COLOR_DARK_GREEN.darker());
        scroll.setOpaque(false);

        add(scroll, BorderLayout.CENTER);

        // Unten: Versions-Button
        versionsButton = new JButton("Versions...");
        versionsButton.setBackground(COLOR_ORANGE);
        versionsButton.setForeground(COLOR_TEXT);
        versionsButton.addActionListener(this::onVersionsClicked);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.add(versionsButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Lädt per Modrinth-API die Details (Titel, Beschreibung, icon_url)
     * und aktualisiert das UI asynchron.
     */
    private void loadData() {
        new Thread(() -> {
            try {
                // API-Aufruf: /project/{projectId}
                JsonObject project = ModrinthDownloader.getProjectDetails(projectId);

                String iconUrl = project.has("icon_url")
                        ? project.get("icon_url").getAsString()
                        : null;
                String name = project.has("title")
                        ? project.get("title").getAsString()
                        : projectId;
                String desc = project.has("description")
                        ? project.get("description").getAsString()
                        : "(Keine Beschreibung)";

                // UI-Update im EDT
                SwingUtilities.invokeLater(() -> {
                    nameLabel.setText(name);
                    descArea.setText(desc);
                });

                // Icon asynchron laden
                if (iconUrl != null && !iconUrl.isBlank()) {
                    try {
                        var img = ImageIO.read(new URL(iconUrl));
                        if (img != null) {
                            var scaled = img.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
                            SwingUtilities.invokeLater(() -> {
                                iconLabel.setIcon(new ImageIcon(scaled));
                                iconLabel.setText("");
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> iconLabel.setText("No Icon"));
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> iconLabel.setText("Icon Error"));
                    }
                } else {
                    SwingUtilities.invokeLater(() -> iconLabel.setText("No Icon"));
                }

            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Fehler beim Laden der Mod-Details:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    /**
     * Klick auf "Versions..." -> Neues Fenster (ModVersionsFrame).
     */
    private void onVersionsClicked(ActionEvent e) {
        new ModVersionsFrame(projectId, modsFolderPath);
    }
}
