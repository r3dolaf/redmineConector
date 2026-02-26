package redmineconnector.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import redmineconnector.model.*;
import redmineconnector.util.I18n;

/**
 * Envoltorio asíncrono para DataService que proporciona operaciones basadas en
 * CompletableFuture.
 * Todas las operaciones se ejecutan en un pool de hilos dedicado para evitar
 * bloquear el
 * hilo de la UI.
 * 
 * <p>
 * Ejemplo de uso:
 * 
 * <pre>
 * AsyncDataService asyncService = new AsyncDataService(dataService);
 * asyncService.fetchTasksAsync("project1", false, 100)
 *         .thenAccept(tasks -> SwingUtilities.invokeLater(() -> updateUI(tasks)))
 *         .exceptionally(ex -> {
 *             SwingUtilities.invokeLater(() -> showError(ex));
 *             return null;
 *         });
 * </pre>
 * 
 * @author Redmine Connector Team
 * @version 2.0
 */
public class AsyncDataService {
    private final DataService delegate;
    private final ExecutorService executorService;

    /**
     * Crea un AsyncDataService con el pool de hilos por defecto (4 hilos).
     * 
     * @param delegate el DataService subyacente a envolver
     */
    public AsyncDataService(DataService delegate) {
        this.delegate = delegate;
        this.executorService = new ThreadPoolExecutor(
                redmineconnector.util.AppConstants.ASYNC_CORE_POOL_SIZE,
                redmineconnector.util.AppConstants.ASYNC_MAX_POOL_SIZE,
                redmineconnector.util.AppConstants.THREAD_KEEP_ALIVE_SEC, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Crea un AsyncDataService con un ExecutorService personalizado.
     * 
     * @param delegate        el DataService subyacente a envolver
     * @param executorService ejecutor personalizado para operaciones asíncronas
     */
    public AsyncDataService(DataService delegate, ExecutorService executorService) {
        this.delegate = delegate;
        this.executorService = executorService;
    }

    /**
     * Crea el ejecutor por defecto con hilos daemon nombrados.
     */
    private static ExecutorService createDefaultExecutor() {
        return Executors.newFixedThreadPool(4, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AsyncDataService-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }

    /**
     * Obtiene tareas de un proyecto de forma asíncrona.
     * 
     * @param pid    identificador del proyecto
     * @param closed si se incluyen tareas cerradas
     * @param limit  número máximo de tareas a obtener (0 para todas)
     * @return CompletableFuture con la lista de tareas
     */
    public CompletableFuture<List<Task>> fetchTasksAsync(String pid, boolean closed, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchTasks(pid, closed, limit);
            } catch (Exception e) {
                throw new RuntimeException(I18n.get("async.error.fetch_tasks"), e);
            }
        }, executorService);
    }

