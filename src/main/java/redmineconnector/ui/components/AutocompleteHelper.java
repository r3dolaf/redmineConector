package redmineconnector.ui.components;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper to add autocomplete functionality to JTextComponents.
 * Triggers on specific characters (e.g., '@', '#').
 */
public class AutocompleteHelper {

    public static class CompletionItem {
        public String label;
        public String replacement;

        public CompletionItem(String label, String replacement) {
            this.label = label;
            this.replacement = replacement;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public interface CompletionProvider {
        List<CompletionItem> getCompletions(String query);
    }

    private final JTextComponent textComponent;
    private final char triggerChar;
    private final CompletionProvider provider;
    private final JPopupMenu popup;
    private final JList<CompletionItem> list;
    private final DefaultListModel<CompletionItem> listModel;

    public static void attach(JTextComponent textComponent, char triggerChar, CompletionProvider provider) {
        new AutocompleteHelper(textComponent, triggerChar, provider);
    }

    private AutocompleteHelper(JTextComponent textComponent, char triggerChar, CompletionProvider provider) {
        this.textComponent = textComponent;
        this.triggerChar = triggerChar;
        this.provider = provider;

        this.listModel = new DefaultListModel<>();
        this.list = new JList<>(listModel);
        this.popup = new JPopupMenu();

        initialize();
    }

    private void initialize() {
        list.setFocusable(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(textComponent.getFont());

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        popup.add(scroll);
        popup.setFocusable(false);
        popup.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Handle list selection
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertSelection();
                }
            }
        });

        textComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (popup.isVisible()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        int index = list.getSelectedIndex();
                        if (index < listModel.size() - 1)
                            list.setSelectedIndex(index + 1);
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        int index = list.getSelectedIndex();
                        if (index > 0)
                            list.setSelectedIndex(index - 1);
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                        insertSelection();
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        popup.setVisible(false);
                        e.consume();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP
                        || e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB
                        || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return;
                }

                checkTrigger();
            }
        });
    }

    private void checkTrigger() {
        try {
            int pos = textComponent.getCaretPosition();
            if (pos == 0)
                return;

            String text = textComponent.getText(0, pos);
            int lastTrigger = text.lastIndexOf(triggerChar);

            if (lastTrigger != -1) {
                // Check if trigger is "valid" (preceded by space or is start of line)
                if (lastTrigger > 0 && !Character.isWhitespace(text.charAt(lastTrigger - 1))) {
                    popup.setVisible(false);
                    return;
                }

                String query = text.substring(lastTrigger + 1);
                // Prevent multi-word queries or too long queries from keeping popup open
                // unnecessarily
                if (query.contains(" ") || query.contains("\n") || query.length() > 20) {
                    popup.setVisible(false);
                    return;
                }

                List<CompletionItem> items = provider.getCompletions(query);
                if (!items.isEmpty()) {
                    listModel.clear();
                    items.forEach(listModel::addElement);
                    list.setSelectedIndex(0);

                    Rectangle caretRect = textComponent.modelToView(pos);
                    popup.setPreferredSize(new Dimension(300, 200)); // Limit size
                    popup.show(textComponent, caretRect.x, caretRect.y + caretRect.height);
                    textComponent.requestFocusInWindow();
                } else {
                    popup.setVisible(false);
                }
            } else {
                popup.setVisible(false);
            }
        } catch (Exception ex) {
            // Ignore (e.g. BadLocation)
            popup.setVisible(false);
        }
    }

    private void insertSelection() {
        CompletionItem item = list.getSelectedValue();
        if (item == null)
            return;

        try {
            int pos = textComponent.getCaretPosition();
            String text = textComponent.getText(0, pos);
            int lastTrigger = text.lastIndexOf(triggerChar);

            if (lastTrigger != -1) {
                textComponent.select(lastTrigger, pos);
                textComponent.replaceSelection(item.replacement + " ");
                popup.setVisible(false);
            }
        } catch (BadLocationException e) {
            redmineconnector.util.LoggerUtil.logError("AutocompleteHelper", "Error inserting selection", e);
        }
    }
}
