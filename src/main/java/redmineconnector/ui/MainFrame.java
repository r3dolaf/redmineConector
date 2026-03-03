package redmineconnector.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;

import redmineconnector.config.ConfigManager;

import redmineconnector.ui.theme.ThemeConfig;
import redmineconnector.ui.theme.ThemeManager;
import redmineconnector.ui.components.LogPanel;
import redmineconnector.notifications.NotificationManager;
import redmineconnector.model.LogEntry.Level;
import redmineconnector.ui.dialogs.ClientManagerDialog;
import redmineconnector.ui.dialogs.GlobalSearchDialog;

import java.awt.SystemTray;

import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.event.KeyEvent;

public class MainFrame extends JFrame {

    private static final String APP_TITLE = redmineconnector.util.I18n.get("main.title");

    private final List<InstanceController> controllers = new ArrayList<>();
    private final LogPanel logPanel;
    private final Properties appProperties;
    private final KeyboardShortcutManager shortcuts;
    private JTabbedPane tabbedPane;
    private JMenuBar menuBar;
    private final javax.swing.JProgressBar progressBar = new javax.swing.JProgressBar(0, 100);

    public MainFrame() {
        super(APP_TITLE);
        // Handle window closing gracefully to ensure cache is saved
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (SystemTray.isSupported()) {
                    try {
                        minimizeToTray();
                    } catch (Exception ex) {
                        logToGlobal("ERROR: Fallo al minimizar al tray: " + ex.getMessage());
                        exitApp();
                    }
                } else {
                    exitApp();
                }
            }

