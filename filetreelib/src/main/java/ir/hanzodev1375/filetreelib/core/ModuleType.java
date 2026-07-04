package ir.hanzodev1375.filetreelib.core;

/**
 * Gradle module classification used by {@link ModuleUtils} and {@code FileTreeView}'s "android
 * mod" project-view (see {@code FileTreeView#setAndroidMod}). Ported from the reference module
 * (com.jahangir.app.features.editor.file.ModuleType) so filetreelib can tell an Android app
 * module apart from a library module instead of treating every folder with a build.gradle the
 * same way.
 */
public enum ModuleType {
  /** com.android.application */
  ANDROID_APP,
  /** com.android.library */
  ANDROID_LIBRARY,
  /** java-library */
  JAVA_LIBRARY,
  /** kotlin("jvm") / org.jetbrains.kotlin.jvm */
  KOTLIN_JVM,
  /** Has a build.gradle(.kts) but no recognized plugin */
  GENERIC_MODULE,
  /** No build.gradle(.kts) in the folder at all */
  NOT_A_MODULE
}
