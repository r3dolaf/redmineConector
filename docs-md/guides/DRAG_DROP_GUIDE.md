# 🎯 Guía de Implementación: Drag & Drop y Clipboard

## Componentes Creados

Se han creado componentes reutilizables para drag & drop y clipboard paste:

### 1. `DragDropFilePanel` - Panel para Adjuntos

**Ubicación**: `src/main/java/redmineconnector/ui/components/DragDropFilePanel.java`

**Características**:
- ✅ Zona visual de drop con feedback
- ✅ Soporte para arrastrar y soltar múltiples archivos
- ✅ **Pegar imágenes desde portapapeles (Ctrl+V)**
- ✅ Click para abrir file chooser tradicional
- ✅ Lista de archivos seleccionados con tamaño
- ✅ Botones para eliminar archivos individuales o todos
- ✅ Callback cuando cambia la lista de archivos
- ✅ Animaciones y feedback visual

**Uso básico**:
```java
// Crear panel con callback
DragDropFilePanel filePanel = new DragDropFilePanel(files -> {
    System.out.println("Archivos seleccionados: " + files.size());
    // Procesar archivos...
});

// Añadir a diálogo
dialog.add(filePanel);

// Obtener archivos cuando sea necesario
List<File> files = filePanel.getSelectedFiles();

// Para pegar imagen: enfocar el panel y presionar Ctrl+V
```

### 2. `DragDropTextArea` - TextArea para Notas

**Ubicación**: `src/main/java/redmineconnector/ui/components/DragDropTextArea.java`

**Características**:
- ✅ Arrastrar archivos de texto (.txt, .md, .log, .json, etc.)
- ✅ Insertar contenido en posición del cursor
- ✅ Soporte para múltiples archivos con separadores
- ✅ Feedback visual durante drag
- ✅ Validación de tipos de archivo
- ✅ Manejo de errores robusto

**Uso básico**:
```java
// Crear text area mejorado
DragDropTextArea notesArea = new DragDropTextArea(10, 50);
notesArea.setToolTipText("Escribe notas o arrastra archivos de texto aquí...");

// Añadir a diálogo
dialog.add(new JScrollPane(notesArea));

// Obtener texto
String notes = notesArea.getText();
```

### 3. `DragDropImageTextPane` - TextPane con Imágenes Inline

**Ubicación**: `src/main/java/redmineconnector/ui/components/DragDropImageTextPane.java`

**Características**:
- ✅ **Pegar imágenes desde portapapeles (Ctrl+V)**
- ✅ **Vista previa inline de imágenes mientras escribes**
- ✅ Arrastrar archivos de texto
- ✅ Generar markup de Redmine automáticamente (`!imagen.png!`)
- ✅ Escalar imágenes grandes automáticamente
- ✅ Tracking de imágenes para subir como adjuntos

**Uso básico**:
```java
// Crear text pane con soporte de imágenes
DragDropImageTextPane commentPane = new DragDropImageTextPane(5, 40);
commentPane.setToolTipText("Pega imágenes con Ctrl+V o arrastra archivos de texto");

// Añadir a diálogo
dialog.add(new JScrollPane(commentPane));

// Al guardar, obtener texto con referencias Redmine
String textWithMarkup = commentPane.getTextWithImageReferences();
// Resultado: "Mira esta captura: !screenshot_20251222_203456.png!\nEs un bug."

// Obtener imágenes para subir
List<File> pastedImages = commentPane.getPastedImages();
for (File img : pastedImages) {
    uploadHandler.upload(img); // Subir como adjunto
}

// Limpiar después de guardar
commentPane.clearPastedImages();
```

### 4. `ClipboardImageHandler` - Utilidad de Portapapeles

**Ubicación**: `src/main/java/redmineconnector/ui/components/ClipboardImageHandler.java`

**Métodos estáticos**:
```java
// Verificar si hay imagen en portapapeles
boolean hasImage = ClipboardImageHandler.hasImageInClipboard();

// Obtener imagen del portapapeles
BufferedImage image = ClipboardImageHandler.getImageFromClipboard();

// Guardar imagen como PNG temporal
File imageFile = ClipboardImageHandler.saveImageToTempFile(image, "screenshot");

// Todo en uno
File imageFile = ClipboardImageHandler.getAndSaveClipboardImage("pasted");
```

