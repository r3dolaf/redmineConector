package redmineconnector.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

/**
 * Clase de utilidad para simplificar operaciones asíncronas con
 * CompletableFuture en aplicaciones Swing.
 * Maneja automáticamente el paso de hilos entre tareas en segundo plano y el
 * hilo de despacho de eventos (EDT).
 */
public class AsyncUIHelper {

    /**
     * Ejecuta una operación asíncrona con gestión automática del EDT.
     * 
     * @param <T>       tipo del resultado
     * @param future    el CompletableFuture a ejecutar
     * @param onSuccess callback en caso de éxito (se ejecuta en el EDT)
     * @param onError   callback en caso de error (se ejecuta en el EDT)
     * @param onFinally callback que siempre se ejecuta al finalizar (se ejecuta en
     *                  el EDT)
     */
    public static <T> void executeAsync(
            CompletableFuture<T> future,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError,
            Runnable onFinally) {

        future
                .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                    try {
                        if (onSuccess != null) {
                            onSuccess.accept(result);
                        }
                    } finally {
                        if (onFinally != null) {
                            onFinally.run();
                        }
                    }
                }))
                .exceptionally(e -> {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            if (onError != null) {
                                onError.accept(e);
                            }
                        } finally {
                            if (onFinally != null) {
                                onFinally.run();
                            }
                        }
                    });
                    return null;
                });
    }

    /**
     * Ejecuta una operación asíncrona con gestión automática del EDT (sin callback
     * finally).
     * 
     * @param <T>       tipo del resultado
     * @param future    el CompletableFuture a ejecutar
     * @param onSuccess callback en caso de éxito (se ejecuta en el EDT)
     * @param onError   callback en caso de error (se ejecuta en el EDT)
     */
    public static <T> void executeAsync(
            CompletableFuture<T> future,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError) {
        executeAsync(future, onSuccess, onError, null);
    }

    /**
     * Ejecuta una operación asíncrona que no devuelve resultado (Void) con gestión
     * automática del EDT.
     * 
     * @param future    el CompletableFuture a ejecutar
     * @param onSuccess callback en caso de éxito (se ejecuta en el EDT)
     * @param onError   callback en caso de error (se ejecuta en el EDT)
     * @param onFinally callback que siempre se ejecuta al finalizar (se ejecuta en
     *                  el EDT)
     */
    public static void executeAsyncVoid(
            CompletableFuture<Void> future,
            Runnable onSuccess,
            Consumer<Throwable> onError,
            Runnable onFinally) {

        executeAsync(
                future,
                v -> {
                    if (onSuccess != null)
                        onSuccess.run();
                },
                onError,
                onFinally);
    }

    /**
     * Ejecuta una operación asíncrona que no devuelve resultado (Void) con gestión
     * automática del EDT (sin callback finally).
     * 
     * @param future    el CompletableFuture a ejecutar
     * @param onSuccess callback en caso de éxito (se ejecuta en el EDT)
     * @param onError   callback en caso de error (se ejecuta en el EDT)
     */
    public static void executeAsyncVoid(
            CompletableFuture<Void> future,
            Runnable onSuccess,
            Consumer<Throwable> onError) {
        executeAsyncVoid(future, onSuccess, onError, null);
    }
}
