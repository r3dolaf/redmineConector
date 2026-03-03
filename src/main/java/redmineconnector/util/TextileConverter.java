package redmineconnector.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple Textile/Markdown to HTML converter for preview purposes.
 * Note: This is not a full parser, just a lightweight converter for common
 * syntax.
 */
public class TextileConverter {

    public static String convertToHtml(String text) {
        if (text == null)
            return "";

        // Escape HTML special chars first
        String html = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        // PROTECT PRE BLOCKS
        // We replace <pre>...</pre> with placeholders to prevent inside content from
        // being formatted
        // We use alphanumeric placeholders to avoid triggering other regexes (like
        // underscores for italics)
        java.util.Map<String, String> preBlocks = new java.util.HashMap<>();
        int preCounter = 0;

        // Regex for <pre>...</pre> (escaped as &lt;pre&gt;)
        Pattern pPre = Pattern.compile("(?s)(&lt;pre&gt;(.*?)&lt;/pre&gt;)");
        Matcher mPre = pPre.matcher(html);
        while (mPre.find()) {
            String placeholder = "PHPREBLOCK" + (preCounter++) + "ENDPH";
            // Important: Use monospace font and explicit background
            preBlocks.put(placeholder,
                    "<pre style='font-family: Consolas, monospace; background-color: #f0f0f0; padding: 5px; border: 1px solid #ccc; font-style: normal;'>"
                            + mPre.group(2) + "</pre>");
            html = html.replace(mPre.group(1), placeholder);
        }

        // Regex for markdown ``` ... ```
        Pattern pCode = Pattern.compile("(?s)(```(.*?)```)");
        Matcher mCode = pCode.matcher(html);
        while (mCode.find()) {
            String placeholder = "PHPREBLOCK" + (preCounter++) + "ENDPH";
            preBlocks.put(placeholder,
                    "<pre style='font-family: Consolas, monospace; background-color: #f0f0f0; padding: 5px; border: 1px solid #ccc; font-style: normal;'>"
                            + mCode.group(2) + "</pre>");
            html = html.replace(mCode.group(1), placeholder);
        }

        // --- Block Elements ---

        // Headers h1., h2., h3.
        html = html.replaceAll("(?m)^h1\\.\\s+(.*)$", "<h1>$1</h1>");
        html = html.replaceAll("(?m)^h2\\.\\s+(.*)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^h3\\.\\s+(.*)$", "<h3>$1</h3>");

        // --- Inline Elements ---

        // Bold *text* or **text**
        html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("\\*(.*?)\\*", "<strong>$1</strong>");

        // Italic _text_ (Ensure we don't break simple underscores, enforce boundary?)
        // Simple regex _text_
        html = html.replaceAll("_(.*?)_", "<em>$1</em>");

        // Underline +text+
        html = html.replaceAll("\\+(.*?)\\+", "<u>$1</u>");

        // Strikethrough -text-
        html = html.replaceAll("-(.*?)-", "<strike>$1</strike>");

        // Inline Code @text@
        html = html.replaceAll("@(.*?)@",
                "<code style='font-family: Consolas, monospace; background-color: #f5f5f5;'>$1</code>");

        // Links [[Page Name]] or "Link Text":URL
        html = html.replaceAll("\\[\\[(.*?)\\]\\]", "<a href='#'>$1</a>");
        html = html.replaceAll("\"(.*?)\":(\\S+)", "<a href='$2'>$1</a>");

        // --- Lists ---

        // Unordered lists * Item
        html = html.replaceAll("(?m)^\\*\\s+(.*)$", "<li>$1</li>");

        // Ordered lists # Item
        html = html.replaceAll("(?m)^#\\s+(.*)$", "<li>$1</li>");

        // Line breaks to <br>
        html = html.replaceAll("\n", "<br>");

        // RESTORE PRE BLOCKS
        for (java.util.Map.Entry<String, String> entry : preBlocks.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }

        // Wraps in body
        return "<html><body style='font-family:sans-serif; font-size:12px; margin:10px;'>" + html + "</body></html>";
    }

