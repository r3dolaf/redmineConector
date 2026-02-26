package redmineconnector.ui.components;

import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.text.JTextComponent;

/**
 * Helper class to create formatting toolbars for text editors.
 * Supports basic Textile formatting (Bold, Italic, Headers, Code).
 */
public class EditorToolbarHelper {

    /**
     * Creates a toolbar attached to the given text component.
     * 
     * @param textComponent The component to manipulate when buttons are clicked.
     * @return A JToolBar populated with formatting buttons.
     */
    public static JToolBar createToolbar(JTextComponent textComponent) {
        JToolBar formatBar = new JToolBar();
        formatBar.setFloatable(false);

        addFormatButton(formatBar, textComponent, "B", "**", "**");
        addFormatButton(formatBar, textComponent, "I", "_", "_");
        addFormatButton(formatBar, textComponent, "H1", "h1. ", "");
        addFormatButton(formatBar, textComponent, "H2", "h2. ", "");
        addFormatButton(formatBar, textComponent, "Code", "<pre>", "</pre>");
        addFormatButton(formatBar, textComponent, "Link", "[[", "]]");

        return formatBar;
    }

    private static void addFormatButton(JToolBar bar, JTextComponent textComponent, String label, String prefix,
            String suffix) {
        JButton btn = new JButton(label);
        btn.setFocusable(false);
        // btn.setMargin(new java.awt.Insets(2, 5, 2, 5)); // Optional: Compact buttons
        btn.addActionListener(e -> {
            String selected = textComponent.getSelectedText();
            if (selected == null)
                selected = "";
            textComponent.replaceSelection(prefix + selected + suffix);
            textComponent.requestFocusInWindow();
        });
        bar.add(btn);
    }
}
