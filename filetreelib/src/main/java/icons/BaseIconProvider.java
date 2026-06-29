package ir.hanzodev1375.filetreelib.icons;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ir.hanzodev1375.filetreelib.core.TreeNode;
import ir.hanzodev1375.filetreelib.model.FilePayload;

/**
 * پایه‌ای برای همه IconProvider ها. برای Glide، Coil یا هر loader دیگه‌ای extend کن.
 *
 * <p>مثال Glide:
 *
 * <pre>
 * public class GlideIconProvider extends BaseIconProvider {
 *     {@literal @}Override
 *     public void loadIconAsync(@NonNull Context ctx, @NonNull TreeNode node,
 *                               @NonNull ImageView target) {
 *         if (isImageFile(node)) {
 *             Glide.with(ctx).load(getFilePath(node)).into(target);
 *         } else {
 *             target.setImageDrawable(getIcon(ctx, node));
 *         }
 *     }
 * }
 * </pre>
 */
public abstract class BaseIconProvider implements IconProvider {

  /** آیا این node یک فایل تصویری است؟ برای Glide loader استفاده کن. */
  protected boolean isImageFile(@NonNull TreeNode node) {
    String name = node.getName().toLowerCase();
    return name.endsWith(".png")
        || name.endsWith(".jpg")
        || name.endsWith(".jpeg")
        || name.endsWith(".webp")
        || name.endsWith(".gif")
        || name.endsWith(".bmp")
        || name.endsWith(".svg");
  }

  /** مسیر absolute فایل رو برمی‌گردونه. اگر FilePayload نداشت، null برمی‌گردونه. */
  @Nullable
  protected String getFilePath(@NonNull TreeNode node) {
    FilePayload payload = node.getPayload(FilePayload.class);
    return payload != null ? payload.getAbsolutePath() : null;
  }

  /** Extension فایل رو برمی‌گردونه (بدون dot، lowercase). مثال: "java", "xml", "png" */
  @NonNull
  protected String getExtension(@NonNull TreeNode node) {
    String name = node.getName();
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) return "";
    return name.substring(dot + 1).toLowerCase();
  }

  /**
   * Override کن اگه می‌خوای آیکون رو async لود کنی (مثلاً با Glide). پیاده‌سازی پیش‌فرض synchronous
   * هست و از getIcon() استفاده می‌کنه.
   *
   * @param target ImageView که آیکون باید توش ست بشه
   */
  public void loadIconInto(
      @NonNull Context context, @NonNull TreeNode node, @NonNull android.widget.ImageView target) {
    Drawable d = getIcon(context, node);
    target.setImageDrawable(d);
  }

  /** Badge icon — پیاده‌سازی پیش‌فرض null برمی‌گردونه. Override کن اگه badge لازم داری. */
  @Nullable
  @Override
  public Drawable getBadgeIcon(@NonNull Context context, @NonNull TreeNode node) {
    return null;
  }
}
