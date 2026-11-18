package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.BodoniModaFamily
import com.example.ui.theme.ManropeFamily
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
  val isEditor: Boolean = false,
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

  val sectionNavDestinations =
    listOf(
      TopLevelDestination(Routes.HOME, strings.navHome, Icons.Filled.Home, "nav_home"),
      TopLevelDestination(
        Routes.FULL_BOOKS,
        strings.navFullBooks,
        Icons.AutoMirrored.Filled.MenuBook,
        "nav_full_books",
      ),
      TopLevelDestination(
        Routes.SUMMARIES,
        strings.navSummaries,
        Icons.Filled.Summarize,
        "nav_summaries",
      ),
      TopLevelDestination(
        Routes.CATEGORIES,
        strings.navCategories,
        Icons.Filled.Category,
        "nav_categories",
      ),
    )

  val allTopDestinations =
    sectionNavDestinations +
      TopLevelDestination(
        Routes.EDITOR,
        strings.navAddBook,
        Icons.Filled.AddCircleOutline,
        "nav_add_book",
        isEditor = true,
      )

  val isTopLevelRoute =
    currentRoute == Routes.HOME ||
      currentRoute == Routes.FULL_BOOKS ||
      currentRoute == Routes.SUMMARIES ||
      currentRoute == Routes.CATEGORIES ||
      currentRoute == Routes.SETTINGS

  fun navigateDestination(dest: TopLevelDestination) {
    if (dest.isEditor) {
      navController.navigate(Routes.editor()) { launchSingleTop = true }
    } else {
      navController.navigate(dest.route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }
  }

  fun navigateTopLevel(route: String) {
    navController.navigate(route) {
      popUpTo(Routes.HOME) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun openCategoryLibrary(catName: String) {
    val hasFull = books.any { it.bookType == BookType.FULL_BOOK && it.category.equals(catName, ignoreCase = true) }
    val hasSummary = books.any { it.bookType == BookType.SUMMARY && it.category.equals(catName, ignoreCase = true) }
    if (!hasFull && hasSummary) {
      summariesFilter = LibraryFilter(category = catName, sortOption = summariesFilter.sortOption)
      navigateTopLevel(Routes.SUMMARIES)
    } else {
      fullBooksFilter = LibraryFilter(category = catName, sortOption = fullBooksFilter.sortOption)
      navigateTopLevel(Routes.FULL_BOOKS)
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

  fun triggerExportZip(book: Book) {
    exportingBook = book
    val safeName = book.title.replace(Regex("[^A-Za-z0-9_-]"), "_").take(32)
    exportBookLauncher.launch("$safeName-bookwise.zip")
  }

  val scaffoldContent: @Composable () -> Unit = {
    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        if (isTopLevelRoute) {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
          ) {
            // Section A: Single compact editorial header (wordmark + subtle subtitle; theme & settings on right)
            Row(
              modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                if (settings.navigationStyle == NavigationStyle.HAMBURGER) {
                  IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.size(48.dp).testTag("hamburger_menu_button"),
                  ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                  }
                }
                Column {
                  Text(
                    text = strings.appTitle,
                    fontFamily = BodoniModaFamily,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.8).sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                  )
                  Text(
                    text = strings.editorialSubtitle,
                    fontFamily = ManropeFamily,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { viewModel.toggleLightDarkQuick() },
                  modifier = Modifier.size(48.dp).testTag("quick_theme_toggle"),
                ) {
                  Icon(
                    imageVector =
                      if (settings.themeMode == AppThemeMode.DARK) Icons.Outlined.LightMode
                      else Icons.Outlined.DarkMode,
                    contentDescription = "Toggle light or dark mode",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(21.dp),
                  )
                }
                IconButton(
                  onClick = { navigateTopLevel(Routes.SETTINGS) },
                  modifier = Modifier.size(48.dp).testTag("nav_settings_button"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = strings.navSettings,
                    tint =
                      if (currentRoute == Routes.SETTINGS) MaterialTheme.colorScheme.secondary
                      else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(21.dp),
                  )
                }
              }
            }

            // Section B: Clean horizontal underline navigation bar
            if (settings.navigationStyle == NavigationStyle.TOP_BAR) {
              BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val minWidth = maxWidth
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .horizontalScroll(rememberScrollState())
                      .widthIn(min = minWidth)
                      .padding(horizontal = 20.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    sectionNavDestinations.forEach { dest ->
                      val selected = currentRoute == dest.route
                      Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier =
                          Modifier.defaultMinSize(minHeight = 48.dp)
                            .clickable { navigateDestination(dest) }
                            .padding(top = 12.dp)
                            .testTag(dest.testTag),
                      ) {
                        Text(
                          text = dest.label,
                          fontFamily = ManropeFamily,
                          fontSize = 12.sp,
                          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                          color =
                            if (selected) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          softWrap = false,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                          modifier =
                            Modifier.width(24.dp)
                              .height(2.dp)
                              .background(
                                if (selected) MaterialTheme.colorScheme.onBackground
                                else Color.Transparent
                              )
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.width(14.dp))

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                      Modifier.defaultMinSize(minHeight = 48.dp)
                        .clickable { navController.navigate(Routes.editor()) { launchSingleTop = true } }
                        .padding(horizontal = 4.dp)
                        .testTag("nav_add_book"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Add,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = strings.navAddBook,
                      fontFamily = ManropeFamily,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onBackground,
                      maxLines = 1,
                      softWrap = false,
                    )
                  }
                }
              }
              HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.75.dp,
              )
            } else {
              HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.75.dp,
              )
            }
          }
        }
      },
      bottomBar = {
        if (isTopLevelRoute) {
          when (settings.navigationStyle) {
            NavigationStyle.FIXED_BOTTOM -> {
              NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
              ) {
                allTopDestinations.forEach { dest ->
                  NavigationBarItem(
                    selected = currentRoute == dest.route,
                    onClick = { navigateDestination(dest) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = {
                      Text(
                        text = dest.label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                      )
                    },
                    modifier = Modifier.testTag(dest.testTag),
                  )
                }
              }
            }
            NavigationStyle.FLOATING_BOTTOM -> {
              Box(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
              ) {
                Surface(
                  shape = RoundedCornerShape(28.dp),
                  tonalElevation = 2.dp,
                  shadowElevation = 8.dp,
                  color = MaterialTheme.colorScheme.background,
                  border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    allTopDestinations.forEach { dest ->
                      val selected = currentRoute == dest.route
                      Surface(
                        onClick = { navigateDestination(dest) },
                        shape = RoundedCornerShape(22.dp),
                        color =
                          if (selected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.background,
                        modifier = Modifier.height(48.dp).testTag(dest.testTag),
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.padding(horizontal = if (selected) 12.dp else 10.dp),
                        ) {
                          Icon(
                            dest.icon,
                            contentDescription = dest.label,
                            tint =
                              if (selected) MaterialTheme.colorScheme.onPrimary
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                          )
                          if (selected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                              text = dest.label,
                              style = MaterialTheme.typography.labelMedium,
                              color = MaterialTheme.colorScheme.onPrimary,
                              maxLines = 1,
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
              onOpenCategory = { catName -> openCategoryLibrary(catName) },
              onViewAllBooks = { navigateTopLevel(Routes.FULL_BOOKS) },
              onViewAllCategories = { navigateTopLevel(Routes.CATEGORIES) },
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
              onEditBook = { b -> navController.navigate(Routes.editor(bookId = b.id, lang = b.defaultLanguageCode)) },
              onToggleFavorite = viewModel::toggleFavorite,
              onExportBookZip = { b -> triggerExportZip(b) },
              onDeleteBook = { b -> viewModel.deleteBook(b.id) },
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
              onEditBook = { b -> navController.navigate(Routes.editor(bookId = b.id, lang = b.defaultLanguageCode)) },
              onToggleFavorite = viewModel::toggleFavorite,
              onExportBookZip = { b -> triggerExportZip(b) },
              onDeleteBook = { b -> viewModel.deleteBook(b.id) },
              onAddBook = { type -> navController.navigate(Routes.editor(type = type)) },
            )
          }

          composable(Routes.CATEGORIES) {
            CategoriesScreen(
              categories = categories,
              books = books,
              strings = strings,
              onSelectCategory = { catName -> openCategoryLibrary(catName) },
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
                onResetProgress = viewModel::resetReadingProgress,
                onStartAudio = { bId, lang, ch, para, sent ->
                  ensureNotificationPermissionIfNeeded()
                  viewModel.ttsManager.startReadingFromParagraph(bId, lang, ch, para, sent)
                },
                onEditEdition = { bId, lang ->
                  navController.navigate(Routes.editor(bookId = bId, lang = lang))
                },
                onAddMissingEdition = { bId, targetLang ->
                  navController.navigate(Routes.editor(bookId = bId, lang = targetLang))
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onSetStatus = viewModel::setReadingStatus,
                onExportBookZip = { b -> triggerExportZip(b) },
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
                onResetProgress = viewModel::resetReadingProgress,
                onAddBookmark = viewModel::addBookmark,
                onRemoveBookmark = viewModel::removeBookmark,
                onAddHighlight = viewModel::addHighlight,
                onRemoveHighlight = viewModel::removeHighlight,
                onSaveNote = viewModel::saveNote,
                onDeleteNote = viewModel::deleteNote,
                onSearchInBook = viewModel::searchInBook,
                onClearSearch = viewModel::clearInBookSearch,
                onUpdateSettings = viewModel::updateSettings,
                onStartTtsAtParagraph = { bId, l, cIdx, pIdx, sIdx ->
                  ensureNotificationPermissionIfNeeded()
                  viewModel.ttsManager.startReadingFromParagraph(bId, l, cIdx, pIdx, sIdx)
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

        // Floating TTS player accessible if background audio is running outside ReaderScreen
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
              fontFamily = BodoniModaFamily,
              style = MaterialTheme.typography.headlineSmall,
            )
            Text(
              text = strings.editorialSubtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          HorizontalDivider()
          Spacer(modifier = Modifier.height(8.dp))
          allTopDestinations.forEach { dest ->
            NavigationDrawerItem(
              label = { Text(dest.label) },
              icon = { Icon(dest.icon, contentDescription = null) },
              selected = currentRoute == dest.route,
              onClick = {
                scope.launch { drawerState.close() }
                navigateDestination(dest)
              },
              modifier = Modifier.padding(horizontal = 12.dp),
            )
          }
          NavigationDrawerItem(
            label = { Text(strings.navSettings) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
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