    /**
     * Obtiene metadatos (usuarios, peticiones, prioridades, etc.) de forma
     * asíncrona.
     * 
     * @param type tipo de metadatos (users, trackers, categories, priorities,
     *             statuses,
     *             versions, activities)
     * @param pid  identificador del proyecto (requerido para algunos tipos)
     * @return CompletableFuture con la lista de entidades
     */
    public CompletableFuture<List<SimpleEntity>> fetchMetadataAsync(String type, String pid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchMetadata(type, pid);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.fetch_metadata", type), e);
            }
        }, executorService);
    }

    public CompletableFuture<List<CustomFieldDefinition>> fetchCustomFieldDefinitionsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchCustomFieldDefinitions();
            } catch (Exception e) {
                throw new RuntimeException(I18n.get("async.error.fetch_custom_fields"), e);
            }
        }, executorService);
    }

    /**
     * Obtiene información detallada de una tarea específica de forma asíncrona.
     * 
     * @param id ID de la tarea
     * @return CompletableFuture con los detalles de la tarea
     */
    public CompletableFuture<Task> fetchTaskDetailsAsync(int id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchTaskDetails(id);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.task_details", id), e);
            }
        }, executorService);
    }

    /**
     * Crea una nueva tarea de forma asíncrona.
     * 
     * @param pid  identificador del proyecto
     * @param task tarea a crear
     * @return CompletableFuture con el ID de la nueva tarea
     */
    public CompletableFuture<Integer> createTaskAsync(String pid, Task task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.createTask(pid, task);
            } catch (Exception e) {
                throw new RuntimeException(I18n.get("async.error.create_task"), e);
            }
        }, executorService);
    }

    /**
     * Actualiza una tarea existente de forma asíncrona.
     * 
     * @param task tarea a actualizar
     * @return CompletableFuture que se completa cuando finaliza la actualización
     */
    public CompletableFuture<Void> updateTaskAsync(Task task) {
        return CompletableFuture.runAsync(() -> {
            try {
                delegate.updateTask(task);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.update_task", task.id), e);
            }
        }, executorService);
    }

    /**
     * Sube un archivo de forma asíncrona.
     * 
     * @param data        datos del archivo
     * @param contentType tipo MIME
     * @return CompletableFuture con el token de subida
     */
    public CompletableFuture<String> uploadFileAsync(byte[] data, String contentType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.uploadFile(data, contentType);
            } catch (Exception e) {
                throw new RuntimeException(I18n.get("async.error.upload_file"), e);
            }
        }, executorService);
    }

    /**
     * Descarga un adjunto de forma asíncrona.
     * 
     * @param att adjunto a descargar
     * @return CompletableFuture con los bytes del archivo
     */
    public CompletableFuture<byte[]> downloadAttachmentAsync(Attachment att) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.downloadAttachment(att);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.download_attachment", att.filename), e);
            }
        }, executorService);
    }

    /**
     * Imputa horas en una tarea de forma asíncrona.
     * 
     * @param issueId    ID de la tarea
     * @param date       fecha en formato YYYY-MM-DD
     * @param hours      horas trabajadas
     * @param userId     ID del usuario
     * @param activityId ID de la actividad
     * @param comment    comentario opcional
     * @return CompletableFuture que se completa cuando se imputan las horas
     */
    public CompletableFuture<Void> logTimeAsync(int issueId, String date, double hours,
            int userId, int activityId, String comment) {
        return CompletableFuture.runAsync(() -> {
            try {
                delegate.logTime(issueId, date, hours, userId, activityId, comment);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.log_time", issueId), e);
            }
        }, executorService);
    }

    /**
     * Obtiene entradas de tiempo de forma asíncrona.
     * 
     * @param pid      identificador del proyecto (null para todos)
     * @param dateFrom fecha inicio (YYYY-MM-DD)
     * @param dateTo   fecha fin (YYYY-MM-DD)
     * @return CompletableFuture con la lista de entradas de tiempo
     */
    public CompletableFuture<List<TimeEntry>> fetchTimeEntriesAsync(String pid, String dateFrom, String dateTo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchTimeEntries(pid, dateFrom, dateTo);
            } catch (Exception e) {
                throw new RuntimeException(I18n.get("async.error.fetch_time_entries"), e);
            }
        }, executorService);
    }

    /**
     * Obtiene todas las versiones de un proyecto de forma asíncrona.
     * 
     * @param pid identificador del proyecto
     * @return CompletableFuture con la lista de versiones
     */
    public CompletableFuture<List<VersionDTO>> fetchVersionsFullAsync(String pid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchVersionsFull(pid);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.fetch_versions", pid), e);
            }
        }, executorService);
    }

    /**
     * Crea una nueva versión de forma asíncrona.
     * 
     * @param pid       identificador del proyecto
     * @param name      nombre de la versión
     * @param status    estado de la versión
     * @param startDate fecha inicio (YYYY-MM-DD)
     * @param dueDate   fecha fin (YYYY-MM-DD)
     * @return CompletableFuture que se completa cuando se crea la versión
     */
    public CompletableFuture<Void> createVersionAsync(String pid, String name, String status,
            String startDate, String dueDate) {
        return CompletableFuture.runAsync(() -> {
            try {
                delegate.createVersion(pid, name, status, startDate, dueDate);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.create_version", name), e);
            }
        }, executorService);
    }

    /**
     * Actualiza una versión de forma asíncrona.
     * 
     * @param id        ID de la versión
     * @param name      nombre de la versión
     * @param status    estado de la versión
     * @param startDate fecha inicio (YYYY-MM-DD)
     * @param dueDate   fecha fin (YYYY-MM-DD)
     * @return CompletableFuture que se completa cuando se actualiza la versión
     */
    public CompletableFuture<Void> updateVersionAsync(int id, String name, String status,
            String startDate, String dueDate) {
        return CompletableFuture.runAsync(() -> {
            try {
                delegate.updateVersion(id, name, status, startDate, dueDate);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.update_version", id), e);
            }
        }, executorService);
    }

    /**
     * Elimina una versión de forma asíncrona.
     * 
     * @param id ID de la versión
     * @return CompletableFuture que se completa cuando se elimina la versión
     */
    public CompletableFuture<Void> deleteVersionAsync(int id) {
        return CompletableFuture.runAsync(() -> {
            try {
                delegate.deleteVersion(id);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.delete_version", id), e);
            }
        }, executorService);
    }

    /**
     * Obtiene tareas de una versión específica de forma asíncrona.
     * 
     * @param pid       identificador del proyecto
     * @param versionId ID de la versión
     * @return CompletableFuture con la lista de tareas
     */
    public CompletableFuture<List<Task>> fetchTasksByVersionAsync(String pid, int versionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchTasksByVersion(pid, versionId);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.fetch_version_tasks", versionId), e);
            }
        }, executorService);
    }

    /**
     * Obtiene tareas cerradas en un rango de fechas de forma asíncrona.
     * 
     * @param pid      identificador del proyecto
     * @param dateFrom fecha inicio (YYYY-MM-DD)
     * @param dateTo   fecha fin (YYYY-MM-DD)
     * @return CompletableFuture con la lista de tareas cerradas
     */
    public CompletableFuture<List<Task>> fetchClosedTasksAsync(String pid, String dateFrom, String dateTo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchClosedTasks(pid, dateFrom, dateTo);
            } catch (Exception e) {
                throw new RuntimeException(I18n.get("async.error.fetch_closed_tasks"), e);
            }
        }, executorService);
    }

    /**
     * Obtiene páginas wiki de un proyecto de forma asíncrona.
     * 
     * @param projectId identificador del proyecto
     * @return CompletableFuture con la lista de páginas wiki
     */
    public CompletableFuture<List<WikiPageDTO>> fetchWikiPagesAsync(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchWikiPages(projectId);
            } catch (Exception e) {
                throw new RuntimeException(I18n.get("async.error.fetch_wiki_pages"), e);
            }
        }, executorService);
    }

    /**
     * Obtiene el contenido de una página wiki específica de forma asíncrona.
     * 
     * @param projectId identificador del proyecto
     * @param pageTitle título de la página
     * @return CompletableFuture con el contenido de la página wiki
     */
    public CompletableFuture<WikiPageDTO> fetchWikiPageContentAsync(String projectId, String pageTitle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchWikiPageContent(projectId, pageTitle);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.fetch_wiki_page", pageTitle), e);
            }
        }, executorService);
    }

    /**
     * Crea o actualiza una página wiki de forma asíncrona.
     * 
     * @param projectId identificador del proyecto
     * @param pageTitle título de la página
     * @param content   contenido (Textile/Markdown)
     * @param comment   comentario del cambio
     * @return CompletableFuture que se completa cuando se guarda la página
     */
    public CompletableFuture<Void> createOrUpdateWikiPageAsync(String projectId, String pageTitle,
            String content, String comment) {
        return CompletableFuture.runAsync(() -> {
            try {
                delegate.createOrUpdateWikiPage(projectId, pageTitle, content, comment);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.save_wiki_page", pageTitle), e);
            }
        }, executorService);
    }

    /**
     * Obtiene el usuario autenticado actual de forma asíncrona.
     * 
     * @return CompletableFuture con la entidad del usuario actual
     */
    public CompletableFuture<SimpleEntity> fetchCurrentUserAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchCurrentUser();
            } catch (Exception e) {
                throw new RuntimeException(I18n.get("async.error.fetch_user"), e);
            }
        }, executorService);
    }

    public CompletableFuture<SimpleEntity> fetchProjectAsync(String identifier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchProject(identifier);
            } catch (Exception e) {
                throw new RuntimeException("Error fetching project info", e);
            }
        }, executorService);
    }

    public CompletableFuture<redmineconnector.model.ContextMetadata> fetchContextMetadataAsync(String projectId,
            int trackerId, int issueId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchContextMetadata(projectId, trackerId, issueId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch context metadata", e);
            }
        }, executorService);
    }

    /**
     * Elimina una página wiki de forma asíncrona.
     * 
     * @param projectId identificador del proyecto
     * @param pageTitle título de la página
     * @return CompletableFuture que se completa cuando se elimina la página
     */
    public CompletableFuture<Void> deleteWikiPageAsync(String projectId, String pageTitle) {
        return CompletableFuture.runAsync(() -> {
            try {
                delegate.deleteWikiPage(projectId, pageTitle);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.delete_wiki_page", pageTitle), e);
            }
        }, executorService);
    }

    /**
     * Sube y vincula un adjunto a una página wiki de forma asíncrona.
     * 
     * @param projectId   identificador del proyecto
     * @param pageTitle   título de la página
     * @param token       token del archivo subido
     * @param filename    nombre del archivo
     * @param contentType tipo MIME
     * @param currentText texto actual de la página (para evitar conflictos de
     *                    versión)
     * @param version     versión actual (para evitar conflictos)
     * @return CompletableFuture que se completa cuando se adjunta el archivo
     */
    public CompletableFuture<Void> uploadWikiAttachmentAsync(String projectId, String pageTitle, String token,
            String filename, String contentType, String currentText, int version) {
        return CompletableFuture.runAsync(() -> {
            try {
                delegate.uploadWikiAttachment(projectId, pageTitle, token, filename, contentType, currentText, version);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.upload_wiki_attachment", filename), e);
            }
        }, executorService);
    }

    /**
     * Obtiene el historial de versiones de una página wiki de forma asíncrona.
     * 
     * @param projectId identificador del proyecto
     * @param pageTitle título de la página
     * @return CompletableFuture con la lista de versiones
     */
    public CompletableFuture<List<WikiVersionDTO>> fetchWikiHistoryAsync(String projectId, String pageTitle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchWikiHistory(projectId, pageTitle);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.fetch_wiki_history", pageTitle), e);
            }
        }, executorService);
    }

    /**
     * Revierte una página wiki a una versión anterior de forma asíncrona.
     * 
     * @param projectId identificador del proyecto
     * @param pageTitle título de la página
     * @param version   versión a restaurar
     * @return CompletableFuture que se completa cuando se revierte la página
     */
    public CompletableFuture<Void> revertWikiPageAsync(String projectId, String pageTitle, int version) {
        return CompletableFuture.runAsync(() -> {
            try {
                delegate.revertWikiPage(projectId, pageTitle, version);
            } catch (Exception e) {
                throw new RuntimeException(I18n.format("async.error.revert_wiki_page", pageTitle), e);
            }
        }, executorService);
    }

    /**
     * Cierra el servicio ejecutor.
     * Debe llamarse cuando la aplicación se está cerrando.
     */
    public void shutdown() {
        executorService.shutdown();
        delegate.shutdown();
    }

    /**
     * Obtiene el DataService síncrono subyacente.
     * 
     * @return el DataService delegado
     */
    public DataService getDelegate() {
        return delegate;
    }

    public CompletableFuture<List<SimpleEntity>> fetchAllowedStatusesAsync(String pid, int trackerId, int issueId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.fetchAllowedStatuses(pid, trackerId, issueId);
            } catch (Exception e) {
                // Propagate the real message
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                throw new RuntimeException(I18n.get("async.error.allowed_statuses") + ": " + msg, e);
            }
        }, executorService);
    }
}
