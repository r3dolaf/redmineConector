# 🐑 Redmine Connector Pro

Cliente profesional de escritorio para Redmine, construido en Java puro con una arquitectura limpia. Diseñado para usuarios intensivos que necesitan gestionar múltiples instancias de Redmine, trabajar offline, registrar tiempos rápidamente y sincronizar tareas ("gemelas") entre servidores.

![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)
![License](https://img.shields.io/badge/License-Proprietary-red.svg)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-lightgrey.svg)

---

## ✨ Características Principales

- **🔄 Multi-Instancia y Cierre Sincronizado:** Conéctate a varios servidores de Redmine en pestañas separadas. Detecta "tareas gemelas" y ciérralas en ambos servidores con un solo clic.
- **⚡ Velocidad y Modo Offline:** Sistema de caché local inteligente. Si la conexión a Redmine falla o es lenta, la aplicación entra automáticamente en "Modo Offline" permitiendo lectura de datos.
- **📊 Time Tracking & Kanban:** Incluye un Dashboard interactivo para reportes de horas, exportación a Excel (CSV) y un tablón visual Kanban generado a partir de los estados.
- **📝 Clonación Inteligente:** Al clonar una tarea de un servidor a otro, todo el historial de comentarios (journals) se anexa de forma limpia a la descripción.
- **🖼️ Quick View Panel:** Lee descripciones y previsualiza imágenes adjuntas sin necesidad de abrir ventanas emergentes.

## 🛠️ Tecnologías y Arquitectura

Este proyecto sigue una filosofía de **Cero Dependencias Externas**. Todo está construido usando la biblioteca estándar de Java.

- **Lenguaje:** Java 8+ (Compatible hasta Java 21)
- **Framework UI:** Java Swing (Nativo) con Look And Feel optimizado.
- **Arquitectura:** MVC (Model-View-Controller) asíncrono.
- **Comunicación HTTP:** Implementación nativa sobre la API REST v1 de Redmine.
- **Tratamiento JSON:** Parses ligeros nativos o adaptados sin cargar pesadas librerías de terceros (ej. sin Gson o Jackson).

## 🚀 Cómo Compilar y Ejecutar desde el Código Fuente

El proyecto no utiliza complejos gestores de dependencias como Maven o Gradle para mantener la simplicidad y portabilidad absoluta. En su lugar, utiliza scripts de PowerShell para automatizar la compilación y empaquetado.

### Requisitos Previos
- **Java JDK 8 o superior** instalado y configurado en tu variable de entorno `PATH`.
- (Opcional) Si usas Windows, puedes ejecutar `.\setup-java.ps1` para validarlo.

### Opción 1: Construcción Automática (Windows)
En la raíz del proyecto, ejecuta el script de construcción integrado:

```powershell
.\build-complete.ps1
```
Este script limpiará, compilará todo el código bajo `src/main/java/`, empaquetará los recursos (`docs/`, imágenes internas) y generará directamente un `.jar` y un `.zip` distribuible en una carpeta terminada en `-Portable`.

### Opción 2: Compilación Manual
Si estás en Linux/macOS o prefieres compilar manualmente:

```bash
# 1. Crear carpeta de binarios
mkdir -p bin

# 2. Compilar especificando el encoding
javac -source 8 -target 8 -d bin -sourcepath src/main/java -encoding UTF-8 src/main/java/redmineconnector/RedmineConnectorApp.java

# 3. Copiar recursos visuales y de idioma
cp -r src/main/java/redmineconnector/resources bin/redmineconnector/

# 4. Crear el ejecutable JAR
jar cfe RedmineConnector.jar redmineconnector.RedmineConnectorApp -C bin/ .
```

## ⚙️ Configuración

La primera vez que ejecutes el programa (`java -jar RedmineConnector.jar`), se generará un archivo `config.properties` en tu directorio de usuario (`%APPDATA%/RedmineConnector/` en Windows u oculto en Linux/Mac).

También puedes configurar las claves de las APIs usando la interfaz gráfica de la aplicación entrando a **Archivo → Gestor de Clientes**.

## 📖 Documentación

- **Manual de Usuario:** Para detalles completos del uso de las funcionalidades (atajos de teclado, reportes, filtros), consulta el archivo [USER_GUIDE.md](README.md) adjunto en el proyecto.
- **Documentación Técnica:** Toda la arquitectura, diseño de clases y evolución del desarrollo se encuentra en la carpeta `/docs-md/`.
