package ir.hanzodev1375.filetreelib.datas;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/** 
* A helper class for clicking with path filtering
* author: Ghost
*/
public class FileTypeText {
  private Set<String> allType =
      new HashSet<>(
          Arrays.asList(
              ".html",
              ".java",
              ".c",
              ".cs",
              ".cpp",
              ".cxx",
              ".hpp",
              ".hxx",
              ".cc",
              ".h",
              ".css",
              ".js",
              ".py",
              ".json",
              ".xml",
              ".kt",
              ".kts",
              ".ts",
              ".tsx",
              ".toml",
              ".groovy",
              ".gradle",
              ".sass",
              ".scss",
              ".md",
              ".markdown",
              ".yml",
              ".yaml",
              ".lua",
              ".go",
              ".php",
              ".dart",
              ".tsx",
              ".jsx",
              ".sql",
              ".sh",
              ".rc",
              ".bash",
              ".bashrc",
              ".ash",
              ".zsh",
              ".zshrc",
              ".rs",
              ".rb",
              ".g4",
              ".ini",
              ".zig"));
  private Set<String> androidType =
      new HashSet<>(
          Arrays.asList(".java", ".kt", ".xml", ".kts", ".aidl", ".gradle", ".toml", ".md"));

  public boolean getAllTypeText(String path) {
    int lastDot = path.lastIndexOf(".");
    String extension = (lastDot > 0) ? path.substring(lastDot).toLowerCase() : "";
    return allType.contains(extension);
  }

  public boolean getAndroidType(String path) {
    int lastDot = path.lastIndexOf(".");
    String extension = (lastDot > 0) ? path.substring(lastDot).toLowerCase() : "";
    return androidType.contains(extension);
  }
}
