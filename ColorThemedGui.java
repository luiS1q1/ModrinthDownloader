package org.example.modrinth;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;

public class ColorThemedGui extends JFrame {

    private static final Color COLOR_DARK_GREEN = new Color(0x073B3A);
    private static final Color COLOR_ORANGE     = new Color(0xF57C00);
    private static final Color COLOR_TEXT       = Color.WHITE;

    private static final String CONFIG_FILE = System.getProperty("user.home") + "/modrinth_downloader_config.json";

    // Filter-Widgets
    private JTextField searchField;
    private JButton searchButton;

    private JComboBox<String> cbVersion;
    private JComboBox<String> cbLoader;
    private JComboBox<String> cbProjectType;  // NEU

    private JComboBox<String> cbPaths;
    private JButton btnAddPath, btnRemovePath;

    // Sortier-Kategorie
    private JComboBox<String> cbSortFilter;

    private JPanel galleryPanel;
    private JScrollPane scrollPane;

    // Puffern der Suchergebnisse
    private JsonArray currentHits = null;

    public ColorThemedGui() {
        super("Modrinth Downloader");

        // 1) FlatLaf
        FlatLightLaf.setup();

        // 2) UIManager-Farben
        UIManager.put("Panel.background", COLOR_DARK_GREEN);
        UIManager.put("Label.foreground", COLOR_TEXT);
        UIManager.put("Button.background", COLOR_ORANGE);
        UIManager.put("Button.foreground", COLOR_TEXT);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        initComponents();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        // Oben: BoxLayout
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Zeile 1: Suche
        JPanel row1 = new JPanel(new BorderLayout(10, 10));
        row1.setOpaque(false);
        searchField = new JTextField();
        searchButton = new JButton("Suchen");
        row1.add(searchField, BorderLayout.CENTER);
        row1.add(searchButton, BorderLayout.EAST);

        searchField.addActionListener(e -> onSearchClicked()); // ENTER
        searchButton.addActionListener(e -> onSearchClicked()); // Button

        // Zeile 2: Filter (Version / Loader / ProjectType / Sort)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row2.setOpaque(false);

        cbVersion = new JComboBox<>(new String[]{
                "All", "1.8.9", "1.9", "1.9.1", "1.9.2", "1.9.3", "1.9.4",
                "1.10", "1.10.1", "1.10.2", "1.11", "1.11.1", "1.11.2",
                "1.12", "1.12.1", "1.12.2", "1.13", "1.13.1", "1.13.2",
                "1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4",
                "1.15", "1.15.1", "1.15.2", "1.16", "1.16.1", "1.16.2",
                "1.16.3", "1.16.4", "1.16.5",
                "1.17", "1.17.1",
                "1.18", "1.18.1", "1.18.2",
                "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4",
                "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
                "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4"
        });
        cbLoader  = new JComboBox<>(new String[]{"All", "fabric", "forge", "neoforge", "quilt"});
        cbProjectType = new JComboBox<>(new String[]{"All", "mod", "datapack"}); // NEU
        cbSortFilter = new JComboBox<>(new String[]{
                "Sort by Relevance",
                "Sort by Title",
                "Sort by Downloads",
                "Sort by Latest Update"

        });
        cbSortFilter.addActionListener(e -> sortAndRefresh());

        row2.add(new JLabel("Version:"));
        row2.add(cbVersion);
        row2.add(Box.createHorizontalStrut(20));
        row2.add(new JLabel("Loader:"));
        row2.add(cbLoader);
        row2.add(Box.createHorizontalStrut(20));
        row2.add(new JLabel("Project-Type:"));
        row2.add(cbProjectType);
        row2.add(Box.createHorizontalStrut(20));
        row2.add(new JLabel("Sortierung:"));
        row2.add(cbSortFilter);

        // Zeile 3: Pfade
        JPanel row3 = new JPanel(new BorderLayout(10, 10));
        row3.setOpaque(false);

        cbPaths = new JComboBox<>();
        btnAddPath    = new JButton("+ Path");
        btnRemovePath = new JButton("- Path");

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightPanel.setOpaque(false);
        rightPanel.add(btnAddPath);
        rightPanel.add(btnRemovePath);

        row3.add(cbPaths, BorderLayout.CENTER);
        row3.add(rightPanel, BorderLayout.EAST);

        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(row1);
        topPanel.add(row2);
        topPanel.add(row3);
        topPanel.add(Box.createVerticalStrut(10));

        add(topPanel, BorderLayout.NORTH);

        // Galerie mit BoxLayout (vertikal)
        galleryPanel = new JPanel();
        galleryPanel.setLayout(new BoxLayout(galleryPanel, BoxLayout.Y_AXIS));
        galleryPanel.setBackground(COLOR_DARK_GREEN);

        scrollPane = new JScrollPane(galleryPanel);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        // Lade die gespeicherten Pfade
        loadPaths();

        // Listener
        initListeners();
    }

