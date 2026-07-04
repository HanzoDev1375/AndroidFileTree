package ir.hanzodev1375.filetreelib.drag;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.filetreelib.adapter.TreeAdapter;
import ir.hanzodev1375.filetreelib.core.TreeNode;
import ir.hanzodev1375.filetreelib.core.TreeController;
import ir.hanzodev1375.filetreelib.provider.TreeDataProvider;
import ir.hanzodev1375.filetreelib.widget.TwoDScrollView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DragManager {

  public interface DragListener {
    void onNodeMoved(@NonNull TreeNode node, @NonNull TreeNode newParent, int newIndex);

    boolean canDrop(@NonNull TreeNode dragged, @NonNull TreeNode target);
  }

  @NonNull private final TreeDataProvider provider;
  @NonNull private final TreeController controller;
  @Nullable private DragListener dragListener;
  @NonNull private final ExecutorService executor = Executors.newSingleThreadExecutor();
  @NonNull private final Handler mainHandler = new Handler(Looper.getMainLooper());

  @Nullable private TreeNode draggedNode = null;
  @NonNull private List<TreeNode> draggedNodes = new ArrayList<>();
  @Nullable private TreeNode pendingDropTarget = null;
  private boolean dragLocked = false;

  private static final int AUTO_SCROLL_EDGE_PX = 120;
  private static final int AUTO_SCROLL_STEP_PX = 16;
  private static final long AUTO_SCROLL_INTERVAL_MS = 16L;

  @Nullable private TwoDScrollView dragScrollView;
  private float lastTouchX;
  private float lastTouchY;
  private boolean autoScrollActive = false;

  private final Runnable autoScrollRunnable =
      new Runnable() {
        @Override
        public void run() {
          if (!isDragging() || dragScrollView == null) {
            autoScrollActive = false;
            return;
          }

          int[] loc = new int[2];
          dragScrollView.getLocationOnScreen(loc);
          float localX = lastTouchX - loc[0];
          float localY = lastTouchY - loc[1];
          int width = dragScrollView.getWidth();
          int height = dragScrollView.getHeight();

          int dx = 0;
          int dy = 0;
          if (localX < AUTO_SCROLL_EDGE_PX) {
            dx = -AUTO_SCROLL_STEP_PX;
          } else if (localX > width - AUTO_SCROLL_EDGE_PX) {
            dx = AUTO_SCROLL_STEP_PX;
          }
          if (localY < AUTO_SCROLL_EDGE_PX) {
            dy = -AUTO_SCROLL_STEP_PX;
          } else if (localY > height - AUTO_SCROLL_EDGE_PX) {
            dy = AUTO_SCROLL_STEP_PX;
          }

          if (dx != 0 || dy != 0) {
            dragScrollView.scrollBy(dx, dy);
            mainHandler.postDelayed(this, AUTO_SCROLL_INTERVAL_MS);
          } else {
            autoScrollActive = false;
          }
        }
      };

  public DragManager(@NonNull TreeController controller) {
    this.controller = controller;
    this.provider = controller.getDataProvider();
  }

  public void setDragListener(@Nullable DragListener listener) {
    this.dragListener = listener;
  }

  /**
   * When {@code locked}, no node can be picked up at all, regardless of type — a manual "lock
   * everything" switch a host app can reach for if it ever needs one (e.g. while a bulk operation
   * is in progress). Not used automatically by {@code FileTreeView}'s "android mod" project view:
   * that view's virtual groupings (like "Gradle Scripts"/"res") are already unmovable via the
   * {@code TreeNode#isVirtual()} checks below, but the real files/folders it shows (module
   * folders, {@code java}, individual resource files, ...) still point at real paths and stay
   * draggable — moving them is exactly as safe there as anywhere else in the tree.
   */
  public void setDragLocked(boolean locked) {
    this.dragLocked = locked;
    if (locked) cancel();
  }

  public boolean isDragLocked() {
    return dragLocked;
  }

  /**
   * ItemTouchHelper callback که به RecyclerView وصل میشه. از طریق TreeView.attachDragManager() صدا
   * زده میشه.
   */
  public ItemTouchHelper buildItemTouchHelper(@NonNull TreeAdapter adapter) {
    return new ItemTouchHelper(
        new ItemTouchHelper.SimpleCallback(0, 0) {

          @Override
          public int getMovementFlags(
              @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int position = viewHolder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return 0;
            TreeNode node = adapter.getNode(position);
            // Virtual groupings (e.g. "Gradle Scripts", "res") have no real filesystem entry to
            // move, so they can never be picked up — regardless of dragLocked. dragLocked itself
            // is a separate, manual "lock everything" switch a host app can opt into; it's off by
            // default and not tied to android-mod (real files/folders there stay draggable).
            if (node == null || node.isVirtual() || dragLocked) return 0;
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
          }

          @Override
          public boolean onMove(
              @NonNull RecyclerView recyclerView,
              @NonNull RecyclerView.ViewHolder dragged,
              @NonNull RecyclerView.ViewHolder target) {

            int fromPos = dragged.getAdapterPosition();
            int toPos = target.getAdapterPosition();
            if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION)
              return false;

            TreeNode draggedNode = adapter.getNode(fromPos);
            TreeNode targetNode = adapter.getNode(toPos);
            if (draggedNode == null || targetNode == null) return false;

            // جلوگیری از drop روی خودش یا فرزندانش
            if (draggedNode.getId().equals(targetNode.getId())) return false;
            if (targetNode.isDescendantOf(draggedNode)) return false;

            // onMove fires repeatedly while the finger is still moving over each row passed —
            // it is NOT a drop event. Only remember the current hover target here; the actual
            // file-system move runs once, in clearView(), when the drag gesture actually ends.
            startDrag(draggedNode);
            pendingDropTarget = targetNode;
            return false;
          }

          @Override
          public void clearView(
              @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            TreeNode target = pendingDropTarget;
            pendingDropTarget = null;
            if (target != null) drop(target, null);
          }

          @Override
          public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // swipe غیرفعاله
          }

          @Override
          public boolean isLongPressDragEnabled() {
            return true;
          }
        });
  }

  /**
   * Feed every raw touch event here (call from {@code TreeView.dispatchTouchEvent()}, NOT via
   * {@code RecyclerView.addOnItemTouchListener}). This must come from dispatchTouchEvent()
   * because once ItemTouchHelper's own OnItemTouchListener claims a drag gesture, RecyclerView
   * stops delivering events to any other OnItemTouchListener for that gesture — dispatchTouchEvent
   * is the only place that reliably still sees every event regardless of who else intercepts it.
   *
   * <p>Note: {@code TwoDScrollView} measures its RecyclerView child with an unspecified height,
   * so the RecyclerView itself never has internal vertical scroll range — both axes are actually
   * scrolled by the TwoDScrollView wrapper, which is why ItemTouchHelper's own built-in
   * auto-scroll (RecyclerView-based) never fires here and both axes must be driven manually.
   */
  public void onRawTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
    if (!isDragging()) {
      autoScrollActive = false;
      return;
    }

    ViewParent parent = rv.getParent();
    if (!(parent instanceof TwoDScrollView)) return;
    dragScrollView = (TwoDScrollView) parent;

    int action = e.getActionMasked();
    if (action == MotionEvent.ACTION_MOVE) {
      lastTouchX = e.getRawX();
      lastTouchY = e.getRawY();
      if (!autoScrollActive) {
        autoScrollActive = true;
        mainHandler.post(autoScrollRunnable);
      }
    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      autoScrollActive = false;
      mainHandler.removeCallbacks(autoScrollRunnable);
    }
  }

  public void startDrag(@NonNull TreeNode node) {
    if (dragLocked || node.isVirtual()) return;
    this.draggedNode = node;
    List<TreeNode> selected = controller.getSelectionManager().getSelectedNodes();
    this.draggedNodes =
        (node.isSelected() && selected.size() > 1)
            ? new ArrayList<>(selected)
            : Collections.singletonList(node);
  }

  public void drop(@NonNull TreeNode targetNode, @Nullable Runnable onSuccess) {

    if (draggedNode == null || draggedNodes.isEmpty()) return;
    // Drop any selected node that is a descendant of another selected node — moving the
    // parent already carries it along, so processing it again would operate on a path that
    // no longer exists once the parent has moved.
    final List<TreeNode> dragged =
        ir.hanzodev1375.filetreelib.utils.TreeUtils.filterTopLevel(draggedNodes);
    draggedNode = null;
    draggedNodes = new ArrayList<>();

    if (dragLocked) return;

    // Dropping directly onto a virtual grouping (e.g. "res", "Gradle Scripts") has no real
    // destination — don't silently redirect to its real parent, since that would move the file
    // into the wrong place (e.g. the module root instead of "inside" res) without any indication
    // to the user that it didn't actually land where they dropped it.
    if (targetNode.isVirtual()) return;

    TreeNode destination = targetNode.isFolder() ? targetNode : targetNode.getParent();
    if (destination == null || destination.isVirtual()) return;

    // Skip any dragged node that the destination is invalid for: dropping onto itself, into
    // one of its own subfolders, a virtual node with nothing real to move, or rejected by the
    // host app's canDrop().
    final List<TreeNode> validDragged = new ArrayList<>();
    for (TreeNode n : dragged) {
      if (n.isVirtual()) continue;
      if (n.getId().equals(destination.getId())) continue;
      if (destination.isDescendantOf(n)) continue;
      if (dragListener != null && !dragListener.canDrop(n, destination)) continue;
      validDragged.add(n);
    }
    if (validDragged.isEmpty()) return;

    final TreeNode finalDestination = destination;

    executor.submit(
        () -> {
          try {
            Map<TreeNode, String> resolvedPaths = provider.moveNodes(validDragged, finalDestination);
            mainHandler.post(
                () -> {
                  for (TreeNode n : validDragged) {
                    String resolvedPath = resolvedPaths.get(n);
                    if (resolvedPath == null) continue; // provider skipped it (no payload)
                    controller.applyMovedNode(n, finalDestination, resolvedPath);
                    if (dragListener != null)
                      dragListener.onNodeMoved(
                          n, finalDestination, finalDestination.indexOfChild(n));
                  }
                  if (onSuccess != null) onSuccess.run();
                });
          } catch (Exception e) {
            mainHandler.post(
                () -> {
                  if (dragListener != null) {
                    for (TreeNode n : validDragged) {
                      dragListener.onNodeMoved(n, finalDestination, -1);
                    }
                  }
                });
          }
        });
  }

  public void cancel() {
    draggedNode = null;
    draggedNodes = new ArrayList<>();
    pendingDropTarget = null;
    autoScrollActive = false;
    mainHandler.removeCallbacks(autoScrollRunnable);
  }

  @Nullable
  public TreeNode getDraggedNode() {
    return draggedNode;
  }

  public boolean isDragging() {
    return draggedNode != null;
  }
}
