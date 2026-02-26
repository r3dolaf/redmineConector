# 📖 Manual de Usuario - Redmine Connector Pro (v2.5)

## 🆕 Actualizaciones Recientes

### Nuevas Funcionalidades (Diciembre 2025)
- **Estado "Nuevo" por Defecto**: Las tareas nuevas y clonadas obtienen automáticamente el estado "Nuevo"
- **Cierre Bidireccional de Gemelas**: Al cerrar tareas gemelas, el sistema sincroniza de vuelta a la instancia original
- **Filtrado de Estados por Tracker**: Los diálogos de multi-cierre y cierre de gemelas muestran solo estados válidos según el tipo de tarea
- **Sistema de Ayuda Mejorado**: Presiona F1 para acceder a ayuda con 3 pestañas (Atajos, Funciones, Consejos)
- **Ordenamiento de Notas Mejorado**: Las notas en Vista Rápida pueden ordenarse de más recientes a más antiguas o viceversa

---

## Índice
1. [Introducción](#introducción)
2. [Instalación y Configuración Inicial](#instalación-y-configuración-inicial)
3. [Interfaz Principal](#interfaz-principal)
4. [Gestión de Tareas](#gestión-de-tareas)
5. [Filtros y Búsqueda](#filtros-y-búsqueda)
6. [Sistema de Notificaciones](#sistema-de-notificaciones)
7. [Sincronización de Tareas Gemelas](#sincronización-de-tareas-gemelas)
8. [Registro de Tiempo](#registro-de-tiempo)
9. [Informes y Métricas](#informes-y-métricas)
10. [Modo Offline](#modo-offline)
11. [Personalización](#personalización)
12. [Atajos de Teclado](#atajos-de-teclado)
13. [Preguntas Frecuentes](#preguntas-frecuentes)

---

## 1. Introducción

**Redmine Connector Pro** es una aplicación de escritorio que proporciona una interfaz mejorada para gestionar proyectos de Redmine. Ofrece funcionalidades avanzadas como:

- ✅ Gestión multi-instancia (múltiples servidores Redmine simultáneamente)
- ✅ Modo offline con caché inteligente
- ✅ Notificaciones de escritorio en tiempo real
- ✅ Sincronización de tareas entre servidores
- ✅ Filtros avanzados y búsqueda global
- ✅ Informes y métricas visuales
- ✅ Temas personalizables
- ✅ Atajos de teclado para flujo de trabajo rápido

---

## 2. Instalación y Configuración Inicial

### 2.1 Requisitos del Sistema

- **Java**: JRE 8 o superior
- **RAM**: Mínimo 512 MB (recomendado 1 GB)
- **Disco**: 100 MB libres
- **Red**: Acceso al servidor Redmine

### 2.2 Instalación

1. Descargue el archivo `RedmineConnector.jar`
2. Colóquelo en una carpeta de su elección
3. Ejecute con doble clic o desde terminal: `java -jar RedmineConnector.jar`

### 2.3 Configuración Inicial

Al iniciar por primera vez:

1. Vaya a **Archivo → Gestor de Clientes**
2. Haga clic en **Agregar Cliente**
3. Complete los datos:
   - **Nombre**: Nombre descriptivo (ej: "Servidor Producción")
   - **URL**: Dirección de su Redmine (ej: `https://redmine.empresa.com/`)
   - **Clave API**: Obténgala en Redmine → Mi cuenta → Clave de acceso API
   - **ID Proyecto**: Identificador del proyecto (visible en la URL del navegador)
4. Haga clic en **Guardar**
5. La aplicación creará una nueva pestaña para este cliente

---

## 3. Interfaz Principal

### 3.1 Barra de Menú

**Archivo**:
- **Gestor de Clientes**: Administrar conexiones a servidores Redmine
- **Donar**: Apoyar el desarrollo del proyecto
- **Ver Manual**: Abrir esta documentación
- **Atajos de Teclado**: Ver lista de atajos
- **Idioma**: Cambiar entre Español e Inglés
- **Salir**: Cerrar la aplicación

**Vista**:
- **Ver Log**: Expandir/contraer panel de registro
- **Tema**: Cambiar tema visual (Claro, Personalizado)

**Menús de Cliente** (dinámicos):
- Uno por cada servidor configurado
- Acceso a funciones específicas del cliente

**Botón de Notificaciones** (🔔):
- Ubicado en la esquina derecha de la barra de menú
- Muestra badge con número de notificaciones no leídas
- Click para abrir Centro de Notificaciones

### 3.2 Pestañas de Cliente

Cada cliente Redmine tiene su propia pestaña con:
- Lista de tareas
- Panel de filtros
- Vista rápida
- Indicador de modo offline (si aplica)

### 3.3 Panel de Filtros

Ubicado en la parte superior de cada pestaña:

**Búsqueda Básica**:
- **ID**: Buscar por número de tarea
- **Asunto**: Búsqueda en tiempo real por palabras clave
- **Excluir**: Palabras a excluir (separadas por comas)

**Filtros de Fecha**:
- **Desde/Hasta**: Rango de fechas para filtrar tareas

**Filtros Multi-selección**:
- **Estado**: Filtrar por estado (Nuevo, En Progreso, Cerrado, etc.)
- **Tracker**: Filtrar por tipo (Error, Funcionalidad, Soporte, etc.)
- **Asignado**: Filtrar por persona asignada
- **Versión**: Filtrar por versión/hito
- **Categoría**: Filtrar por categoría

**Filtros Rápidos**:
- **Mis Tareas**: Solo tareas asignadas al usuario actual
- **Vencidas**: Tareas con fecha límite pasada
- **Hoy**: Tareas con vencimiento hoy

**Botones de Acción**:
- **Crear**: Nueva tarea
- **Refrescar**: Actualizar datos del servidor
- **Palabras Clave**: Análisis de palabras frecuentes
- **Limpiar Filtros**: Resetear todos los filtros

### 3.4 Lista de Tareas

Tabla con columnas:
- **ID**: Número de tarea
- **Asunto**: Título de la tarea
- **Estado**: Estado actual
- **Prioridad**: Nivel de prioridad (con icono)
- **Tipo**: Tracker/tipo de tarea
- **Cat.**: Categoría
- **Versión**: Versión asignada
- **Asignado**: Persona responsable
- **Horas**: Horas estimadas
- **%**: Porcentaje completado
- **Fecha**: Fecha de creación o actualización

**Funcionalidades**:
- **Ordenar**: Click en encabezado de columna (Shift+Click para multi-columna)
- **Mostrar/Ocultar Columnas**: Click derecho en encabezado
- **Tareas Fijadas**: Aparecen primero con estrella dorada (⭐)
- **Efecto Hover**: Resaltado visual al pasar el mouse

### 3.5 Vista Rápida

Panel inferior que muestra detalles de la tarea seleccionada:

**Pestañas**:
- **Descripción**: Texto completo de la descripción
- **Notas**: Últimos 3 comentarios y cambios
  - **Ordenamiento**: Combo box para elegir orden (Más recientes primero / Más antiguas primero)
  - Por defecto muestra las notas más recientes primero
- **Adjuntos**: Lista de archivos (con vista previa de imágenes)
- **Tiempo**: Formulario rápido para registrar horas

**Botón de Expansión** (▲/▼):
- Expandir: Vista completa del panel
- Contraer: Vista mínima (solo encabezado)
- **Atajo Q**: Presiona Q para expandir/contraer rápidamente

### 3.6 Panel de Log

Ubicado en la parte inferior de la ventana:

**Funcionalidades**:
- Registro de todas las operaciones
- Filtros por nivel (Debug, Info, Warn, Error)
- Filtro por origen (cliente específico o sistema)
- Búsqueda de texto
- Auto-scroll opcional
- Botón para expandir/contraer

---

## 4. Gestión de Tareas

### 4.1 Crear Nueva Tarea

**Método 1**: Botón "Crear" en panel de filtros
**Método 2**: Atajo `Ctrl+N`
**Método 3**: Menú Cliente → Crear Tarea

**Campos del Formulario**:
- **Asunto** (obligatorio): Título de la tarea
- **Descripción**: Detalles completos (soporta Markdown/Textile)
- **Tracker**: Tipo de tarea
- **Estado**: Estado inicial
- **Prioridad**: Nivel de importancia
- **Asignado**: Persona responsable
- **Versión**: Hito/versión objetivo
- **Categoría**: Clasificación
- **Fecha Inicio**: Fecha de inicio planificada
- **Fecha Fin**: Fecha límite
- **Horas Estimadas**: Estimación de esfuerzo
- **% Completado**: Porcentaje de avance

### 4.2 Editar Tarea

**Método 1**: Doble click en la tarea
**Método 2**: Seleccionar tarea y presionar `Enter`
**Método 3**: Atajo `Ctrl+E`
**Método 4**: Click derecho → Editar

**Formulario de Edición**:
- Mismo formulario que crear, con valores actuales
- Pestaña **Notas** con historial completo de cambios
  - **Ordenamiento**: Combo box para elegir orden de visualización
  - **Más recientes primero**: Muestra las notas más nuevas arriba (predeterminado)
  - **Más antiguas primero**: Muestra las notas más antiguas arriba (orden cronológico)
  - Útil para seguir la evolución de la tarea desde el inicio

### 4.3 Eliminar Tarea

**Método**: Click derecho → Eliminar

**Confirmación**:
- Diálogo de confirmación antes de eliminar
- Operación irreversible

### 4.4 Operaciones con Menú Contextual

Click derecho en una tarea para:

**Operaciones Básicas**:
- **Editar**: Abrir formulario de edición
- **Eliminar**: Borrar tarea
- **Copiar ID**: Copiar número de tarea al portapapeles
- **Copiar Asunto + Descripción**: Copiar formato para email/chat
- **Abrir en Navegador**: Ver tarea en interfaz web de Redmine

**Operaciones Avanzadas**:
- **Crear Subtarea**: Nueva tarea hija vinculada automáticamente
- **Clonar a Otro Servidor**: Copiar tarea a servidor gemelo
- **Emparejar con Gemela**: Buscar/vincular tarea equivalente
- **Descargar a Escritorio**: Guardar tarea completa en carpeta local
- **Pin/Quitar Pin**: Marcar como favorita

**Operaciones Múltiples** (selección múltiple):
- **Edición Masiva**: Cambiar campos en varias tareas
- **Cierre Masivo**: Cerrar múltiples tareas con selección de estado
  - **Nuevo**: El diálogo ahora incluye selector de estado
  - **Filtrado Inteligente**: Solo muestra estados válidos según el tipo de tarea (tracker)
  - **Basado en Flujo de Trabajo**: Respeta las transiciones configuradas en Redmine
- **Cierre Sincronizado**: Cerrar tareas y sus gemelas
  - **Bidireccional**: Cierra gemelas y luego propaga de vuelta al origen
  - **Estados por Tracker**: Cada instancia muestra solo estados compatibles

### 4.5 Fijar Tareas (Pin)

**Propósito**: Mantener tareas importantes siempre visibles al inicio de la lista

**Cómo Fijar**:
1. Click derecho en tarea
2. Seleccionar "⭐ Pin (Favorito)"

**Cómo Quitar Pin**:
1. Click derecho en tarea fijada
2. Seleccionar "⭐ Quitar Pin"

**Indicador Visual**:
- Estrella dorada (⭐) en la columna de prioridad
- Tareas fijadas aparecen primero en la lista

---

## 5. Filtros y Búsqueda

### 5.1 Búsqueda Básica

**Por ID**:
- Escribir número de tarea en campo "ID"
- Búsqueda exacta

**Por Asunto**:
- Escribir palabras clave en campo "Asunto"
- Búsqueda en tiempo real (mientras escribe)
- No distingue mayúsculas/minúsculas

**Exclusión**:
- Escribir palabras a excluir en campo "Excluir"
- Separar múltiples palabras con comas
- Ejemplo: `test, reunión` ocultará tareas con esas palabras

### 5.2 Filtros por Fecha

**Rango de Fechas**:
1. Click en campo "Desde" → seleccionar fecha en calendario
2. Click en campo "Hasta" → seleccionar fecha en calendario
3. Solo se mostrarán tareas creadas/actualizadas en ese rango

**Limpiar Fechas**:
- Botón "✕" junto a cada campo de fecha

### 5.3 Filtros Multi-selección

**Cómo Usar**:
1. Click en dropdown (Estado, Tracker, Asignado, etc.)
2. Marcar/desmarcar opciones deseadas
3. Click fuera del dropdown para aplicar

**Opciones Especiales en "Asignado"**:
- **⭐ A mí mismo**: Filtra tareas asignadas al usuario actual
- **⚪ Sin Asignar**: Filtra tareas sin responsable

### 5.4 Sincronización Inteligente de Filtros

**Tracker → Estado**:
- Al seleccionar un Tracker, el filtro de Estado se actualiza automáticamente
- Solo muestra estados aplicables a ese tipo de tarea

### 5.5 Búsqueda Global (Command Palette)

**Atajo**: `Ctrl+P`

**Funcionalidad**:
- Buscar tareas en **todos los clientes** simultáneamente
- Buscar por ID o palabras clave
- Navegación con flechas ↑↓
- Enter para abrir tarea (cambia automáticamente a pestaña correcta)

**Ejemplo**:
1. Presionar `Ctrl+P`
2. Escribir "login bug"
3. Ver resultados de todos los servidores
4. Seleccionar con flechas
5. Enter para abrir

### 5.6 Análisis de Palabras Clave

**Acceso**: Botón "Palabras Clave" en panel de filtros

**Funcionalidad**:
- Analiza asuntos de todas las tareas visibles
- Muestra palabras más frecuentes
- Útil para identificar temas recurrentes

---

## 6. Sistema de Notificaciones

### 6.1 Tipos de Notificaciones

- **Nueva Tarea Creada**: Cuando creas una tarea
- **Nueva Tarea Asignada**: Cuando te asignan una tarea
- **Cambio de Estado**: Cuando cambia el estado de una tarea
- **Nuevo Comentario**: Cuando alguien comenta en tu tarea
- **Fecha Límite**: Recordatorio de vencimiento próximo
- **Tarea Vencida**: Alerta de tarea con fecha pasada

### 6.2 Notificaciones de Escritorio

**Requisitos**:
- Sistema operativo con soporte de bandeja del sistema
- Permisos de notificaciones habilitados

**Comportamiento**:
- Aparecen en esquina de pantalla (según SO)
- Duración: 5-10 segundos
- Click en notificación para abrir tarea

### 6.3 Centro de Notificaciones

**Acceso**: Click en botón 🔔 en barra de menú

**Funcionalidades**:
- Lista de todas las notificaciones recientes
- Badge con número de no leídas
- Marcar como leída/no leída
- Limpiar todas
- Click en notificación para navegar a tarea

**Persistencia**:
- Historial guardado entre sesiones
- Máximo 100 notificaciones

### 6.4 Configuración de Notificaciones

**Prevención de Duplicados**:
- El sistema recuerda qué tareas ya han sido notificadas
- No se repiten notificaciones para la misma tarea
- Tracking persistente entre sesiones

**Notificaciones Automáticas**:
- Se activan al refrescar datos
- Detecta tareas nuevas asignadas en última hora
- Distingue entre tareas creadas por ti vs asignadas por otros

---

## 7. Sincronización de Tareas Gemelas

### 7.1 Concepto de Tareas Gemelas

**Definición**: Tareas relacionadas en diferentes servidores Redmine que representan el mismo trabajo.

**Caso de Uso**: Empresa con servidor interno y servidor de cliente externo.

**Ejemplo**:
- Servidor A (Interno): Tarea #1234 "Implementar login"
- Servidor B (Cliente): Tarea #5678 "Implementar login [Ref #1234]"

### 7.2 Configurar Patrón de Referencia

**Ubicación**: Configuración de Cliente → Patrón de Referencia

**Formato**: `[Ref #{id}]`

**Variables**:
- `{id}`: ID de la tarea original

**Ejemplo**:
- Patrón: `[Ref #{id}]`
- Resultado: `[Ref #1234]` en el asunto de la tarea gemela

### 7.3 Clonar Tarea a Otro Servidor

**Pasos**:
1. Click derecho en tarea
2. Seleccionar "Clonar a Otro Servidor"
3. Elegir servidor destino
4. Revisar datos pre-llenados
5. Confirmar

**Comportamiento**:
- Copia asunto, descripción, prioridad
- Añade referencia automática en descripción
- Vincula tareas como gemelas

### 7.4 Emparejar con Tarea Gemela

**Detección Automática**:
- El sistema busca referencias en asuntos
- Usa el patrón configurado
- Marca tareas como emparejadas

**Emparejamiento Manual**:
1. Click derecho en tarea
2. "Emparejar con Gemela"
3. Buscar tarea en servidor peer
4. Confirmar emparejamiento

### 7.5 Cierre Sincronizado

**Propósito**: Cerrar tarea y su gemela simultáneamente con sincronización bidireccional

**Pasos**:
1. Click derecho en tarea emparejada
2. "Cierre Sincronizado"
3. Sistema detecta gemela automáticamente
4. Muestra diálogo con ambas tareas
5. Seleccionar estado final para cada una (filtrados por tracker)
6. Confirmar

**Sincronización Bidireccional** (Nueva Funcionalidad):
- Al cerrar gemelas en Instancia B, el sistema automáticamente:
  1. Cierra las tareas gemelas en B
  2. Detecta las tareas originales en Instancia A
  3. Muestra diálogo en A para cerrar las originales
  4. Completa la sincronización en ambas direcciones

**Ejemplo de Flujo Bidireccional**:
```
Instancia A (Origen) → Multi-cierre de 3 tareas
  ↓
Instancia B (Cliente) → Recibe diálogo para cerrar 3 gemelas
  ↓ Usuario acepta
Instancia B → Cierra las 3 gemelas
  ↓
Instancia A ← Recibe diálogo para cerrar las 3 originales
  ↓ Usuario acepta
Instancia A → Completa el ciclo cerrando originales
```

**Cierre Masivo Sincronizado**:
- Seleccionar múltiples tareas
- Click derecho → "Multi-cierre Sincronizado"
- Sistema detecta cuáles tienen gemelas
- **Estados Filtrados**: Cada diálogo muestra solo estados válidos según el tracker de las tareas
- Cierra todas simultáneamente con sincronización bidireccional

---

## 8. Registro de Tiempo

### 8.1 Registro Rápido desde Vista Rápida

**Ubicación**: Pestaña "Tiempo" en Vista Rápida

**Campos**:
- **Usuario**: Quién registra (por defecto: usuario actual)
- **Fecha**: Día del trabajo (por defecto: hoy)
- **Horas**: Cantidad de horas (formato decimal: 1.5 = 1h 30m)
- **Actividad**: Tipo de actividad (Desarrollo, Diseño, Testing, etc.)
- **Comentario**: Descripción del trabajo realizado

**Registrar**:
1. Completar campos
2. Click en "⏰ Registrar Tiempo"
3. Confirmación visual

### 8.2 Registro desde Formulario de Tarea

**Ubicación**: Pestaña "Tiempo" en diálogo de edición

**Funcionalidad Adicional**:
- Ver historial de tiempo registrado
- Editar/eliminar registros existentes

### 8.3 Ver Tiempo Registrado

**Acceso**: Menú Cliente → Informes → Horas

**Visualización**:
- Tabla con todos los registros
- Filtros por fecha, usuario, actividad
- Totales por usuario y actividad
- Exportar a CSV

---

## 9. Informes y Métricas

### 9.1 Dashboard de Métricas

**Acceso**: Menú Cliente → Dashboard de Métricas

**Visualizaciones**:

**Gráfico de Estados**:
- Barras horizontales con distribución de tareas por estado
- Colores configurables
- Tooltips con números exactos

**Gráfico de Prioridades**:
- Distribución por nivel de prioridad
- Identificación rápida de urgencias

**Estadísticas Generales**:
- Total de tareas
- Tareas abiertas vs cerradas
- Promedio de horas por tarea
- Tareas vencidas

**Botón "Copiar Resumen"**:
- Copia estadísticas en formato texto
- Listo para pegar en email/chat

### 9.2 Informe de Horas

**Acceso**: Menú Cliente → Informes → Horas

**Filtros**:
- Rango de fechas
- Usuario específico
- Actividad específica

**Tabla de Resultados**:
- Columnas: Fecha, Usuario, Tarea, Horas, Actividad, Comentario
- Totales por usuario
- Totales por actividad
- Total general

**Exportar**:
- Botón "Copiar Tabla"
- Formato compatible con Excel
- Mantiene estructura de columnas

### 9.3 Informe de Tareas por Categoría

**Acceso**: Menú Cliente → Informes → Categorías

**Visualización**:
- Tabla con conteo de tareas por categoría
- Porcentaje del total
- Gráfico de barras

### 9.4 Informe de Versiones

**Acceso**: Menú Cliente → Gestor de Versiones

**Información por Versión**:
- Nombre y descripción
- Fecha de inicio y fin
- Porcentaje de progreso
- Tareas abiertas/cerradas
- Estado (Abierta/Cerrada)

**Email de Despliegue**:
- Botón "Generar Email de Despliegue"
- Crea email con changelog
- Lista de tareas cerradas
- Resumen de cambios

---

## 10. Modo Offline

### 10.1 ¿Qué es el Modo Offline?

**Definición**: Modo de operación cuando no hay conexión al servidor Redmine.

**Filosofía**: Offline-First
- La aplicación siempre intenta funcionar
- Usa caché local cuando no hay conexión
- Datos disponibles incluso sin internet

### 10.2 Activación Automática

**Trigger**: Fallo de conexión durante refresh

**Indicadores Visuales**:
- Etiqueta roja "MODO OFFLINE (SÓLO LECTURA)" en encabezado
- Botones de escritura deshabilitados
- Mensaje en log

### 10.3 Funcionalidades Disponibles

**Permitido**:
- ✅ Ver lista de tareas (última versión cacheada)
- ✅ Ver detalles de tareas
- ✅ Buscar y filtrar
- ✅ Ordenar columnas
- ✅ Ver adjuntos (si están en caché)
- ✅ Leer notas y comentarios

**No Permitido**:
- ❌ Crear tareas
- ❌ Editar tareas
- ❌ Eliminar tareas
- ❌ Registrar tiempo
- ❌ Subir adjuntos
- ❌ Actualizar wiki

### 10.4 Recuperación de Conexión

**Método 1**: Automático
- La aplicación reintenta conexión periódicamente
- Al recuperar conexión, sale de modo offline automáticamente

**Método 2**: Manual
- Click en botón "Reintentar Conexión"
- Fuerza refresh inmediato

**Sincronización**:
- Al recuperar conexión, se actualiza caché con datos frescos
- No hay conflictos (modo offline es solo lectura)

---

## 11. Personalización

### 11.1 Temas Visuales

**Acceso**: Menú Vista → Tema

**Opciones**:
- **Claro**: Tema por defecto con colores claros
- **Personalizado**: Editor de tema custom

### 11.2 Editor de Tema Personalizado

**Acceso**: Vista → Tema → Personalizado

**Elementos Configurables**:
- **Color de Fondo**: Color principal de paneles
- **Color de Encabezado**: Color de barras de título
- **Color de Botones**: Color de botones de acción
- **Color de Texto**: Color de texto principal

**Vista Previa**:
- Cambios se aplican en tiempo real
- Botón "Restablecer" para volver a valores por defecto

**Guardar**:
- Tema se guarda automáticamente
- Persiste entre sesiones

### 11.3 Colores de Estado

**Acceso**: Configuración de Cliente → Colores de Estado

**Funcionalidad**:
- Asignar color específico a cada estado
- Ejemplo: "En Progreso" = Azul, "Nuevo" = Naranja
- Colores se aplican en tabla de tareas

**Configuración**:
1. Seleccionar estado
2. Elegir color en selector
3. Guardar

### 11.4 Configuración de Columnas

**Mostrar/Ocultar**:
1. Click derecho en encabezado de tabla
2. Marcar/desmarcar columnas
3. Cambios se guardan automáticamente

**Reordenar**:
- Arrastrar encabezados de columna
- Orden se guarda automáticamente

**Anchos**:
- Redimensionar arrastrando borde de columna
- Anchos se guardan automáticamente

### 11.5 Idioma

**Acceso**: Archivo → Idioma

**Opciones**:
- Español
- English

**Aplicación**:
- Cambio requiere reiniciar aplicación
- Preferencia se guarda en configuración

---

## 12. Atajos de Teclado

### 12.1 Navegación Global

| Atajo | Acción |
|-------|--------|
| `Ctrl+P` | Búsqueda Global (Command Palette) |
| `Ctrl+1` a `Ctrl+9` | Cambiar a pestaña de cliente 1-9 |
| `F1` | Ayuda de Atajos de Teclado |
| `Esc` | Cerrar Diálogo/Popup Actual |

### 12.2 Operaciones de Tareas

| Atajo | Acción |
|-------|--------|
| `Ctrl+N` | Crear Nueva Tarea |
| `Enter` | Abrir Tarea Seleccionada |
| `Ctrl+E` | Editar Tarea Seleccionada |
| `Ctrl+Shift+C` | Copiar ID de Tarea |
| `Delete` | Eliminar Tarea Seleccionada |

### 12.3 Navegación en Lista

| Atajo | Acción |
|-------|--------|
| `↑` / `↓` | Navegar Arriba/Abajo |
| `J` | Siguiente Tarea |
| `K` | Tarea Anterior |
| `Home` | Primera Tarea |
| `End` | Última Tarea |

### 12.4 Actualización y Filtros

| Atajo | Acción |
|-------|--------|
| `F5` / `Ctrl+R` | Refrescar Datos |
| `Ctrl+F` | Enfocar Campo de Búsqueda |
| `Ctrl+L` | Limpiar Filtros |

### 12.5 Vista Rápida

| Atajo | Acción |
|-------|--------|
| `Q` | Ciclar tamaño de Vista Rápida (Min/30%/50%) |
| `Ctrl+Q` | Expandir/Contraer Vista Rápida (alternativo) |
| `W` / `E` | Pestaña Anterior / Siguiente en Vista Rápida |
| `Ctrl+1` a `Ctrl+4` | Cambiar Pestaña en Vista Rápida |

---

## 13. Preguntas Frecuentes

### 13.1 Conexión y Autenticación

**P: ¿Dónde obtengo la clave API?**
R: En Redmine web → Mi cuenta → Clave de acceso API (columna derecha)

**P: ¿Puedo usar usuario y contraseña en lugar de API key?**
R: Sí, pero API key es más seguro y recomendado

**P: ¿Cómo sé si mi conexión funciona?**
R: Al guardar configuración, la app intenta conectar. Verás mensaje de éxito/error en el log.

### 13.2 Tareas y Datos

**P: ¿Por qué no veo todas mis tareas?**
R: Revisa:
- Filtros activos (botón "Limpiar Filtros")
- Límite de tareas en configuración
- Permisos en Redmine

**P: ¿Cómo actualizo los datos?**
R: Botón "Refrescar" o `F5`

**P: ¿Los cambios se guardan automáticamente?**
R: No, debes hacer click en "Guardar" en cada diálogo

### 13.3 Modo Offline

**P: ¿Puedo trabajar sin internet?**
R: Sí, en modo solo lectura con datos cacheados

**P: ¿Cuánto tiempo duran los datos en caché?**
R: Por defecto 5 minutos para tareas, 30 minutos para metadatos

**P: ¿Cómo salgo del modo offline?**
R: Automático al recuperar conexión, o botón "Reintentar Conexión"

### 13.4 Notificaciones

**P: ¿Por qué no recibo notificaciones?**
R: Verifica:
- Permisos de notificaciones del sistema operativo
- Sistema de bandeja soportado
- Notificaciones no deshabilitadas en configuración

**P: ¿Puedo desactivar notificaciones?**
R: Actualmente no hay opción global, pero puedes cerrar el Centro de Notificaciones

### 13.5 Rendimiento

**P: La aplicación va lenta, ¿qué hago?**
R: Intenta:
- Reducir límite de tareas
- Aumentar intervalo de refresh
- Limpiar caché (reiniciar aplicación)
- Cerrar clientes no usados

**P: ¿Cuántos clientes puedo tener?**
R: No hay límite técnico, pero recomendamos máximo 5 para buen rendimiento

### 13.6 Sincronización de Gemelas

**P: ¿Cómo funciona la detección automática de gemelas?**
R: Busca el patrón de referencia configurado en los asuntos

**P: ¿Puedo tener más de 2 servidores sincronizados?**
R: Sí, pero la sincronización es por pares (A↔B, B↔C, etc.)

---

**© 2025 Redmine Connector Pro - Manual de Usuario**

*Para soporte técnico o reportar problemas, contacte al desarrollador.*