---

## 📋 Funcionalidad de Portapapeles

### Pegar Imágenes en Comentarios

1. **Tomar captura de pantalla** (Win+Shift+S en Windows)
2. **Hacer clic** en el área de comentarios
3. **Presionar Ctrl+V**
4. **La imagen aparece inline** en el texto
5. **Continuar escribiendo** antes/después de la imagen
6. **Al guardar**, la imagen se sube automáticamente y el texto incluye `!nombre_imagen.png!`

### Pegar Imágenes en Adjuntos

1. **Copiar imagen** al portapapeles
2. **Enfocar** el panel de adjuntos (hacer clic en él)
3. **Presionar Ctrl+V**
4. **La imagen se añade** a la lista con nombre auto-generado

### Formato de Nombres

Las imágenes pegadas se guardan con nombres descriptivos:
- Desde comentarios: `screenshot_YYYYMMDD_HHMMSS.png`
- Desde adjuntos: `pasted_YYYYMMDD_HHMMSS.png`

### Markup de Redmine

El componente `DragDropImageTextPane` genera automáticamente el markup de Redmine:

**Texto en el editor**:
```
Encontré un error en la página de login.
[IMAGEN INLINE VISIBLE AQUÍ]
Por favor revisa el botón de "Enviar".
```

**Texto generado para Redmine**:
```
Encontré un error en la página de login.
!screenshot_20251222_203456.png!
Por favor revisa el botón de "Enviar".
```

**Resultado en Redmine**: La imagen se muestra inline en el comentario.

---

## 📝 Integración en Diálogos Existentes

### Opción 1: TaskFormDialog (Crear/Editar Tarea)

**Archivo**: `src/main/java/redmineconnector/ui/dialogs/TaskFormDialog.java`

**Cambios sugeridos**:

```java
// ANTES: JButton para seleccionar archivos
JButton attachButton = new JButton("Seleccionar archivos...");

// DESPUÉS: Panel de drag & drop
DragDropFilePanel attachmentPanel = new DragDropFilePanel(files -> {
    // Actualizar lista de adjuntos pendientes
    this.pendingAttachments = files;
});

// Añadir al formulario
formPanel.add(new JLabel("Adjuntos:"));
formPanel.add(attachmentPanel);
```

**Cambios en el área de descripción**:

```java
// ANTES: JTextArea normal
JTextArea descriptionArea = new JTextArea(10, 50);

// DESPUÉS: TextArea con drag & drop
DragDropTextArea descriptionArea = new DragDropTextArea(10, 50);
descriptionArea.setToolTipText("Puedes arrastrar archivos de texto aquí");
```

### Opción 2: Diálogo de Añadir Nota

**Crear nuevo diálogo o modificar existente**:

```java
public class AddNoteDialog extends JDialog {
    
    private DragDropTextArea notesArea;
    private DragDropFilePanel attachmentPanel;
    
    public AddNoteDialog(JFrame parent, Task task) {
        super(parent, "Añadir Nota - #" + task.id, true);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Área de notas con drag & drop
        JPanel notesPanel = new JPanel(new BorderLayout(5, 5));
        notesPanel.add(new JLabel("Notas:"), BorderLayout.NORTH);
        
        notesArea = new DragDropTextArea(8, 60);
        notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);
        
        // Panel de adjuntos con drag & drop
        attachmentPanel = new DragDropFilePanel(files -> {
            System.out.println("Adjuntos: " + files.size());
        });
        
        // Layout
        mainPanel.add(notesPanel, BorderLayout.CENTER);
        mainPanel.add(attachmentPanel, BorderLayout.SOUTH);
        
        // Botones
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton saveButton = new JButton("Guardar");
        saveButton.addActionListener(e -> saveNote());
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        
        panel.add(saveButton);
        panel.add(cancelButton);
        
        return panel;
    }
    
    private void saveNote() {
        String notes = notesArea.getText();
        List<File> attachments = attachmentPanel.getSelectedFiles();
        
        // Subir nota y adjuntos...
        // dataService.updateTask(task);
        // for (File file : attachments) {
        //     dataService.uploadFile(file);
        // }
        
        dispose();
    }
}
```

