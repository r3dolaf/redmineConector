package redmineconnector.util;

import javax.swing.SwingWorker;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factoría para crear SwingWorkers de forma simplificada.
 * 
 * <p>
 * Elimina la necesidad de crear clases anónimas repetitivas de SwingWorker,
 * proporcionando métodos de utilidad para los casos de uso más comunes.
 * </p>
 * 
 * <p>
 * Ejemplo de uso:
 * </p>
 * 
 * <pre>{@code
 * SwingWorkerFactory.createWorker(
 *         () -> service.fetchTasks(), // Tarea en background
 *         tasks -> updateUI(tasks), // Al completar exitosamente
 *         error -> showError(error) // Al fallar
 * ).execute();
 * }</pre>
 * 
 * @author Redmine Connector Team
 * @version 8.5.5
 */
public class SwingWorkerFactory {

    /**
     * Crea un SwingWorker con manejo de éxito y error.
     * 
     * @param <T>            Tipo de resultado de la tarea en background
     * @param backgroundTask Tarea a ejecutar en background thread
     * @param onSuccess      Callback al completar exitosamente (recibe el
     *                       resultado)
     * @param onError        Callback al fallar (recibe la excepción)
     * @return SwingWorker configurado
     */
    public static <T> SwingWorker<T, Void> createWorker(
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {

        return new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return backgroundTask.get();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    onSuccess.accept(result);
                } catch (Exception e) {
                    onError.accept(e);
                }
            }
        };
    }

    /**
     * Crea un SwingWorker solo con callback de éxito.
     * Los errores se manejan con ErrorHandler por defecto.
     * 
     * @param <T>            Tipo de resultado
     * @param backgroundTask Tarea en background
     * @param onSuccess      Callback de éxito
     * @param errorContext   Contexto para mensajes de error
     * @return SwingWorker configurado
     */
    public static <T> SwingWorker<T, Void> createWorker(
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess,
            String errorContext) {

        return createWorker(
                backgroundTask,
                onSuccess,
                error -> ErrorHandler.handle(error, errorContext));
    }

    /**
     * Crea un SwingWorker para tareas sin resultado (Runnable).
     * 
     * @param backgroundTask Tarea en background sin resultado
     * @param onSuccess      Callback al completar exitosamente
     * @param onError        Callback al fallar
     * @return SwingWorker configurado
     */
    public static SwingWorker<Void, Void> createVoidWorker(
            Runnable backgroundTask,
            Runnable onSuccess,
            Consumer<Exception> onError) {

        return new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                backgroundTask.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Lanza excepción si hubo error
                    onSuccess.run();
                } catch (Exception e) {
                    onError.accept(e);
                }
            }
        };
    }

    /**
     * Crea un SwingWorker para tareas sin resultado con manejo de error por
     * defecto.
     * 
     * @param backgroundTask Tarea en background
     * @param onSuccess      Callback de éxito
     * @param errorContext   Contexto para errores
     * @return SwingWorker configurado
     */
    public static SwingWorker<Void, Void> createVoidWorker(
            Runnable backgroundTask,
            Runnable onSuccess,
            String errorContext) {

        return createVoidWorker(
                backgroundTask,
                onSuccess,
                error -> ErrorHandler.handle(error, errorContext));
    }

    /**
     * Crea y ejecuta un SwingWorker en un solo paso.
     * 
     * @param <T>            Tipo de resultado
     * @param backgroundTask Tarea en background
     * @param onSuccess      Callback de éxito
     * @param onError        Callback de error
     */
    public static <T> void executeAsync(
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {

        createWorker(backgroundTask, onSuccess, onError).execute();
    }

    /**
     * Crea y ejecuta un SwingWorker con manejo de error por defecto.
     * 
     * @param <T>            Tipo de resultado
     * @param backgroundTask Tarea en background
     * @param onSuccess      Callback de éxito
     * @param errorContext   Contexto para errores
     */
    public static <T> void executeAsync(
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess,
            String errorContext) {

        createWorker(backgroundTask, onSuccess, errorContext).execute();
    }
}
