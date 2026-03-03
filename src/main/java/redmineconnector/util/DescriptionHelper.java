package redmineconnector.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import redmineconnector.model.Attachment;

/**
 * Clase de utilidad para corregir problemas de formato en la descripción al
 * clonar tareas.
 */
public class DescriptionHelper {

    /**
     * Corrige las referencias a imágenes en la descripción de una tarea clonada.
     * Reemplaza IDs antiguos, URLs absolutas y sintaxis rota con referencias
     * simples por nombre de archivo.
     * 
     * @param description La descripción original
     * @param attachments La lista de adjuntos que se están migrando
     * @return La descripción corregida
     */
    public static String fixClonedDescription(String description, List<Attachment> attachments) {
        return fixClonedDescription(description, attachments, "textile");
    }

    public static String fixClonedDescription(String description, List<Attachment> attachments, String targetFormat) {
        if (description == null || description.isEmpty() || attachments == null || attachments.isEmpty()) {
            return description;
        }

        String fixed = description;
        boolean toMarkdown = "markdown".equalsIgnoreCase(targetFormat);

        // 1. Corrige patrón extraño de pantallazos: ![]((filename)) -> !filename!
        fixed = fixed.replaceAll("!\\[\\]\\(\\(([^)]+)\\)\\)", "!$1!");

        // 1b. Handle standard Markdown image syntax with empty alt: ![](filename) -> !filename!
        fixed = fixed.replaceAll("!\\[\\]\\(([^)]+)\\)", "!$1!");

        for (Attachment att : attachments) {
            String filename = att.filename;
            if (filename == null || filename.isEmpty())
                continue;

            String safeName = Pattern.quote(filename);
            
            // 2. Normalizar a formato canónico !filename! primero
            if (att.id > 0) {
                fixed = fixed.replaceAll("!#" + att.id + "!", "!" + filename + "!");
            }

            // Reemplazar formatos Markdown rotos o mixtos a Textile standard !filename!
            fixed = fixed.replaceAll("!\\[.*?\\]\\(.*?\\/" + safeName + "\\)", "!" + filename + "!");
            fixed = fixed.replaceAll("!\\[\\]\\(\\(.*?" + safeName + ".*?\\)\\)", "!" + filename + "!");
            fixed = fixed.replaceAll("!\\[.*?\\]\\(" + safeName + "\\)", "!" + filename + "!"); // Std markdown
                                                                                                // [alt](file)

            // 3. Convertir al formato destino
            if (toMarkdown) {
                // Textile !filename! -> Markdown ![](filename)
                // Usamos un placeholder para evitar re-emplazos infinitos si ya lo convertimos
                fixed = fixed.replaceAll("!" + safeName + "!", "![](%MARKER%" + filename + ")");
            }
        }

        if (toMarkdown) {
            fixed = fixed.replace("%MARKER%", "");
        }

        // 4. Limpieza general
        Matcher m = Pattern.compile("!\\[\\]\\(\\((clipboard-[^)]+)\\)\\)").matcher(fixed);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String val = m.group(1);
            String replacement = toMarkdown ? "![](" + val + ")" : "!" + val + "!";
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        fixed = sb.toString();

        return fixed;
    }
}