---

## 🎨 Personalización Visual

### Cambiar Colores

```java
// En DragDropFilePanel.java, modificar constantes:
private static final Color DROP_ZONE_NORMAL = new Color(245, 245, 245);
private static final Color DROP_ZONE_HOVER = new Color(200, 230, 255);
private static final Color DROP_ZONE_BORDER = new Color(70, 130, 180);
```

### Cambiar Tamaños

```java
// Tamaño de la zona de drop
dropZone.setPreferredSize(new Dimension(500, 120));

// Tamaño de la lista de archivos
scrollPane.setPreferredSize(new Dimension(500, 200));
```

### Cambiar Textos

```java
// Modificar labels en createDropZone()
dropZoneLabel.setText(
    "<html><center>" +
    "<b>🎯 Tu mensaje personalizado</b>" +
    "</center></html>"
);
```

---

## 🧪 Ejemplo de Prueba

Crear un diálogo de prueba para verificar funcionalidad:

```java
public class DragDropTestDialog extends JDialog {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            DragDropTestDialog dialog = new DragDropTestDialog(frame);
            dialog.setVisible(true);
        });
    }
    
    public DragDropTestDialog(JFrame parent) {
        super(parent, "Prueba Drag & Drop", true);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Panel de archivos
        DragDropFilePanel filePanel = new DragDropFilePanel(files -> {
            System.out.println("✓ Archivos seleccionados: " + files.size());
            files.forEach(f -> System.out.println("  - " + f.getName()));
        });
        
        // Text area
        DragDropTextArea textArea = new DragDropTextArea(10, 50);
        
        // Layout
        mainPanel.add(filePanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        // Botón de prueba
        JButton testButton = new JButton("Mostrar archivos seleccionados");
        testButton.addActionListener(e -> {
            List<File> files = filePanel.getSelectedFiles();
            String text = textArea.getText();
            
            JOptionPane.showMessageDialog(this,
                "Archivos: " + files.size() + "\n" +
                "Texto: " + text.length() + " caracteres",
                "Resultado",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
        
        mainPanel.add(testButton, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
        setSize(600, 500);
        setLocationRelativeTo(parent);
    }
}
```

---

## ✅ Checklist de Integración

- [ ] Compilar nuevos componentes
- [ ] Probar `DragDropFilePanel` standalone
- [ ] Probar `DragDropTextArea` standalone
- [ ] Integrar en `TaskFormDialog`
- [ ] Integrar en diálogo de notas
- [ ] Probar con archivos grandes (>10MB)
- [ ] Probar con múltiples archivos simultáneos
- [ ] Verificar feedback visual
- [ ] Probar en Windows/Linux/Mac
- [ ] Actualizar manual de usuario

---

## 🐛 Troubleshooting

### Problema: No se detecta el drag

**Solución**: Verificar que el componente tiene `setEnabled(true)` y es visible.

### Problema: Archivos no se añaden

**Solución**: Verificar permisos de lectura del archivo y que el callback está configurado.

### Problema: UI se congela con archivos grandes

**Solución**: Procesar archivos en background thread:

```java
SwingWorker<Void, Void> worker = new SwingWorker<>() {
    protected Void doInBackground() {
        // Procesar archivos...
        return null;
    }
    protected void done() {
        // Actualizar UI...
    }
};
worker.execute();
```

---

## 📚 Referencias

- [Java DnD Tutorial](https://docs.oracle.com/javase/tutorial/uiswing/dnd/)
- [DropTarget API](https://docs.oracle.com/javase/8/docs/api/java/awt/dnd/DropTarget.html)
- [DataFlavor API](https://docs.oracle.com/javase/8/docs/api/java/awt/datatransfer/DataFlavor.html)

---

**© 2025 Redmine Connector Pro - Drag & Drop Implementation Guide**
