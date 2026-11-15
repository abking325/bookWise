package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.localization.stringsFor
import com.example.model.AppThemeMode
import com.example.model.Book
import com.example.model.BookType
import com.example.model.DuplicateResolution
import com.example.model.LibraryFilter
import com.example.model.NavigationStyle
import com.example.ui.components.FloatingTtsPlayer
import com.example.ui.screens.BookDetailsScreen
import com.example.ui.screens.BookEditorScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryListScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.viewmodel.BookWiseViewModel
import kotlinx.coroutines.launch

object Routes {
  const val HOME = "home"
  const val FULL_BOOKS = "full_books"
  const val SUMMARIES = "book_summaries"
  const val CATEGORIES = "categories"
  const val SETTINGS = "settings"
  const val BOOK_DETAILS = "book_details/{bookId}"
  const val READER = "reader/{bookId}?lang={lang}&ch={ch}&para={para}"
  const val EDITOR = "book_editor?bookId={bookId}&lang={lang}&type={type}"

  fun bookDetails(bookId: String) = "book_details/$bookId"

  fun reader(bookId: String, lang: String, ch: Int = 0, para: Int = 0) =
    "reader/$bookId?lang=$lang&ch=$ch&para=$para"

  fun editor(bookId: String? = null, lang: String? = null, type: BookType? = null): String {
    val b = bookId ?: ""
    val l = lang ?: ""
    val t = type?.id ?: ""
    return "book_editor?bookId=$b&lang=$l&type=$t"
  }
}

