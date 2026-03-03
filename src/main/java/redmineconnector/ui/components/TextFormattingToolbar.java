package redmineconnector.ui.components;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import redmineconnector.util.I18n;

public class TextFormattingToolbar extends JPanel {

    private final JTextComponent target;

    public TextFormattingToolbar(JTextComponent target) {
        this.target = target;
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));

        addButton("B", "Negrita (*texto*)", e -> wrapSelection("*", "*"));
        addButton("I", "Cursiva (_texto_)", e -> wrapSelection("_", "_"));
        addButton("U", "Subrayado (+texto+)", e -> wrapSelection("+", "+"));
        addButton("S", "Tachado (-texto-)", e -> wrapSelection("-", "-"));
        addButton("Code", "Código (@texto@)", e -> wrapSelection("@", "@"));
        addButton("Pre", "Bloque Pre (<pre>...</pre>)", e -> wrapSelection("<pre>\n", "\n</pre>"));
        addButton("Link", "Insertar Enlace", e -> insertLink());
    }

    private void addButton(String label, String tooltip, ActionListener action) {
        JButton btn = new JButton(label);
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(1, 6, 1, 6));
        btn.setFocusable(false);
        btn.addActionListener(action);
        add(btn);
    }

    private void wrapSelection(String pre, String post) {
        String sel = target.getSelectedText();
        if (sel == null)
            sel = "";
        target.replaceSelection(pre + sel + post);
        target.requestFocusInWindow();
    }

    private void insertLink() {
        String url = JOptionPane.showInputDialog(this, "URL del enlace:", "http://");
        if (url != null && !url.trim().isEmpty()) {
            String sel = target.getSelectedText();
            if (sel != null && !sel.isEmpty()) {
                // Redmine format: "Text":url
                target.replaceSelection("\"" + sel + "\":" + url);
            } else {
                target.replaceSelection(" " + url + " ");
            }
        }
        target.requestFocusInWindow();
    }
}