    /**
     * Converts Textile with image resolution support.
     * 
     * @param text       The raw textile string
     * @param imagePaths A map of filename -> local absolute path (or URL)
     * @return HTML string with img tags
     */
    public static String convertToHtml(String text, java.util.Map<String, String> imagePaths) {
        if (text == null)
            return "";

        // 1. Escape HTML special chars
        String html = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        // 2. PROTECT PRE & CODE BLOCKS (Store in map)
        java.util.Map<String, String> preBlocks = new java.util.HashMap<>();
        int preCounter = 0;

        // <pre>
        Pattern pPre = Pattern.compile("(?s)(&lt;pre&gt;(.*?)&lt;/pre&gt;)");
        Matcher mPre = pPre.matcher(html);
        while (mPre.find()) {
            String placeholder = "PHPREBLOCK" + (preCounter++) + "ENDPH";
            preBlocks.put(placeholder,
                    "<pre style='font-family: Consolas, monospace; background-color: #f0f0f0; padding: 5px; border: 1px solid #ccc; font-style: normal;'>"
                            + mPre.group(2) + "</pre>");
            html = html.replace(mPre.group(1), placeholder);
        }

        // ``` code ```
        Pattern pCode = Pattern.compile("(?s)(```(.*?)```)");
        Matcher mCode = pCode.matcher(html);
        while (mCode.find()) {
            String placeholder = "PHPREBLOCK" + (preCounter++) + "ENDPH";
            preBlocks.put(placeholder,
                    "<pre style='font-family: Consolas, monospace; background-color: #f0f0f0; padding: 5px; border: 1px solid #ccc; font-style: normal;'>"
                            + mCode.group(2) + "</pre>");
            html = html.replace(mCode.group(1), placeholder);
        }

        // 3. PROTECT IMAGES (Store in map)
        // Check for images BEFORE inline formatting (like strikethrough) breaks them.
        java.util.Map<String, String> imageBlocks = new java.util.HashMap<>();
        int imgCounter = 0;

        // Use regex consistent with extractImageNames: !([^!\s]+)!
        Pattern pImg = Pattern.compile("!([^!\\s]+)!(\\s?)");
        Matcher mImg = pImg.matcher(html);
        StringBuffer sbImg = new StringBuffer();

        while (mImg.find()) {
            String filename = mImg.group(1);
            String trailingSpace = mImg.group(2);
            String replacement;

            // Check resolved path
            if (imagePaths != null && imagePaths.containsKey(filename)) {
                String localPath = imagePaths.get(filename);
                if (localPath.startsWith("ERROR:")) {
                    replacement = "<span style='color:red; font-weight:bold' title='" + localPath + "'>[Image Error: "
                            + filename + "]</span>";
                } else if (localPath.startsWith("DEBUG:")) {
                    replacement = "<span style='color:gray' title='" + localPath + "'>[Image: " + filename + "]</span>";
                } else {
                    String urlPrefix = localPath.startsWith("http") ? "" : "file:///";
                    replacement = "<img src='" + urlPrefix + localPath + "' style='max-width:100%;'>";
                }
            } else {
                replacement = "<span style='color:gray'>[Image: " + filename + "]</span>";
            }

            // Append trailing space if captured
            if (trailingSpace != null)
                replacement += trailingSpace;

            String placeholder = "PHIMAGEBLOCK" + (imgCounter++) + "ENDPH";
            imageBlocks.put(placeholder, replacement);
            mImg.appendReplacement(sbImg, placeholder);
        }
        mImg.appendTail(sbImg);
        html = sbImg.toString();

        // 4. APPLY INLINE FORMATTING

        // Headers
        html = html.replaceAll("(?m)^h1\\.\\s+(.*)$", "<h1>$1</h1>");
        html = html.replaceAll("(?m)^h2\\.\\s+(.*)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^h3\\.\\s+(.*)$", "<h3>$1</h3>");

        // Bold
        html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("\\*(.*?)\\*", "<strong>$1</strong>");

        // Italic
        html = html.replaceAll("_(.*?)_", "<em>$1</em>");

        // Underline
        html = html.replaceAll("\\+(.*?)\\+", "<u>$1</u>");

        // Strikethrough (The cause of the issue!)
        html = html.replaceAll("-(.*?)-", "<strike>$1</strike>");

        // Inline Code
        html = html.replaceAll("@(.*?)@",
                "<code style='font-family: Consolas, monospace; background-color: #f5f5f5;'>$1</code>");

        // Links
        html = html.replaceAll("\\[\\[(.*?)\\]\\]", "<a href='#'>$1</a>");
        html = html.replaceAll("\"(.*?)\":(\\S+)", "<a href='$2'>$1</a>");

        // Lists
        html = html.replaceAll("(?m)^\\*\\s+(.*)$", "<li>$1</li>");
        html = html.replaceAll("(?m)^#\\s+(.*)$", "<li>$1</li>");

        // Line breaks
        html = html.replaceAll("\n", "<br>");

        // 5. RESTORE IMAGES
        for (java.util.Map.Entry<String, String> entry : imageBlocks.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }

        // 6. RESTORE PRE BLOCKS
        for (java.util.Map.Entry<String, String> entry : preBlocks.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }

        // 7. Wrap
        return "<html><body style='font-family:sans-serif; font-size:12px; margin:10px;'>" + html + "</body></html>";
    }

