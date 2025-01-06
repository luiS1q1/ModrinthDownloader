package org.example.modrinth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class ModVersionsFrame extends JFrame {

    // ---- NEUE FARBCONSTANTEN ----
    private static final Color COLOR_DARK_GREEN = new Color(0x073B3A);
    private static final Color COLOR_TEXT       = Color.WHITE;
    private static final Color COLOR_ORANGE     = new Color(0xF57C00);
    // --------------------------------

    private final String projectId;
    private final String modsFolderPath;

    private DefaultListModel<JsonObject> versionListModel;
    private JList<JsonObject> versionList;
    private JButton downloadButton;

    public ModVersionsFrame(String projectId, String modsFolderPath) {
        super("Mod-Versionen – " + projectId);
        this.projectId = projectId;
        this.modsFolderPath = modsFolderPath;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 400);

        initComponents();
        loadVersions();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        // Hintergründe für das ganze Fenster (optional)
        getContentPane().setBackground(COLOR_DARK_GREEN);

        setLayout(new BorderLayout());
        versionListModel = new DefaultListModel<>();
        versionList = new JList<>(versionListModel);

        // ---------- FARBEN am JList festlegen ----------
        versionList.setBackground(COLOR_DARK_GREEN.darker());  // dunkleres Grün
        versionList.setForeground(COLOR_TEXT);                 // weiße Schrift
        versionList.setSelectionBackground(COLOR_ORANGE);      // orange bei Auswahl
        versionList.setSelectionForeground(COLOR_TEXT);        // Schrift bleibt weiß bei Auswahl

        // Custom Renderer, damit der Hintergrund bei "nicht ausgewählt" auch stimmt
        versionList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            // Zeige name, game_versions, loaders
            String name = value.has("name") ? value.get("name").getAsString() : "???";
            String gameVersions = value.has("game_versions")
                    ? value.getAsJsonArray("game_versions").toString()
                    : "[]";
            String loaders = value.has("loaders")
                    ? value.getAsJsonArray("loaders").toString()
                    : "[]";
            String text = name + " (MC: " + gameVersions + ", Loader: " + loaders + ")";

            JLabel lbl = new JLabel(text);

            // Hintergrund und Schrift beim Auswählen / Nicht-Auswählen
            if (isSelected) {
                lbl.setOpaque(true);
                lbl.setBackground(versionList.getSelectionBackground());
                lbl.setForeground(versionList.getSelectionForeground());
            } else {
                lbl.setOpaque(true);
                lbl.setBackground(versionList.getBackground());
                lbl.setForeground(versionList.getForeground());
            }

            return lbl;
        });

        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);

        // Button-Farben (orange + weiße Schrift)
        downloadButton.setBackground(COLOR_ORANGE);
        downloadButton.setForeground(COLOR_TEXT);

        // ScrollPane für die Liste
        JScrollPane scrollPane = new JScrollPane(versionList);
        // ScrollPane-Viewport-Hintergrund angleichen
        scrollPane.getViewport().setBackground(versionList.getBackground());
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        // Unten ein Panel für den Button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottom.setBackground(COLOR_DARK_GREEN); // optional: grün im Hintergrund
        bottom.add(downloadButton);
        add(bottom, BorderLayout.SOUTH);

        versionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                downloadButton.setEnabled(!versionList.isSelectionEmpty());
            }
        });

        downloadButton.addActionListener(this::onDownloadClicked);
    }

    private void loadVersions() {
        new Thread(() -> {
            try {
                JsonArray versions = ModrinthDownloader.getProjectVersions(projectId);
                SwingUtilities.invokeLater(() -> {
                    if (versions.size() == 0) {
                        JOptionPane.showMessageDialog(this, "Keine Versionen gefunden!");
                    } else {
                        for (int i = 0; i < versions.size(); i++) {
                            versionListModel.addElement(versions.get(i).getAsJsonObject());
                        }
                    }
                });
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Fehler beim Laden der Versionen:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void onDownloadClicked(ActionEvent e) {
        int idx = versionList.getSelectedIndex();
        if (idx < 0) return;

        JsonObject chosenVersion = versionListModel.get(idx);
        new Thread(() -> {
            try {
                ModrinthDownloader.downloadVersion(chosenVersion, modsFolderPath);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Download abgeschlossen!"));
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Fehler beim Download:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }
}