            @Override
            public void windowIconified(java.awt.event.WindowEvent e) {
                // Optional: Auto-hide from taskbar when minimized if using tray
                // setVisible(false); // Can be annoying if not expected
            }
        });

        setSize(1550, 950);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Enable verbose logging (DEBUG mode)
        redmineconnector.util.LoggerUtil.setDebugEnabled(true);

        // 1. Inicialización del Logger Global
        logPanel = new LogPanel();

        logToGlobal(redmineconnector.util.I18n.get("main.log.start"));
        logToGlobal(redmineconnector.util.I18n.get("main.log.config"));

        // 2. Carga de propiedades y creación de Controladores
        appProperties = ConfigManager.loadConfig();

        // Cargar y aplicar tema
        ThemeConfig savedTheme = ConfigManager.loadTheme(appProperties);
        ThemeManager.setTheme(savedTheme);
        logToGlobal(redmineconnector.util.I18n.format("main.log.theme", savedTheme.getDisplayName()));

        // Inicializar notificaciones
        try {
            java.awt.Image icon = createDefaultIcon();
            setIconImage(icon); // Update Frame Icon
            if (NotificationManager.initialize("Redmine Connector", icon)) {

                // Configure Tray Actions
                PopupMenu popup = new PopupMenu();
                MenuItem restoreItem = new MenuItem("Restaurar");
                MenuItem exitItem = new MenuItem("Salir");

                restoreItem.addActionListener(e -> restoreFromTray());
                exitItem.addActionListener(e -> exitApp());

                popup.add(restoreItem);
                popup.addSeparator();
                popup.add(exitItem);

                NotificationManager.setPopupMenu(popup);
                NotificationManager.addActionListener(e -> restoreFromTray()); // Double click

                logToGlobal(redmineconnector.util.I18n.get("main.log.notif.ok"));
            }
        } catch (Exception e) {
            logToGlobal(redmineconnector.util.I18n.format("main.log.notif.err", e.getMessage()));
        }

        initializeControllers();

        // 3. Construcción de la UI
        setJMenuBar(createAppMenuBar());

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(225, 230, 235));
        tabbedPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        enableTabReordering(tabbedPane);

        refreshTabs();

        add(tabbedPane, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 20));
        footer.add(progressBar);

        add(footer, BorderLayout.SOUTH);

        // Aplicar tema a toda la ventana
        ThemeManager.applyTheme(this);

        // Configurar atajos de teclado globales
        shortcuts = new KeyboardShortcutManager(getRootPane());
        setupGlobalShortcuts();
        setupHelpShortcut();

        logToGlobal(redmineconnector.util.I18n.get("main.log.ready"));

        // 4. Carga asíncrona inicial de datos
        SwingUtilities.invokeLater(() -> {
            for (InstanceController c : controllers) {
                c.refreshData();
            }
        });
    }

    private void initializeControllers() {
        controllers.clear();
        String clientList = appProperties.getProperty("clients.list", "client1,client2");
        if (clientList.trim().isEmpty())
            clientList = "client1,client2";

        String[] clients = clientList.split(",");
        for (String clientId : clients) {
            String id = clientId.trim();
            if (!id.isEmpty()) {
                String name = appProperties.getProperty(id + ".clientName", "Cliente " + id);
                InstanceController c = new InstanceController(id, name, appProperties, msg -> logToGlobal(name, msg));
                controllers.add(c);
            }
        }

        // Update peers ensuring everyone knows everyone else
        for (InstanceController c : controllers) {
            c.setPeers(controllers);
            c.setOnSyncMatch(found -> updateTabHeader(c, found));
        }
    }

    private void updateTabHeader(InstanceController c, boolean matched) {
        int idx = tabbedPane.indexOfComponent(c.getView());
        if (idx == -1)
            return;

        String title = matched ? redmineconnector.util.I18n.format("main.tab.detached", c.getTitle()) : c.getTitle();
        tabbedPane.setTitleAt(idx, title);

        // Update custom component if present
        Component comp = tabbedPane.getTabComponentAt(idx);
        if (comp instanceof JPanel) {
            JPanel pnl = (JPanel) comp;
            for (Component sub : pnl.getComponents()) {
                if (sub instanceof JLabel) {
                    ((JLabel) sub).setText(title);
                    break;
                }
            }
        }
    }

    private void refreshTabs() {
        tabbedPane.removeAll();
        for (InstanceController c : controllers) {
            addDetachableTab(c);
            if (c.getConfig().isDetached) {
                // Detach immediately if configured
                SwingUtilities.invokeLater(() -> detachTab(c));
            }
        }
    }

    private void addDetachableTab(InstanceController c) {
        // Add the tab normally first
        tabbedPane.addTab(c.getTitle(), c.getView());
        int index = tabbedPane.indexOfComponent(c.getView());

        // Create custom tab header
        JPanel pnlTab = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
        pnlTab.setOpaque(false);
        JLabel lblTitle = new JLabel(c.getTitle());
        JButton btnDetach = new JButton("↗");
        btnDetach.setToolTipText(redmineconnector.util.I18n.get("main.btn.detach.tooltip"));
        btnDetach.setMargin(new java.awt.Insets(0, 2, 0, 2));
        btnDetach.setPreferredSize(new Dimension(20, 20));
        btnDetach.setBorder(BorderFactory.createEmptyBorder());
        btnDetach.setContentAreaFilled(false);
        btnDetach.setFocusable(false);

        // Hover effect for button
        btnDetach.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnDetach.setContentAreaFilled(true);
                btnDetach.setBackground(new Color(220, 220, 220));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                btnDetach.setContentAreaFilled(false);
            }
        });

        btnDetach.addActionListener(e -> detachTab(c));

        pnlTab.add(lblTitle);
        pnlTab.add(btnDetach);

        tabbedPane.setTabComponentAt(index, pnlTab);
    }

    private void detachTab(InstanceController c) {
        int index = tabbedPane.indexOfComponent(c.getView());
        if (index != -1) {
            tabbedPane.removeTabAt(index);
        }

        // Save state
        c.getConfig().isDetached = true;
        updateConfigProperty(c.getConfig().prefix + ".isDetached", "true");

        JFrame detachedFrame = new JFrame(c.getTitle() + " - " + APP_TITLE);
        detachedFrame.setIconImage(getIconImage());
        detachedFrame.setLayout(new BorderLayout());
        detachedFrame.add(c.getView(), BorderLayout.CENTER);

        detachedFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                reattachTab(c, detachedFrame);
            }
        });

        detachedFrame.setSize(1000, 700);
        detachedFrame.setLocationRelativeTo(null);
        detachedFrame.setVisible(true);

        logToGlobal(redmineconnector.util.I18n.format("main.log.detached", c.getTitle()));
    }

    private void reattachTab(InstanceController c, JFrame frame) {
        frame.dispose();
        // Save state
        c.getConfig().isDetached = false;
        updateConfigProperty(c.getConfig().prefix + ".isDetached", "false");

        // Check if already attached (just in case)
        if (tabbedPane.indexOfComponent(c.getView()) == -1) {
            addDetachableTab(c);
            // Select the newly added tab
            tabbedPane.setSelectedComponent(c.getView());
        }
        logToGlobal(redmineconnector.util.I18n.format("main.log.reattached", c.getTitle()));
    }

    private JMenuBar createAppMenuBar() {
        menuBar = new JMenuBar();
        JMenu mFile = new JMenu("📁 " + redmineconnector.util.I18n.get("main.menu.file"));

        JMenuItem mClientMgr = new JMenuItem("👥 " + redmineconnector.util.I18n.get("main.menu.client_mgr"));
        mClientMgr.addActionListener(e -> openClientManager());
        mFile.add(mClientMgr);
        mFile.addSeparator();

        JMenuItem miExport = new JMenuItem(
                "📤 " + redmineconnector.util.I18n.get("main.menu.export_config", "Exportar Configuración"));
        miExport.addActionListener(e -> exportConfiguration());
        mFile.add(miExport);

        JMenuItem miImport = new JMenuItem(
                "📥 " + redmineconnector.util.I18n.get("main.menu.import_config", "Importar Configuración"));
        miImport.addActionListener(e -> importConfiguration());
        mFile.add(miImport);
        mFile.addSeparator();

        JMenuItem miReset = new JMenuItem(
                "⚠️ " + redmineconnector.util.I18n.get("main.menu.factory_reset", "Restablecer de Fábrica"));
        miReset.addActionListener(e -> factoryReset());
        mFile.add(miReset);
        mFile.addSeparator();

        JMenuItem miExit = new JMenuItem("🚪 " + redmineconnector.util.I18n.get("main.menu.exit"));
        miExit.addActionListener(e -> {
            logToGlobal(redmineconnector.util.I18n.get("main.log.exit"));
            shutdown();
            System.exit(0);
        });

        JMenuItem miDonate = new JMenuItem(redmineconnector.util.I18n.get("main.menu.donate"));
        miDonate.addActionListener(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(
                        "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=alfredojavierpiris@gmail.com&item_name=RedmineConnector+Donation&currency_code=EUR"));
                logToGlobal(redmineconnector.util.I18n.get("main.log.donate"));
            } catch (Exception ex) {
                logToGlobal(redmineconnector.util.I18n.format("main.log.browser.err", ex.getMessage()));
                JOptionPane.showMessageDialog(this,
                        redmineconnector.util.I18n.format("main.msg.browser.err", ex.getMessage()));
            }
        });

        mFile.add(miDonate);
        mFile.addSeparator();

        JMenuItem miManual = new JMenuItem(
                "📖 " + redmineconnector.util.I18n.get("main.menu.manual", "Ver Manual de Usuario"));
        miManual.addActionListener(e -> openManual());
        mFile.add(miManual);

        // Help Menu Item - Opens F1 Help Dialog
        JMenuItem miHelp = new JMenuItem(redmineconnector.util.I18n.get("main.menu.help.shortcuts"));
        miHelp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        miHelp.addActionListener(e -> {
            InstanceController activeController = getActiveController();
            if (activeController != null) {
                activeController.openHelpDialog();
            }
        });
        mFile.add(miHelp);

        JMenu mLang = new JMenu("🌐 " + redmineconnector.util.I18n.get("main.menu.language", "Idioma"));
        JMenuItem miEs = new JMenuItem("Español");
        miEs.addActionListener(e -> changeLanguage("es"));
        JMenuItem miEn = new JMenuItem("English");
        miEn.addActionListener(e -> changeLanguage("en"));
        mLang.add(miEs);
        mLang.add(miEn);
        mFile.add(mLang);

        mFile.addSeparator();
        mFile.add(miExit);
        menuBar.add(mFile);

        // Menú Buscar
        JMenu mSearch = new JMenu("🔍 " + redmineconnector.util.I18n.get("main.menu.search", "Buscar"));
        JMenuItem miGlobalSearch = new JMenuItem(
                redmineconnector.util.I18n.get("main.menu.search.global", "Búsqueda Global"));
        miGlobalSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        miGlobalSearch.addActionListener(e -> openGlobalSearch());
        mSearch.add(miGlobalSearch);
        menuBar.add(mSearch);

        // Menú Vista (Moved next to Archivo)
        JMenu mView = new JMenu("👁️ " + redmineconnector.util.I18n.get("main.menu.view"));

        // Submenu Look & Feel
        JMenu mLookAndFeel = new JMenu("🎨 Look & Feel");
        UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        String currentLAF = UIManager.getLookAndFeel().getClass().getName();

        for (UIManager.LookAndFeelInfo info : lafs) {
            JMenuItem miLAF = new JMenuItem(info.getName());
            if (info.getClassName().equals(currentLAF)) {
                miLAF.setText("✓ " + info.getName());
            }
            miLAF.addActionListener(e -> changeLookAndFeel(info.getClassName(), info.getName()));
            mLookAndFeel.add(miLAF);
        }

        mView.add(mLookAndFeel);
        mView.addSeparator();

        JMenuItem miViewLog = new JMenuItem("📋 " + redmineconnector.util.I18n.get("main.menu.view.log"));
        miViewLog.addActionListener(e -> logPanel.expandLog());
        mView.add(miViewLog);
        menuBar.add(mView);

        // Dynamic Client Menus
        for (InstanceController c : controllers) {
            menuBar.add(createClientMenu(c, c.getTitle()));
        }

        // Add notification button to the right side of menu bar
        menuBar.add(javax.swing.Box.createHorizontalGlue());
        redmineconnector.ui.components.NotificationButton btnNotifications = new redmineconnector.ui.components.NotificationButton(
                () -> {
                    javax.swing.JDialog dialog = new javax.swing.JDialog(
                            this,
                            "🔔 Notificaciones",
                            false);
                    redmineconnector.ui.components.NotificationCenter center = new redmineconnector.ui.components.NotificationCenter(
                            () -> dialog.dispose());
                    dialog.setContentPane(center);
                    dialog.pack();
                    dialog.setLocationRelativeTo(this);

                    dialog.getRootPane().registerKeyboardAction(
                            e -> dialog.dispose(),
                            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);

                    dialog.setVisible(true);
                });
        btnNotifications.setToolTipText("Centro de Notificaciones");
        menuBar.add(btnNotifications);

        return menuBar;
    }

    private void openManual() {
        try {
            // Try to export docs from JAR resources to temp directory
            java.io.File docsDir = exportDocsFromResources();

            if (docsDir != null && docsDir.exists()) {
                java.io.File indexFile = new java.io.File(docsDir, "index.html");
                if (indexFile.exists()) {
                    java.awt.Desktop.getDesktop().open(indexFile);
                    logToGlobal("INFO: Abriendo manual desde: " + indexFile.getAbsolutePath());
                    return;
                }
            }

            // Fallback: try local docs/ folder (for development)
            java.io.File localDocs = new java.io.File("docs/index.html");
            if (localDocs.exists()) {
                java.awt.Desktop.getDesktop().open(localDocs);
                logToGlobal("INFO: Abriendo manual local: docs/index.html");
                return;
            }

            // Fallback: try old manual files
            String lang = redmineconnector.util.I18n.getCurrentLocale().getLanguage();
            String filename = "es".equals(lang) ? "MANUAL_ES.html" : "MANUAL_EN.html";
            java.io.File oldManual = new java.io.File(filename);

            if (oldManual.exists()) {
                java.awt.Desktop.getDesktop().open(oldManual);
                logToGlobal("INFO: Abriendo manual antiguo: " + filename);
                return;
            }

            // No manual found
            throw new java.io.FileNotFoundException("No se encontró documentación disponible");

        } catch (Exception ex) {
            logToGlobal("ERROR: No se pudo abrir el manual: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "No se pudo abrir el archivo de ayuda.\n" +
                            "Asegúrate de que la documentación esté incluida en la distribución.\n\n" +
                            "Error: " + ex.getMessage(),
                    "Error al Abrir Manual",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Exports documentation from JAR resources to temp directory
     * 
     * @return Directory containing exported docs, or null if failed
     */
    private java.io.File exportDocsFromResources() {
        try {
            // Create temp directory for docs
            java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"), "redmine-connector-docs");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // List of files to export
            String[] docFiles = {
                    "docs/index.html",
                    "docs/shared.css",
                    "docs/manual-es/index.html",
                    "docs/manual-es/introduccion.html",
                    "docs/manual-es/instalacion.html",
                    "docs/manual-es/interfaz.html",
                    "docs/manual-es/tareas.html",
                    "docs/manual-es/filtros.html",
                    "docs/manual-es/notificaciones.html",
                    "docs/manual-es/gemelas.html",
                    "docs/manual-es/offline.html",
                    "docs/manual-es/atajos.html",
                    "docs/manual-es/faq.html",
                    "docs/technical/index.html",
                    "docs/config/index.html",
                    "docs/developer/index.html"
            };

            boolean anyExported = false;
            for (String resourcePath : docFiles) {
                try {
                    // Try multiple ways to load the resource
                    java.io.InputStream is = null;

                    // Try 1: ClassLoader
                    is = getClass().getClassLoader().getResourceAsStream(resourcePath);

                    // Try 2: With leading slash
                    if (is == null) {
                        is = getClass().getResourceAsStream("/" + resourcePath);
                    }

                    // Try 3: Without leading slash
                    if (is == null) {
                        is = getClass().getResourceAsStream(resourcePath);
                    }

                    // Try 4: Thread context ClassLoader
                    if (is == null) {
                        is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                    }

                    if (is != null) {
                        // Create subdirectories if needed
                        java.io.File targetFile = new java.io.File(tempDir, resourcePath.substring("docs/".length()));
                        targetFile.getParentFile().mkdirs();

                        // Copy resource to file
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                        fos.close();
                        is.close();
                        anyExported = true;
                        logToGlobal("DEBUG: Exportado: " + resourcePath);
                    } else {
                        logToGlobal("WARN: No se encontró recurso: " + resourcePath);
                    }
                } catch (Exception e) {
                    // Continue with next file
                    logToGlobal("WARN: Error al exportar " + resourcePath + ": " + e.getMessage());
                }
            }

            if (!anyExported) {
                logToGlobal("ERROR: No se pudo exportar ningún archivo de documentación desde el JAR");
                logToGlobal("INFO: Asegúrate de que la carpeta 'docs/' esté incluida en el JAR");
                logToGlobal("INFO: Verifica con: jar tf RedmineConnector.jar | grep docs");
            }

            return anyExported ? tempDir : null;

        } catch (Exception ex) {
            logToGlobal("ERROR: Fallo al exportar documentación: " + ex.getMessage());
            return null;
        }
    }

    private void changeLanguage(String langCode) {
        String current = appProperties.getProperty("app.language", Locale.getDefault().getLanguage());
        if (current.equals(langCode))
            return;

        appProperties.setProperty("app.language", langCode);
        ConfigManager.saveConfig(appProperties);

        // Live reload!
        redmineconnector.util.I18n.loadBundle(new Locale(langCode));

        // Recreate the whole frame
        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            dispose();
            new MainFrame().setVisible(true);
        });

        logToGlobal("INFO: Idioma cambiado a: " + langCode + ". UI Recargada.");
    }

    private void reloadApplicationState() {
        appProperties.clear();
        appProperties.putAll(ConfigManager.loadConfig());

        // Dispose old views if needed?
        // Ideally we should close connections but here we just GC old controllers

        initializeControllers();
        refreshTabs();
        setJMenuBar(createAppMenuBar());
        SwingUtilities.updateComponentTreeUI(this);

        SwingUtilities.invokeLater(() -> {
            for (InstanceController c : controllers) {
                c.refreshData();
            }
        });
    }

    private void openClientManager() {
        ClientManagerDialog d = new ClientManagerDialog(this);
        d.setVisible(true);
        if (d.isChanged()) {
            // Simplest approach: Reload config
            logToGlobal(redmineconnector.util.I18n.get("main.log.clients.changed"));
            reloadApplicationState();
        }
    }

    private JMenu createClientMenu(InstanceController controller, String label) {
        JMenu menu = new JMenu(label);
        JMenuItem mConfig = new JMenuItem(redmineconnector.util.I18n.get("main.menu.config"));
        mConfig.addActionListener(e -> controller.openConfigDialog());

        JMenuItem mColors = new JMenuItem(redmineconnector.util.I18n.get("main.menu.colors"));
        mColors.addActionListener(e -> controller.openColorConfigDialog());

        JMenu mViewOpts = new JMenu(redmineconnector.util.I18n.get("main.menu.view_opts"));
        JCheckBoxMenuItem chkClosed = new JCheckBoxMenuItem(redmineconnector.util.I18n.get("main.menu.show_closed"));
        chkClosed.setSelected(controller.getConfig().showClosed);
        chkClosed.addActionListener(e -> controller.toggleShowClosed(chkClosed.isSelected()));
        JCheckBoxMenuItem chkEpics = new JCheckBoxMenuItem(redmineconnector.util.I18n.get("main.menu.show_epics"));
        chkEpics.setSelected(controller.getConfig().includeEpics);
        chkEpics.addActionListener(e -> controller.toggleIncludeEpics(chkEpics.isSelected()));
        mViewOpts.add(chkClosed);
        mViewOpts.add(chkEpics);

        JMenuItem mCsv = new JMenuItem(redmineconnector.util.I18n.get("main.menu.csv"));
        mCsv.addActionListener(e -> controller.exportToCsv());

        JMenuItem mReports = new JMenuItem(redmineconnector.util.I18n.get("main.menu.reports"));
        mReports.addActionListener(e -> controller.openReportsDialog());

        JMenuItem mVersions = new JMenuItem(redmineconnector.util.I18n.get("main.menu.versions"));
        mVersions.addActionListener(e -> controller.openVersionManager());

        JMenuItem mWiki = new JMenuItem(redmineconnector.util.I18n.get("main.menu.wiki"));
        mWiki.addActionListener(e -> controller.openWikiManager());

        JMenuItem mDashboard = new JMenuItem(redmineconnector.util.I18n.get("main.menu.dashboard"));
        mDashboard.addActionListener(e -> controller.openDashboard());

        menu.add(mConfig);
        menu.add(mColors);
        menu.addSeparator();
        menu.add(mVersions);
        menu.add(mWiki);
        menu.addSeparator();
        menu.add(mDashboard);

        menu.addSeparator();
        menu.add(mViewOpts);
        menu.addSeparator();
        menu.add(mCsv);
        menu.add(mReports);
        return menu;
    }

    public void showProgress(String msg, int percent) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(true);
            progressBar.setString(msg);
            if (percent < 0) {
                progressBar.setIndeterminate(true);
            } else {
                progressBar.setIndeterminate(false);
                progressBar.setValue(percent);
            }
        });
    }

    public void hideProgress() {
        SwingUtilities.invokeLater(() -> progressBar.setVisible(false));
    }

    public void logToGlobal(String msg) {
        logToGlobal("System", msg);
    }

    public void logToGlobal(String source, String msg) {
        Level level = Level.INFO;
        String cleanMsg = msg;

        if (msg.startsWith("DEBUG:")) {
            level = Level.DEBUG;
            cleanMsg = msg.substring(6).trim();
        } else if (msg.startsWith("INFO:")) {
            level = Level.INFO;
            cleanMsg = msg.substring(5).trim();
        } else if (msg.startsWith("WARN:")) {
            level = Level.WARN;
            cleanMsg = msg.substring(5).trim();
        } else if (msg.startsWith("ERROR:")) {
            level = Level.ERROR;
            cleanMsg = msg.substring(6).trim();
        }

        logPanel.addLog(level, cleanMsg, source);
    }

    private void setupGlobalShortcuts() {
        // Ctrl+1 to Ctrl+9 for tabs
        for (int i = 0; i < 9; i++) {
            final int index = i;
            String key = "switchToTab" + (i + 1);
            shortcuts.registerShortcut(key,
                    javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_1 + i,
                            java.awt.event.InputEvent.CTRL_DOWN_MASK),
                    () -> {
                        if (index < tabbedPane.getTabCount()) {
                            tabbedPane.setSelectedIndex(index);
                            logToGlobal("DEBUG: Cambiado a pestaña " + (index + 1));
                        }
                    });
        }

        // Global Search (Command Palette)
        shortcuts.registerShortcut("globalSearch",
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P,
                        java.awt.event.InputEvent.CTRL_DOWN_MASK),
                this::openGlobalSearch);

        // Exit on Escape (User Request)
        shortcuts.registerShortcut("exitApp",
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                () -> {
                    int confirmed = JOptionPane.showConfirmDialog(this,
                            "¿Estás seguro de que deseas salir de la aplicación?",
                            "Confirmar Salida",
                            JOptionPane.YES_NO_OPTION);
                    if (confirmed == JOptionPane.YES_OPTION) {
                        logToGlobal(redmineconnector.util.I18n.get("main.log.exit"));
                        shutdown();
                        System.exit(0);
                    }
                });
    }

    /**
     * Cambia el Look and Feel de la aplicación en tiempo real.
     * 
     * @param lafClassName Nombre completo de la clase del L&F
     * @param lafName      Nombre legible del L&F
     */
    private void changeLookAndFeel(String lafClassName, String lafName) {
        try {
            // Cambiar el L&F
            UIManager.setLookAndFeel(lafClassName);

            // Actualizar TODA la aplicación
            SwingUtilities.updateComponentTreeUI(this);

            // Reconstruir menú para actualizar el tick
            setJMenuBar(createAppMenuBar());

            // Recargar la conexión activa para aplicar los colores
            InstanceController activeController = getActiveController();
            if (activeController != null) {
                activeController.reloadConfig();
            }

            // Persistir cambios
            Properties props = ConfigManager.loadConfig();
            props.setProperty("app.lookandfeel", lafClassName);
            ConfigManager.saveConfig(props);

            logToGlobal("INFO: Look & Feel cambiado a: " + lafName);

        } catch (Exception ex) {
            logToGlobal("ERROR: No se pudo cambiar Look & Feel: " + ex.getMessage());
            redmineconnector.util.LoggerUtil.logError("MainFrame", "Failed to change Look & Feel", new Exception(ex));
        }
    }

    private void setupHelpShortcut() {
        shortcuts.registerShortcut("help",
                KeyboardShortcutManager.CommonShortcuts.HELP,
                () -> {
                    InstanceController activeController = getActiveController();
                    if (activeController != null) {
                        activeController.openHelpDialog();
                    }
                });

        shortcuts.registerShortcut("helpAlt",
                KeyboardShortcutManager.CommonShortcuts.HELP_ALT,
                () -> {
                    InstanceController activeController = getActiveController();
                    if (activeController != null) {
                        activeController.openHelpDialog();
                    }
                });
    }

    private InstanceController getActiveController() {
        Component selected = tabbedPane.getSelectedComponent();
        for (InstanceController ctrl : controllers) {
            if (ctrl.getView() == selected) {
                return ctrl;
            }
        }
        // If no active tab, return first controller
        return controllers.isEmpty() ? null : controllers.get(0);
    }

    private void updateConfigProperty(String key, String value) {
        Properties p = ConfigManager.loadConfig();
        p.setProperty(key, value);
        ConfigManager.saveConfig(p);
    }

    private void openGlobalSearch() {
        GlobalSearchDialog dialog = new GlobalSearchDialog(this, controllers, result -> {
            // Switch tab
            int index = controllers.indexOf(result.controller);
            if (index != -1 && index < tabbedPane.getTabCount()) {
                tabbedPane.setSelectedIndex(index);
                // Focus task in the table
                InstanceView view = result.controller.getView();
                view.selectTaskById(result.task.id);
            }
        });
        dialog.showSearch();
    }

    private void enableTabReordering(JTabbedPane pane) {
        final int[] dragSourceIndex = { -1 };

        pane.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                dragSourceIndex[0] = pane.indexAtLocation(e.getX(), e.getY());
            }
        });

        pane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                int sourceIndex = dragSourceIndex[0];
                int targetIndex = pane.indexAtLocation(e.getX(), e.getY());

                if (sourceIndex != -1 && targetIndex != -1 && sourceIndex != targetIndex) {
                    // Prevent index out of bounds or invalid moves
                    if (targetIndex >= pane.getTabCount() || sourceIndex >= pane.getTabCount())
                        return;

                    // Swap tabs logic
                    Component comp = pane.getComponentAt(sourceIndex);
                    String title = pane.getTitleAt(sourceIndex);
                    String tooltip = pane.getToolTipTextAt(sourceIndex);
                    Component tabComp = pane.getTabComponentAt(sourceIndex); // Save custom header (detach button)

                    // Remove source
                    pane.removeTabAt(sourceIndex);

                    // Insert at target
                    pane.insertTab(title, null, comp, tooltip, targetIndex);

                    // Restore custom header
                    pane.setTabComponentAt(targetIndex, tabComp);

                    // Maintain selection
                    pane.setSelectedIndex(targetIndex);

                    // Update source index to continue dragging
                    dragSourceIndex[0] = targetIndex;
                }
            }
        });
    }

    private void minimizeToTray() throws java.awt.AWTException {
        if (!SystemTray.isSupported()) {
            return;
        }
        setVisible(false);
        logToGlobal("INFO: Aplicación minimizada a la bandeja del sistema.");
    }

    private void restoreFromTray() {
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
    }

    private void exitApp() {
        logToGlobal(redmineconnector.util.I18n.get("main.log.exit"));
        NotificationManager.shutdown(); // Removes tray icon
        shutdown();
        System.exit(0);
    }

    private void exportConfiguration() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle(redmineconnector.util.I18n.get("config.export.title", "Exportar Configuración"));
        fc.setSelectedFile(new java.io.File("redmine_config.properties"));
        if (fc.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {

            // Ask for sanitized export
            Object[] options = { "Completo (Con Claves)", "Sanitizado (Sin Claves)", "Cancelar" };
            int choice = javax.swing.JOptionPane.showOptionDialog(this,
                    "¿Cómo deseas exportar la configuración?\n\nCompleto: Incluye claves cifradas (para backup seguro).\nSanitizado: Elimina claves y datos de usuario (para compartir).",
                    "Tipo de Exportación",
                    javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 2 || choice == javax.swing.JOptionPane.CLOSED_OPTION)
                return;

            try {
                if (choice == 1) { // Sanitizado
                    ConfigManager.exportSanitizedConfig(fc.getSelectedFile());
                } else { // Completo
                    ConfigManager.exportConfig(fc.getSelectedFile());
                }

                javax.swing.JOptionPane.showMessageDialog(this,
                        redmineconnector.util.I18n.get("config.export.success", "Configuración exportada con éxito."),
                        redmineconnector.util.I18n.get("config.export.title", "Exportar Configuración"),
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                logToGlobal("ERROR: Fallo al exportar configuración: " + e.getMessage());
                javax.swing.JOptionPane.showMessageDialog(this,
                        redmineconnector.util.I18n.format("config.export.error", e.getMessage()),
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importConfiguration() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle(redmineconnector.util.I18n.get("config.import.title", "Importar Configuración"));
        fc.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter("Properties files (*.properties)", "properties"));
        if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
                    redmineconnector.util.I18n.get("config.import.confirm",
                            "Esto SOBRESCRIBIRÁ tu configuración actual y recargará la aplicación.\nSe creará un backup automático (.bak) antes de proceder.\n¿Estás seguro?"),
                    redmineconnector.util.I18n.get("config.import.title", "Importar Configuración"),
                    javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE);

            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                try {
                    // Backup automático
                    ConfigManager.backupConfig();
                    logToGlobal("INFO: Backup de configuración creado.");

                    ConfigManager.importConfig(fc.getSelectedFile());
                    // Recarga en caliente
                    reloadApplicationState();

                    javax.swing.JOptionPane.showMessageDialog(this,
                            redmineconnector.util.I18n.get("config.import.success",
                                    "Configuración importada con éxito."),
                            redmineconnector.util.I18n.get("config.import.title", "Importar Configuración"),
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    logToGlobal("ERROR: Fallo al importar configuración: " + e.getMessage());
                    javax.swing.JOptionPane.showMessageDialog(this,
                            redmineconnector.util.I18n.format("config.import.error", e.getMessage()),
                            "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void factoryReset() {
        int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que deseas RESTABLECER todos los valores de fábrica?\n\nEsta acción:\n1. Eliminará tu configuración actual.\n2. Borrará todas las conexiones y preferencias.\n3. NO afectará a tus tareas guardadas en el historial de caché.\n\nEsta acción NO se puede deshacer.",
                "Restablecer de Fábrica",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.ERROR_MESSAGE);

        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            try {
                ConfigManager.resetConfig();
                reloadApplicationState();

                javax.swing.JOptionPane.showMessageDialog(this,
                        "La aplicación se ha restablecido a los valores por defecto.",
                        "Restablecer Completado",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                logToGlobal("ERROR: Fallo al restablecer configuración: " + e.getMessage());
            }
        }
    }

    private java.awt.Image createDefaultIcon() {
        int size = 16;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2.setColor(new Color(180, 50, 50)); // Redmine Red-ish
        g2.fillRoundRect(0, 0, size, size, 4, 4);

        // Text
        g2.setColor(Color.WHITE);
        g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
        g2.drawString("R", 4, 12);

        g2.dispose();
        return image;
    }

    public void shutdown() {
        for (InstanceController c : controllers) {
            c.shutdown();
        }
    }
}