    private void initListeners() {
        // Pfad hinzufügen
        btnAddPath.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                String folder = chooser.getSelectedFile().getAbsolutePath();
                // Duplikate checken
                for (int i = 0; i < cbPaths.getItemCount(); i++) {
                    if (cbPaths.getItemAt(i).equals(folder)) {
                        JOptionPane.showMessageDialog(this, "Path already exists.");
                        return;
                    }
                }
                cbPaths.addItem(folder);
                cbPaths.setSelectedItem(folder);
                savePaths(); // Speichern nach dem Hinzufügen
            }
        });

        // Pfad entfernen
        btnRemovePath.addActionListener(e -> {
            int idx = cbPaths.getSelectedIndex();
            if (idx >= 0) {
                cbPaths.removeItemAt(idx);
                savePaths(); // Speichern nach dem Entfernen
            }
        });
    }

    /**
     * Speichert die aktuellen Pfade in einer JSON-Datei.
     */
    private void savePaths() {
        JsonArray pathsArray = new JsonArray();
        for (int i = 0; i < cbPaths.getItemCount(); i++) {
            pathsArray.add(cbPaths.getItemAt(i));
        }
        JsonObject config = new JsonObject();
        config.add("paths", pathsArray);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Error:\n" + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    /**
     * Lädt die gespeicherten Pfade aus der JSON-Konfigurationsdatei.
     */
    private void loadPaths() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            // Keine gespeicherten Pfade vorhanden
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            JsonObject config = gson.fromJson(reader, JsonObject.class);
            if (config.has("paths")) {
                JsonArray pathsArray = config.getAsJsonArray("paths");
                for (int i = 0; i < pathsArray.size(); i++) {
                    cbPaths.addItem(pathsArray.get(i).getAsString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "\n" + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void onSearchClicked() {
        // Alte Karten weg
        galleryPanel.removeAll();
        galleryPanel.revalidate();
        galleryPanel.repaint();

        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter something");
            return;
        }

        String version = (String) cbVersion.getSelectedItem();
        String loader  = (String) cbLoader.getSelectedItem();
        String projectType = (String) cbProjectType.getSelectedItem(); // NEU
        String facets  = buildFacets(version, loader, projectType); // NEU

        // Async-Suche
        new Thread(() -> {
            try {
                JsonArray hits = ModrinthDownloader.searchMods(query, facets);
                currentHits = hits;

                SwingUtilities.invokeLater(() -> {
                    if (hits.size() == 0) {
                        JOptionPane.showMessageDialog(this, "No mods found.");
                    } else {
                        sortAndRefresh();
                    }
                });
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Error:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void sortAndRefresh() {
        if (currentHits == null || currentHits.size() == 0) return;

        String sortMode = (String) cbSortFilter.getSelectedItem();
        java.util.List<JsonObject> list = new ArrayList<>();
        for (int i = 0; i < currentHits.size(); i++) {
            list.add(currentHits.get(i).getAsJsonObject());
        }

        switch (sortMode) {
            case "Sort by Title":
                list.sort(Comparator.comparing(o ->
                        o.has("title") ? o.get("title").getAsString().toLowerCase() : ""));
                break;
            case "Sort by Downloads":
                list.sort((o1, o2) -> {
                    int d1 = o1.has("downloads") ? o1.get("downloads").getAsInt() : 0;
                    int d2 = o2.has("downloads") ? o2.get("downloads").getAsInt() : 0;
                    return Integer.compare(d2, d1); // absteigend
                });
                break;
            case "Sort by Latest Update":
                // Prüfe, ob JSON 'date_modified' hat (Beispiel)
                list.sort((o1, o2) -> {
                    String date1 = o1.has("date_modified")
                            ? o1.get("date_modified").getAsString() : "1970-01-01T00:00:00Z";
                    String date2 = o2.has("date_modified")
                            ? o2.get("date_modified").getAsString() : "1970-01-01T00:00:00Z";
                    Instant i1 = Instant.parse(date1);
                    Instant i2 = Instant.parse(date2);
                    return i2.compareTo(i1); // neueste zuerst
                });
                break;
            case "Sort by Relevance":
                // Evtl. nichts tun, da 'searchMods' von Modrinth
                // standardmäßig nach Relevanz sortiert
                break;
            default:
                break;
        }

        // Baue currentHits neu
        currentHits = new JsonArray();
        for (JsonObject obj : list) {
            currentHits.add(obj);
        }

        galleryPanel.removeAll();
        for (int i = 0; i < currentHits.size(); i++) {
            addModCard(currentHits.get(i).getAsJsonObject());
        }
        galleryPanel.revalidate();
        galleryPanel.repaint();
    }

    /**
     * Baue Facets (Version, Loader, ProjectType).
     */
    private String buildFacets(String version, String loader, String projectType) {
        java.util.List<String> facetList = new ArrayList<>();

        if (version != null && !"All".equals(version)) {
            facetList.add("versions:" + version);
        }
        if (loader != null && !"All".equals(loader)) {
            facetList.add("categories:" + loader);
        }
        // NEU: project_type:mod / project_type:datapack
        if (projectType != null && !"All".equals(projectType)) {
            facetList.add("project_type:" + projectType);
        }

        if (facetList.isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < facetList.size(); i++) {
            sb.append("[\"").append(facetList.get(i)).append("\"]");
            if (i < facetList.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Baut eine Karte (Titel, Beschreibung, Versions, Icon, etc.)
     * Bei Klick -> Öffnet ModDetailsFrame (altes Layout).
     */
    private void addModCard(JsonObject modHit) {
        String title = modHit.has("title")
                ? modHit.get("title").getAsString()
                : "???";
        String description = modHit.has("description")
                ? modHit.get("description").getAsString()
                : "Keine Beschreibung vorhanden.";
        String projectId = modHit.get("project_id").getAsString();
        String iconUrl = modHit.has("icon_url")
                ? modHit.get("icon_url").getAsString()
                : null;

        // Karte
        JPanel card = new JPanel(new BorderLayout(10,10));
        card.setMaximumSize(new Dimension(900, 180));
        card.setBackground(COLOR_DARK_GREEN.darker());
        card.setBorder(BorderFactory.createLineBorder(COLOR_ORANGE, 2));

        // Link: Icon
        JLabel imgLabel = new JLabel();
        imgLabel.setPreferredSize(new Dimension(130, 130));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (iconUrl != null && !iconUrl.isBlank()) {
            new Thread(() -> {
                try {
                    var img = ImageIO.read(new URL(iconUrl));
                    if (img != null) {
                        var scaled = img.getScaledInstance(130, 130, Image.SCALE_SMOOTH);
                        SwingUtilities.invokeLater(() -> imgLabel.setIcon(new ImageIcon(scaled)));
                    } else {
                        SwingUtilities.invokeLater(() -> imgLabel.setText("Error"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> imgLabel.setText("Error"));
                }
            }).start();
        } else {
            imgLabel.setText("No picture");
        }
        card.add(imgLabel, BorderLayout.WEST);

        // Rechts: Titel, Beschreibung, Versions, Tags
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(COLOR_TEXT);

        JLabel descLabel = new JLabel("<html>" + description + "</html>");
        descLabel.setForeground(COLOR_TEXT);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(descLabel);

        rightPanel.add(textPanel, BorderLayout.NORTH);

        // Unten: Versions, Categories
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bottomPanel.setOpaque(false);

        // Versions
        if (modHit.has("game_versions")) {
            JsonArray versions = modHit.getAsJsonArray("game_versions");
            for (int i = 0; i < Math.min(5, versions.size()); i++) {
                String v = versions.get(i).getAsString();
                JLabel vLabel = new JLabel(v);
                vLabel.setForeground(COLOR_ORANGE);
                bottomPanel.add(vLabel);
            }
        }

        // Categories
        if (modHit.has("categories")) {
            JsonArray cats = modHit.getAsJsonArray("categories");
            for (int i = 0; i < cats.size(); i++) {
                String cat = cats.get(i).getAsString();
                JLabel catLabel = new JLabel(cat);
                catLabel.setOpaque(true);
                catLabel.setBackground(COLOR_ORANGE);
                catLabel.setForeground(COLOR_TEXT);
                catLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
                bottomPanel.add(catLabel);
            }
        }

        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        card.add(rightPanel, BorderLayout.CENTER);

        // Klick -> ModDetailsFrame
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 1) {
                    String activePath = (String) cbPaths.getSelectedItem();
                    if (activePath == null || activePath.isEmpty()) {
                        JOptionPane.showMessageDialog(card, "Please choose a mod path!");
                        return;
                    }
                    // Starte das alte Layout: ModDetailsFrame
                    new ModDetailsFrame(projectId, activePath);
                }
            }
        });

        // Vertikale Ausrichtung
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        galleryPanel.add(card);
        galleryPanel.add(Box.createVerticalStrut(10));
        galleryPanel.revalidate();
        galleryPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ColorThemedGui::new);
    }
}
