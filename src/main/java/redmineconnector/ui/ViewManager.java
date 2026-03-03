package redmineconnector.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages UI coordination, synchronization between instances,
 * and view-specific state.
 */
public class ViewManager {
    private final String defaultTitle;
    private String title;
    private final List<InstanceController> peers = new ArrayList<>();
    private Consumer<Boolean> onSyncMatch;
    private boolean isSelectionSyncing = false;
    private boolean isOffline = false;

    public ViewManager(String defaultTitle) {
        this.defaultTitle = defaultTitle;
        this.title = defaultTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public List<InstanceController> getPeers() {
        return peers;
    }

    public void addPeer(InstanceController peer) {
        if (peer != null && !peers.contains(peer)) {
            peers.add(peer);
        }
    }

    public boolean isSelectionSyncing() {
        return isSelectionSyncing;
    }

    public void setSelectionSyncing(boolean syncing) {
        this.isSelectionSyncing = syncing;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public void setOffline(boolean offline) {
        this.isOffline = offline;
    }

    public void setOnSyncMatch(Consumer<Boolean> onSyncMatch) {
        this.onSyncMatch = onSyncMatch;
    }

    public Consumer<Boolean> getOnSyncMatch() {
        return onSyncMatch;
    }
}