    /**
     * Extracts all image filenames from the textile text.
     */
    public static java.util.Set<String> extractImageNames(String text) {
        java.util.Set<String> images = new java.util.HashSet<>();
        if (text == null)
            return images;

        Pattern pImg = Pattern.compile("!([^!\\s]+)!(\\s?)");
        Matcher mImg = pImg.matcher(text);
        while (mImg.find()) {
            images.add(mImg.group(1));
        }
        return images;
    }

    private static String convertToHtmlBodyOnly(String text) {
        if (text == null)
            return "";

        // Escape HTML special chars first
        String html = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        // PROTECT PRE BLOCKS
        java.util.Map<String, String> preBlocks = new java.util.HashMap<>();
        int preCounter = 0;

        Pattern pPre = Pattern.compile("(?s)(&lt;pre&gt;(.*?)&lt;/pre&gt;)");
        Matcher mPre = pPre.matcher(html);
        while (mPre.find()) {
            String placeholder = "PHPREBLOCK" + (preCounter++) + "ENDPH";
            preBlocks.put(placeholder,
                    "<pre style='font-family: Consolas, monospace; background-color: #f0f0f0; padding: 5px; border: 1px solid #ccc; font-style: normal;'>"
                            + mPre.group(2) + "</pre>");
            html = html.replace(mPre.group(1), placeholder);
        }

        Pattern pCode = Pattern.compile("(?s)(```(.*?)```)");
        Matcher mCode = pCode.matcher(html);
        while (mCode.find()) {
            String placeholder = "PHPREBLOCK" + (preCounter++) + "ENDPH";
            preBlocks.put(placeholder,
                    "<pre style='font-family: Consolas, monospace; background-color: #f0f0f0; padding: 5px; border: 1px solid #ccc; font-style: normal;'>"
                            + mCode.group(2) + "</pre>");
            html = html.replace(mCode.group(1), placeholder);
        }

        // Headers
        html = html.replaceAll("(?m)^h1\\.\\s+(.*)$", "<h1>$1</h1>");
        html = html.replaceAll("(?m)^h2\\.\\s+(.*)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^h3\\.\\s+(.*)$", "<h3>$1</h3>");

        // Inline
        html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("\\*(.*?)\\*", "<strong>$1</strong>");
        html = html.replaceAll("_(.*?)_", "<em>$1</em>");
        html = html.replaceAll("\\+(.*?)\\+", "<u>$1</u>");
        html = html.replaceAll("-(.*?)-", "<strike>$1</strike>");
        html = html.replaceAll("@(.*?)@",
                "<code style='font-family: Consolas, monospace; background-color: #f5f5f5;'>$1</code>");
        html = html.replaceAll("\\[\\[(.*?)\\]\\]", "<a href='#'>$1</a>");
        html = html.replaceAll("\"(.*?)\":(\\S+)", "<a href='$2'>$1</a>");

        // Lists
        html = html.replaceAll("(?m)^\\*\\s+(.*)$", "<li>$1</li>");
        html = html.replaceAll("(?m)^#\\s+(.*)$", "<li>$1</li>");

        // Line breaks
        html = html.replaceAll("\n", "<br>");

        // Restore PRE
        for (java.util.Map.Entry<String, String> entry : preBlocks.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }

        return html;
    }

    public static String convertSimpleHtml(String text) {
        if (text == null)
            return "";
        // Escape HTML
        String html = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        // Line breaks
        html = html.replace("\n", "<br>");
        return "<html><body style='font-family:Segoe UI; font-size:12px; margin:10px;'>" + html + "</body></html>";
    }
}
