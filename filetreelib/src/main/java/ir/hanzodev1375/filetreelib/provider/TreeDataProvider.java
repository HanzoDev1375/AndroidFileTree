package ir.hanzodev1375.filetreelib.provider;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import ir.hanzodev1375.filetreelib.core.TreeNode;
import java.util.List;
import java.util.Map;

/**
 * Abstract data source for the tree. Implement this to connect the tree to any backend: real
 * filesystem, network API, mock data, etc.
 */
public interface TreeDataProvider {

  /** Loads and returns the children of the given parent node. Called on a background thread. */
  @WorkerThread
  @NonNull
  List<TreeNode> loadChildren(@NonNull TreeNode parent) throws Exception;

  /**
   * Returns true if the given node has children (without loading them). Used to decide whether to
   * show an expand arrow.
   */
  @WorkerThread
  boolean hasChildren(@NonNull TreeNode node) throws Exception;

  /**
   * Renames the node. Should rename the underlying resource (file/folder). Called on a background
   * thread.
   */
  @WorkerThread
  void renameNode(@NonNull TreeNode node, @NonNull String newName) throws Exception;

  /** Deletes the given nodes and their contents. Called on a background thread. */
  @WorkerThread
  void deleteNodes(@NonNull List<TreeNode> nodes) throws Exception;

  /**
   * Creates a new node (file or folder) under parent with the given name and type. Returns the
   * newly created TreeNode. Called on a background thread.
   */
  @WorkerThread
  @NonNull
  TreeNode createNode(@NonNull TreeNode parent, @NonNull String name, int type) throws Exception;

  /** Copies nodes to the destination parent. Called on a background thread. */
  @WorkerThread
  void copyNodes(@NonNull List<TreeNode> nodes, @NonNull TreeNode destination) throws Exception;

  /**
   * Moves nodes to the destination parent. Called on a background thread.
   *
   * @return the actual absolute path each successfully-moved node ended up at, keyed by the
   *     original node. This can differ from the naive "destination + name" guess if a
   *     same-named file/folder already existed there — implementations are expected to resolve
   *     that conflict (e.g. by appending " (1)") rather than overwrite, so callers must use the
   *     returned path (not recompute their own) when updating the node's identity afterward.
   */
  @WorkerThread
  @NonNull
  Map<TreeNode, String> moveNodes(@NonNull List<TreeNode> nodes, @NonNull TreeNode destination)
      throws Exception;

  /**
   * Copies or moves {@code nodes} into {@code destination}. Called on a background thread.
   *
   * <p>For a FILE-SYSTEM implementation:
   *
   * <ul>
   *   <li>COPY — duplicate each file/folder under {@code destination}
   *   <li>MOVE (isCut) — rename/move each file/folder under {@code destination}
   * </ul>
   *
   * @param destination the target folder node
   * @param nodes the nodes to paste (from clipboard)
   * @param isCut true = move, false = copy
   * @return list of newly created {@link TreeNode}s ready to be inserted in the model
   * @throws Exception on any I/O failure
   */
  @WorkerThread
  @NonNull
  List<TreeNode> pasteNodes(
      @NonNull TreeNode destination, @NonNull List<TreeNode> nodes, boolean isCut) throws Exception;
}