private data class TopLevelDestination(
  val route: String,
  val label: String,
  val icon: ImageVector,
  val testTag: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookWiseAppShell(viewModel: BookWiseViewModel) {
  val context = LocalContext.current
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

  val books by viewModel.books.collectAsStateWithLifecycle()
  val categories by viewModel.categories.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()
  val offlineVoices by viewModel.offlineVoices.collectAsStateWithLifecycle()
  val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
  val pendingBackup by viewModel.pendingBackup.collectAsStateWithLifecycle()
  val pendingMigration by viewModel.pendingMigration.collectAsStateWithLifecycle()
  val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

  val activeChapter by viewModel.activeChapter.collectAsStateWithLifecycle()
  val readerBookmarks by viewModel.readerBookmarks.collectAsStateWithLifecycle()
  val readerHighlights by viewModel.readerHighlights.collectAsStateWithLifecycle()
  val readerNotes by viewModel.readerNotes.collectAsStateWithLifecycle()
  val searchHits by viewModel.inBookSearchHits.collectAsStateWithLifecycle()

  val strings = remember(settings.appLanguage) { stringsFor(settings.appLanguage) }
  val snackbarHostState = remember { SnackbarHostState() }
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  var fullBooksFilter by remember { mutableStateOf(LibraryFilter()) }
  var summariesFilter by remember { mutableStateOf(LibraryFilter()) }
  var exportingBook by remember { mutableStateOf<Book?>(null) }

  val exportBookLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
      val book = exportingBook
      exportingBook = null
      if (uri != null && book != null) {
        viewModel.exportBookZip(book.id, uri)
      }
    }

  val importBackupFromHomeLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) viewModel.inspectBackupUriSafe(uri)
    }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

  fun ensureNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      settings.ttsBackgroundPlaybackEnabled
    ) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  val topDestinations =
    listOf(
      TopLevelDestination(Routes.HOME, strings.navHome, Icons.Filled.Home, "nav_home"),
      TopLevelDestination(
        Routes.FULL_BOOKS,
        strings.navFullBooks,
        Icons.AutoMirrored.Filled.MenuBook,
        "nav_full_books",
      ),
      TopLevelDestination(Routes.SUMMARIES, strings.navSummaries, Icons.Filled.Summarize, "nav_summaries"),
      TopLevelDestination(Routes.CATEGORIES, strings.navCategories, Icons.Filled.Category, "nav_categories"),
    )

  val isTopLevelRoute =
    currentRoute == Routes.HOME ||
      currentRoute == Routes.FULL_BOOKS ||
      currentRoute == Routes.SUMMARIES ||
      currentRoute == Routes.CATEGORIES ||
      currentRoute == Routes.SETTINGS

  fun navigateTopLevel(route: String) {
    navController.navigate(route) {
      popUpTo(Routes.HOME) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun openReader(book: Book, lang: String? = null, ch: Int? = null, para: Int? = null) {
    val targetLang = lang ?: book.defaultLanguageCode
    val saved = book.edition(targetLang)?.progress
    val chIdx = ch ?: saved?.chapterIndex ?: 0
    val paraIdx = para ?: saved?.paragraphIndex ?: 0
    navController.navigate(Routes.reader(book.id, targetLang, chIdx, paraIdx))
  }

  fun cycleTtsSpeed() {
    val speeds = listOf(0.8f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    val cur = ttsState.speechRate
    val next = speeds.firstOrNull { it > cur + 0.05f } ?: speeds.first()
    viewModel.ttsManager.updateSpeechRate(next)
  }

  val scaffoldContent: @Composable () -> Unit = {
    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        if (isTopLevelRoute) {
          Column(modifier = Modifier.statusBarsPadding()) {
            TopAppBar(
              colors =
                TopAppBarDefaults.topAppBarColors(
                  containerColor = MaterialTheme.colorScheme.background,
                ),
              navigationIcon = {
                if (settings.navigationStyle == NavigationStyle.HAMBURGER) {
                  IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.testTag("hamburger_menu_button"),
                  ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                  }
                }
              },
              title = {
                if (settings.navigationStyle != NavigationStyle.TOP_BAR) {
                  Text(
                    text = strings.appTitle,
                    fontFamily = PlayfairDisplayFamily,
                    style = MaterialTheme.typography.titleLarge,
                  )
                }
              },
              actions = {
                IconButton(
                  onClick = { viewModel.toggleLightDarkQuick() },
                  modifier = Modifier.size(48.dp).testTag("quick_theme_toggle"),
                ) {
                  Icon(
                    imageVector =
                      if (settings.themeMode == AppThemeMode.DARK) Icons.Filled.LightMode
                      else Icons.Filled.DarkMode,
                    contentDescription = "Toggle light or dark mode",
                  )
                }
                IconButton(
                  onClick = { navigateTopLevel(Routes.SETTINGS) },
                  modifier = Modifier.size(48.dp).testTag("nav_settings_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = strings.navSettings,
                    tint =
                      if (currentRoute == Routes.SETTINGS) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface,
                  )
                }
              },
            )

            // Default Top Navigation bar
            if (settings.navigationStyle == NavigationStyle.TOP_BAR) {
              val selectedTabIdx = topDestinations.indexOfFirst { it.route == currentRoute }
              if (selectedTabIdx >= 0 || currentRoute == Routes.SETTINGS) {
                TabRow(
                  selectedTabIndex = selectedTabIdx.coerceAtLeast(0),
                  containerColor = MaterialTheme.colorScheme.background,
                  divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
                ) {
                  topDestinations.forEachIndexed { idx, dest ->
                    Tab(
                      selected = currentRoute == dest.route,
                      onClick = { navigateTopLevel(dest.route) },
                      modifier = Modifier.testTag(dest.testTag),
                      text = {
                        Text(
                          text = dest.label,
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight =
                            if (currentRoute == dest.route) FontWeight.SemiBold
                            else FontWeight.Normal,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      },
                    )
                  }
                }
              }
            }
          }
        }
      },
      bottomBar = {
        if (isTopLevelRoute) {
          when (settings.navigationStyle) {
            NavigationStyle.FIXED_BOTTOM -> {
              NavigationBar {
                topDestinations.forEach { dest ->
                  NavigationBarItem(
                    selected = currentRoute == dest.route,
                    onClick = { navigateTopLevel(dest.route) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag(dest.testTag),
                  )
                }
              }
            }
            NavigationStyle.FLOATING_BOTTOM -> {
              Box(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
              ) {
                Surface(
                  shape = RoundedCornerShape(28.dp),
                  tonalElevation = 6.dp,
                  shadowElevation = 10.dp,
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    topDestinations.forEach { dest ->
                      val selected = currentRoute == dest.route
                      Surface(
                        onClick = { navigateTopLevel(dest.route) },
                        shape = RoundedCornerShape(22.dp),
                        color =
                          if (selected) MaterialTheme.colorScheme.primaryContainer
                          else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.height(48.dp).testTag(dest.testTag),
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.padding(horizontal = 12.dp),
                        ) {
                          Icon(
                            dest.icon,
                            contentDescription = dest.label,
                            tint =
                              if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                          )
                          if (selected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                              text = dest.label,
                              style = MaterialTheme.typography.labelMedium,
                              color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            else -> Unit
          }
        }
      },
    ) { padding ->
      Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        NavHost(
          navController = navController,
          startDestination = Routes.HOME,
          modifier = Modifier.fillMaxSize(),
        ) {
          composable(Routes.HOME) {
            HomeScreen(
              books = books,
              categories = categories,
              settings = settings,
              strings = strings,
              onOpenBookDetails = { navController.navigate(Routes.bookDetails(it.id)) },
              onReadBook = { openReader(it) },
              onOpenAddBook = { navController.navigate(Routes.editor()) },
              onOpenCategory = { catName ->
                fullBooksFilter = fullBooksFilter.copy(category = catName)
                navigateTopLevel(Routes.FULL_BOOKS)
              },
              onImportBackup = {
                importBackupFromHomeLauncher.launch(
                  arrayOf("application/zip", "application/json", "text/html", "*/*")
                )
              },
            )
          }

          composable(Routes.FULL_BOOKS) {
            val filtered =
              remember(books, fullBooksFilter) {
                viewModel.filterAndSort(books, BookType.FULL_BOOK, fullBooksFilter)
              }
            LibraryListScreen(
              screenTitle = strings.navFullBooks,
              bookType = BookType.FULL_BOOK,
              books = filtered,
              categories = categories,
              filter = fullBooksFilter,
              strings = strings,
              onFilterChange = { fullBooksFilter = it },
              onOpenBookDetails = { navController.navigate(Routes.bookDetails(it.id)) },
              onReadBook = { openReader(it) },
              onToggleFavorite = viewModel::toggleFavorite,
              onAddBook = { type -> navController.navigate(Routes.editor(type = type)) },
            )
          }

          composable(Routes.SUMMARIES) {
            val filtered =
              remember(books, summariesFilter) {
                viewModel.filterAndSort(books, BookType.SUMMARY, summariesFilter)
              }
            LibraryListScreen(
              screenTitle = strings.navSummaries,
              bookType = BookType.SUMMARY,
              books = filtered,
              categories = categories,
              filter = summariesFilter,
              strings = strings,
              onFilterChange = { summariesFilter = it },
              onOpenBookDetails = { navController.navigate(Routes.bookDetails(it.id)) },
              onReadBook = { openReader(it) },
              onToggleFavorite = viewModel::toggleFavorite,
              onAddBook = { type -> navController.navigate(Routes.editor(type = type)) },
            )
          }

          composable(Routes.CATEGORIES) {
            CategoriesScreen(
              categories = categories,
              books = books,
              strings = strings,
              onSelectCategory = { catName ->
                fullBooksFilter = fullBooksFilter.copy(category = catName)
                navigateTopLevel(Routes.FULL_BOOKS)
              },
              onCreateCategory = viewModel::createCategory,
              onRenameCategory = viewModel::renameCategory,
              onDeleteCategory = viewModel::deleteCategory,
            )
          }

          composable(
            route = Routes.BOOK_DETAILS,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
          ) { entry ->
            val bookId = entry.arguments?.getString("bookId") ?: ""
            val book = books.firstOrNull { it.id == bookId }
            if (book == null) {
              LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
              BookDetailsScreen(
                book = book,
                bookmarks = readerBookmarks,
                highlights = readerHighlights,
                notes = readerNotes,
                strings = strings,
                onBack = { navController.popBackStack() },
                onLoadAnnotations = { bId, lang ->
                  viewModel.loadBookAnnotationsAndChapter(bId, lang, 0)
                },
                onOpenReader = { bId, lang, ch, para ->
                  navController.navigate(Routes.reader(bId, lang, ch, para))
                },
                onEditEdition = { bId, lang ->
                  navController.navigate(Routes.editor(bookId = bId, lang = lang))
                },
                onAddMissingEdition = { bId, targetLang ->
                  navController.navigate(Routes.editor(bookId = bId, lang = targetLang))
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onSetStatus = viewModel::setReadingStatus,
                onExportBookZip = { b ->
                  exportingBook = b
                  val safeName = b.title.replace(Regex("[^A-Za-z0-9_-]"), "_").take(32)
                  exportBookLauncher.launch("$safeName-bookwise.zip")
                },
                onDeleteBook = { id ->
                  viewModel.deleteBook(id) { navController.popBackStack() }
                },
              )
            }
          }

          composable(
            route = Routes.READER,
            arguments =
              listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("lang") { type = NavType.StringType; defaultValue = "en" },
                navArgument("ch") { type = NavType.IntType; defaultValue = 0 },
                navArgument("para") { type = NavType.IntType; defaultValue = 0 },
              ),
          ) { entry ->
            val bookId = entry.arguments?.getString("bookId") ?: ""
            val lang = entry.arguments?.getString("lang") ?: "en"
            val ch = entry.arguments?.getInt("ch") ?: 0
            val para = entry.arguments?.getInt("para") ?: 0
            val book = books.firstOrNull { it.id == bookId }
            if (book == null) {
              LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
              ReaderScreen(
                book = book,
                initialLanguageCode = lang,
                initialChapterIndex = ch,
                initialParagraphIndex = para,
                chapterContent = activeChapter,
                bookmarks = readerBookmarks,
                highlights = readerHighlights,
                notes = readerNotes,
                searchHits = searchHits,
                settings = settings,
                ttsState = ttsState,
                strings = strings,
                onLoadChapterAndAnnotations = viewModel::loadBookAnnotationsAndChapter,
                onLoadChapter = viewModel::loadChapterOnly,
                onSaveProgress = viewModel::saveReadingProgress,
                onAddBookmark = viewModel::addBookmark,
                onRemoveBookmark = viewModel::removeBookmark,
                onAddHighlight = viewModel::addHighlight,
                onRemoveHighlight = viewModel::removeHighlight,
                onSaveNote = viewModel::saveNote,
                onDeleteNote = viewModel::deleteNote,
                onSearchInBook = viewModel::searchInBook,
                onClearSearch = viewModel::clearInBookSearch,
                onUpdateSettings = viewModel::updateSettings,
                onStartTtsAtParagraph = { bId, l, cIdx, pIdx ->
                  ensureNotificationPermissionIfNeeded()
                  viewModel.ttsManager.startReadingFromParagraph(bId, l, cIdx, pIdx)
                },
                onToggleTtsPlayPause = { viewModel.ttsManager.togglePlayPause() },
                onTtsPrev = { viewModel.ttsManager.previousParagraph() },
                onTtsNext = { viewModel.ttsManager.nextParagraph() },
                onTtsStop = { viewModel.ttsManager.stop() },
                onTtsCycleSpeed = { cycleTtsSpeed() },
                onTtsToggleMinimize = {
                  viewModel.ttsManager.setMinimized(!ttsState.isMinimized)
                },
                onOpenVoiceSetup = { viewModel.ttsManager.openInstallTtsDataIntent() },
                onBack = { navController.popBackStack() },
              )
            }
          }

          composable(
            route = Routes.EDITOR,
            arguments =
              listOf(
                navArgument("bookId") { type = NavType.StringType; defaultValue = "" },
                navArgument("lang") { type = NavType.StringType; defaultValue = "" },
                navArgument("type") { type = NavType.StringType; defaultValue = "" },
              ),
          ) { entry ->
            val bookIdArg = entry.arguments?.getString("bookId")?.takeIf { it.isNotBlank() }
            val langArg = entry.arguments?.getString("lang")?.takeIf { it.isNotBlank() }
            val typeArg =
              entry.arguments?.getString("type")?.takeIf { it.isNotBlank() }?.let(BookType::fromId)
            BookEditorScreen(
              initialBookId = bookIdArg,
              initialLanguageCode = langArg,
              initialBookType = typeArg,
              categories = categories,
              onLoadDraft = viewModel::loadBookIntoDraft,
              onAutoSaveDraft = viewModel::autoSaveDraft,
              onCopyPickedCover = viewModel::copyPickedCoverToTemp,
              onImportFile = viewModel::importFileToDraft,
              onSaveBook = viewModel::saveBookFromDraft,
              onClearDraft = viewModel::clearDraft,
              onShowMessage = viewModel::showMessage,
              onSavedDone = { saved, otherLang ->
                if (otherLang != null) {
                  navController.navigate(Routes.editor(bookId = saved.id, lang = otherLang)) {
                    popUpTo(Routes.HOME)
                  }
                } else {
                  navController.navigate(Routes.bookDetails(saved.id)) {
                    popUpTo(Routes.HOME)
                  }
                }
              },
              onBack = { navController.popBackStack() },
            )
          }

          composable(Routes.SETTINGS) {
            SettingsScreen(
              settings = settings,
              offlineVoices = offlineVoices,
              storageStats = storageStats,
              pendingBackup = pendingBackup,
              strings = strings,
              onUpdateSettings = viewModel::updateSettings,
              onPreviewVoice = { vName, locale ->
                viewModel.ttsManager.previewVoice(vName, locale)
              },
              onOpenVoiceDataSetup = { viewModel.ttsManager.openInstallTtsDataIntent() },
              onExportLibraryZip = viewModel::exportLibraryZip,
              onExportSettingsJson = viewModel::exportSettingsJson,
              onInspectBackupUri = viewModel::inspectBackupFile,
              onConfirmRestoreBackup = viewModel::confirmRestoreBackup,
              onDismissPendingBackup = viewModel::dismissPendingBackup,
              onOpenImportBook = { navController.navigate(Routes.editor()) },
              onClearCache = viewModel::clearTempCache,
            )
          }
        }

        // Keep Floating TTS player accessible if background audio is running outside ReaderScreen
        if (!currentRoute.startsWith("reader")) {
          FloatingTtsPlayer(
            state = ttsState,
            onTogglePlayPause = { viewModel.ttsManager.togglePlayPause() },
            onPreviousParagraph = { viewModel.ttsManager.previousParagraph() },
            onNextParagraph = { viewModel.ttsManager.nextParagraph() },
            onStop = { viewModel.ttsManager.stop() },
            onCycleSpeed = { cycleTtsSpeed() },
            onToggleMinimize = { viewModel.ttsManager.setMinimized(!ttsState.isMinimized) },
            onOpenSystemVoiceSetup = { viewModel.ttsManager.openInstallTtsDataIntent() },
            modifier =
              Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 72.dp),
          )
        }
      }
    }
  }

  if (settings.navigationStyle == NavigationStyle.HAMBURGER && isTopLevelRoute) {
    ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        ModalDrawerSheet {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = strings.appTitle,
              fontFamily = PlayfairDisplayFamily,
              style = MaterialTheme.typography.headlineSmall,
            )
            Text(
              text = strings.offlineBadge,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
          HorizontalDivider()
          Spacer(modifier = Modifier.height(8.dp))
          topDestinations.forEach { dest ->
            NavigationDrawerItem(
              label = { Text(dest.label) },
              icon = { Icon(dest.icon, contentDescription = null) },
              selected = currentRoute == dest.route,
              onClick = {
                scope.launch { drawerState.close() }
                navigateTopLevel(dest.route)
              },
              modifier = Modifier.padding(horizontal = 12.dp),
            )
          }
          NavigationDrawerItem(
            label = { Text(strings.navSettings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            selected = currentRoute == Routes.SETTINGS,
            onClick = {
              scope.launch { drawerState.close() }
              navigateTopLevel(Routes.SETTINGS)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
          )
        }
      },
      content = scaffoldContent,
    )
  } else {
    scaffoldContent()
  }

  // Multi-book migration dialog (BookWise JSON/TrebEdit imports)
  pendingMigration?.let { migration ->
    var resolution by remember { mutableStateOf(DuplicateResolution.KEEP_BOTH) }
    AlertDialog(
      onDismissRequest = viewModel::dismissMultiBookMigration,
      title = { Text("Migrate BookWise Collection") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Found ${migration.books.size} book(s) in ${migration.sourceLabel}.")
          DuplicateResolution.entries.forEach { opt ->
            TextButton(onClick = { resolution = opt }) {
              Text(
                text =
                  (if (resolution == opt) "• " else "") +
                    when (opt) {
                      DuplicateResolution.SKIP -> "Skip Duplicates"
                      DuplicateResolution.REPLACE -> "Replace Duplicates"
                      DuplicateResolution.KEEP_BOTH -> "Keep Both"
                    }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { viewModel.confirmMultiBookMigration(resolution) }) {
          Text("Migrate Books")
        }
      },
      dismissButton = {
        TextButton(onClick = viewModel::dismissMultiBookMigration) { Text("Cancel") }
      },
    )
  }
}

private fun BookWiseViewModel.inspectBackupUriSafe(uri: android.net.Uri) {
  inspectBackupFile(uri)
}
