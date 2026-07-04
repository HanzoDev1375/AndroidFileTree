package ir.hanzodev1375.filetreelib.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Gradle module detection, ported from the reference implementation
 * (com.jahangir.app.features.editor.file.ModuleUtils). Only the {@code java.io.File} path is
 * kept — filetreelib's {@code FileTreeProvider} builds its tree from local files only, it has no
 * SAF/DocumentFile tree, so the DocumentFile overloads from the reference were dropped as dead
 * code here.
 */
public final class ModuleUtils {

  private ModuleUtils() {}

  /** {@code build.gradle}/{@code build.gradle.kts} exists in {@code folder} and its plugin type. */
  public static ModuleType getModuleType(File folder) {
    if (folder == null || !folder.isDirectory()) return ModuleType.NOT_A_MODULE;
    File gradleFile = new File(folder, "build.gradle");
    File gradleKtsFile = new File(folder, "build.gradle.kts");
    if (!gradleFile.exists() && !gradleKtsFile.exists()) return ModuleType.NOT_A_MODULE;
    try {
      String content = gradleFile.exists() ? readFile(gradleFile) : readFile(gradleKtsFile);
      return detectTypeFromContent(content);
    } catch (IOException e) {
      return ModuleType.GENERIC_MODULE;
    }
  }

  /** {@code folder} has a {@code build.gradle}/{@code build.gradle.kts} file. */
  public static boolean isModule(File folder) {
    if (folder == null) return false;
    return new File(folder, "build.gradle").exists() || new File(folder, "build.gradle.kts").exists();
  }

  /** {@code moduleDir} is declared with {@code include(...)} in the nearest settings.gradle(.kts) above it. */
  public static boolean isRegistered(File moduleDir) {
    File rootDir = findSettingsGradleRoot(moduleDir);
    return rootDir != null && isDeclaredModuleInSettings(rootDir, moduleDir);
  }

  // ---------------------------------------------------------------------------------------------
  // PRIVATE HELPERS
  // ---------------------------------------------------------------------------------------------

  private static ModuleType detectTypeFromContent(String content) {
    if (content.contains("com.android.application")) return ModuleType.ANDROID_APP;
    if (content.contains("com.android.library")) return ModuleType.ANDROID_LIBRARY;
    if (content.contains("java-library")) return ModuleType.JAVA_LIBRARY;
    if (content.contains("kotlin(\"jvm\")") || content.contains("org.jetbrains.kotlin.jvm")) {
      return ModuleType.KOTLIN_JVM;
    }
    return ModuleType.GENERIC_MODULE;
  }

  private static boolean isDeclaredModuleInSettings(File rootDir, File moduleDir) {
    File settingsFile = findSettingsGradle(rootDir);
    if (settingsFile == null) return false;
    String modulePath = toGradleModulePath(rootDir, moduleDir);
    if (modulePath == null) return false;
    try {
      String content = readFile(settingsFile);
      content = content.replaceAll("//.*", "").replaceAll("/\\*(.|\\R)*?\\*/", "");
      String quotedPath = Pattern.quote(modulePath);
      Pattern pattern =
          Pattern.compile(
              "include\\s*\\(?[^)]*?['\"]" + quotedPath + "['\"]", Pattern.MULTILINE | Pattern.DOTALL);
      return pattern.matcher(content).find();
    } catch (IOException e) {
      return false;
    }
  }

  private static File findSettingsGradle(File startDir) {
    File dir = startDir;
    while (dir != null) {
      File s = new File(dir, "settings.gradle");
      File sKts = new File(dir, "settings.gradle.kts");
      if (s.exists()) return s;
      if (sKts.exists()) return sKts;
      dir = dir.getParentFile();
    }
    return null;
  }

  private static File findSettingsGradleRoot(File startDir) {
    File dir = startDir;
    while (dir != null) {
      if (new File(dir, "settings.gradle").exists() || new File(dir, "settings.gradle.kts").exists()) {
        return dir;
      }
      dir = dir.getParentFile();
    }
    return null;
  }

  private static String toGradleModulePath(File rootDir, File moduleDir) {
    try {
      String rootPath = rootDir.getCanonicalPath();
      String modulePath = moduleDir.getCanonicalPath();
      if (!modulePath.startsWith(rootPath)) return null;
      String relative = modulePath.substring(rootPath.length());
      String sep = File.separator.equals("\\") ? "\\\\" : File.separator;
      String[] parts = relative.split(sep);
      StringBuilder sb = new StringBuilder();
      for (String p : parts) {
        if (p.isEmpty()) continue;
        sb.append(":").append(p);
      }
      return sb.toString();
    } catch (IOException e) {
      return null;
    }
  }

  private static String readFile(File file) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append('\n');
      }
    }
    return sb.toString();
  }
}
