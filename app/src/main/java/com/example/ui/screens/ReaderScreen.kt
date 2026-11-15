package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.storage.SearchHit
import com.example.localization.UiStrings
import com.example.model.AppSettings
import com.example.model.Book
import com.example.model.BookNote
import com.example.model.Bookmark
import com.example.model.ChapterContent
import com.example.model.Highlight
import com.example.model.ReaderFont
import com.example.model.ReaderTheme
import com.example.model.TextAlignmentMode
import com.example.tts.TtsPlaybackState
import com.example.ui.components.FloatingTtsPlayer
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LoraFamily
import com.example.ui.theme.NotoSansEthiopicFamily
import com.example.ui.theme.NotoSerifEthiopicFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.readerPaletteFor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(
  book: Book,
  initialLanguageCode: String,
  initialChapterIndex: Int,
  initialParagraphIndex: Int,
  chapterContent: ChapterContent?,
  bookmarks: List<Bookmark>,
  highlights: List<Highlight>,
  notes: List<BookNote>,
  searchHits: List<SearchHit>,
  settings: AppSettings,
  ttsState: TtsPlaybackState,
  strings: UiStrings,
  onLoadChapterAndAnnotations: (bookId: String, lang: String, chapterIndex: Int) -> Unit,
  onLoadChapter: (bookId: String, lang: String, chapterIndex: Int) -> Unit,
  onSaveProgress: (bookId: String, lang: String, chIdx: Int, paraIdx: Int, scrollOffset: Int, totalCh: Int, totalPara: Int) -> Unit,
  onAddBookmark: (bookId: String, lang: String, chIdx: Int, chTitle: String, paraIdx: Int, excerpt: String) -> Unit,
  onRemoveBookmark: (bookId: String, bookmarkId: String) -> Unit,
  onAddHighlight: (bookId: String, lang: String, chIdx: Int, paraIdx: Int, text: String, note: String) -> Unit,
  onRemoveHighlight: (bookId: String, highlightId: String) -> Unit,
  onSaveNote: (bookId: String, lang: String, chIdx: Int, paraIdx: Int, title: String, content: String) -> Unit,
  onDeleteNote: (bookId: String, noteId: String) -> Unit,
  onSearchInBook: (bookId: String, lang: String, query: String) -> Unit,
  onClearSearch: () -> Unit,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onStartTtsAtParagraph: (bookId: String, lang: String, chIdx: Int, paraIdx: Int) -> Unit,
  onToggleTtsPlayPause: () -> Unit,
  onTtsPrev: () -> Unit,
  onTtsNext: () -> Unit,
  onTtsStop: () -> Unit,
  onTtsCycleSpeed: () -> Unit,
  onTtsToggleMinimize: () -> Unit,
  onOpenVoiceSetup: () -> Unit,
  onBack: () -> Unit,
) {
  var activeLang by remember(book.id, initialLanguageCode) {
    mutableStateOf(book.edition(initialLanguageCode)?.languageCode ?: book.defaultLanguageCode)
  }
  val edition = book.edition(activeLang) ?: book.editions.values.firstOrNull()
  val chapters = edition?.chapters.orEmpty()
  var currentChapterIndex by remember(book.id, initialChapterIndex) {
    mutableIntStateOf(initialChapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0)))
  }
  var pendingScrollParagraph by remember { mutableIntStateOf(initialParagraphIndex) }

  var showControls by remember { mutableStateOf(true) }
  var isFullscreen by remember { mutableStateOf(false) }
  var showTocSheet by remember { mutableStateOf(false) }
  var showTypographySheet by remember { mutableStateOf(false) }
  var showSearchSheet by remember { mutableStateOf(false) }
  var actionParagraphPair by remember { mutableStateOf<Pair<Int, String>?>(null) }

  val palette = readerPaletteFor(settings.readerTheme)
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val view = LocalView.current

  // Manage system bars for fullscreen distraction-free reading
  DisposableEffect(isFullscreen) {
    val activity = view.context as? Activity
    val window = activity?.window
    if (window != null) {
      val controller = WindowCompat.getInsetsController(window, view)
      controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      if (isFullscreen) {
        controller.hide(WindowInsetsCompat.Type.systemBars())
      } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
      }
    }
    onDispose {
      val act = view.context as? Activity
      act?.window?.let { w ->
        WindowCompat.getInsetsController(w, view).show(WindowInsetsCompat.Type.systemBars())
      }
    }
  }

  LaunchedEffect(book.id, activeLang) {
    onLoadChapterAndAnnotations(book.id, activeLang, currentChapterIndex)
  }

  LaunchedEffect(currentChapterIndex, activeLang) {
    onLoadChapter(book.id, activeLang, currentChapterIndex)
  }

  // Scroll to target paragraph when chapter loads
  LaunchedEffect(chapterContent?.meta?.id, pendingScrollParagraph) {
    val content = chapterContent ?: return@LaunchedEffect
    if (content.meta.index == currentChapterIndex && pendingScrollParagraph >= 0) {
      val targetItem = (pendingScrollParagraph + 1).coerceIn(0, content.paragraphs.size)
      listState.scrollToItem(targetItem)
      pendingScrollParagraph = -1
    }
  }

  // Sync reader view when TTS advances chapters or paragraphs
  val isTtsActiveForThisBook =
    ttsState.isPlayerVisible && ttsState.bookId == book.id && ttsState.languageCode == activeLang
  LaunchedEffect(ttsState.chapterIndex, ttsState.paragraphIndex, isTtsActiveForThisBook) {
    if (isTtsActiveForThisBook) {
      if (ttsState.chapterIndex != currentChapterIndex) {
        currentChapterIndex = ttsState.chapterIndex
        pendingScrollParagraph = ttsState.paragraphIndex
      } else {
        val itemIdx = (ttsState.paragraphIndex + 1).coerceAtLeast(1)
        listState.animateScrollToItem(itemIdx)
      }
    }
  }

  // Persist reading position smoothly as user reads
  val visibleParaIndex by remember {
    derivedStateOf {
      (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
    }
  }
  LaunchedEffect(visibleParaIndex, currentChapterIndex, chapterContent?.meta?.id) {
    val totalPara = chapterContent?.paragraphs?.size ?: 1
    if (chapterContent != null && chapterContent.meta.index == currentChapterIndex) {
      onSaveProgress(
        book.id,
        activeLang,
        currentChapterIndex,
        visibleParaIndex,
        listState.firstVisibleItemScrollOffset,
        chapters.size,
        totalPara,
      )
    }
  }

  BackHandler {
    when {
      actionParagraphPair != null -> actionParagraphPair = null
      showTypographySheet -> showTypographySheet = false
      showTocSheet -> showTocSheet = false
      showSearchSheet -> showSearchSheet = false
      isFullscreen -> {
        isFullscreen = false
        showControls = true
      }
      else -> onBack()
    }
  }

  val resolvedFontFamily: FontFamily =
    remember(settings.readerFont, activeLang) {
      if (activeLang == "am" &&
        settings.readerFont != ReaderFont.ETHIOPIC_SANS &&
        settings.readerFont != ReaderFont.ETHIOPIC_SERIF
      ) {
        NotoSerifEthiopicFamily
      } else {
        when (settings.readerFont) {
          ReaderFont.LORA_SERIF -> LoraFamily
          ReaderFont.PLAYFAIR_CLASSIC -> PlayfairDisplayFamily
          ReaderFont.INTER_SANS -> InterFamily
          ReaderFont.ETHIOPIC_SERIF -> NotoSerifEthiopicFamily
          ReaderFont.ETHIOPIC_SANS -> NotoSansEthiopicFamily
        }
      }
    }

  val chapterParagraphs = chapterContent?.paragraphs.orEmpty()
  val currentChapterMeta = chapters.getOrNull(currentChapterIndex)
  val isCurrentPositionBookmarked =
    bookmarks.any {
      it.languageCode == activeLang &&
        it.chapterIndex == currentChapterIndex &&
        it.paragraphIndex == visibleParaIndex
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.background)
        .testTag("reader_screen"),
  ) {
    // Core distraction-free vertical chapter reader
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        state = listState,
        modifier =
          Modifier.widthIn(max = 760.dp)
            .fillMaxSize()
            .pointerInput(Unit) {
              detectTapGestures(
                onTap = { showControls = !showControls }
              )
            },
        contentPadding =
          PaddingValues(
            start = settings.readerMarginDp.dp,
            end = settings.readerMarginDp.dp,
            top = if (showControls && !isFullscreen) 84.dp else 44.dp,
            bottom = 132.dp,
          ),
      ) {
        item(key = "chapter_header_${currentChapterMeta?.id ?: currentChapterIndex}") {
          Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
              text = book.title.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              letterSpacing = 1.4.sp,
              color = palette.secondaryText,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = currentChapterMeta?.title ?: "Chapter ${currentChapterIndex + 1}",
              fontFamily =
                if (activeLang == "am") NotoSerifEthiopicFamily else PlayfairDisplayFamily,
              fontSize = (settings.readerFontSizeSp + 8f).sp,
              lineHeight = ((settings.readerFontSizeSp + 8f) * 1.28f).sp,
              color = palette.text,
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = palette.divider, thickness = 0.75.dp)
          }
        }

        itemsIndexed(
          items = chapterParagraphs,
          key = { idx, _ -> "${currentChapterMeta?.id}_p_$idx" },
        ) { pIdx, paragraph ->
          val isTtsSpeakingThis =
            isTtsActiveForThisBook &&
              ttsState.chapterIndex == currentChapterIndex &&
              ttsState.paragraphIndex == pIdx
          val hasHighlight =
            highlights.any {
              it.languageCode == activeLang &&
                it.chapterIndex == currentChapterIndex &&
                it.paragraphIndex == pIdx
            }
          val hasBookmark =
            bookmarks.any {
              it.languageCode == activeLang &&
                it.chapterIndex == currentChapterIndex &&
                it.paragraphIndex == pIdx
            }
          val hasNote =
            notes.any {
              it.languageCode == activeLang &&
                it.chapterIndex == currentChapterIndex &&
                it.paragraphIndex == pIdx
            }

          val bgColor =
            when {
              isTtsSpeakingThis -> palette.highlightParagraph
              hasHighlight -> palette.userHighlightYellow.copy(alpha = 0.55f)
              else -> Color.Transparent
            }

          Column(
            modifier =
              Modifier.fillMaxWidth()
                .padding(bottom = settings.readerParagraphSpacingDp.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .pointerInput(currentChapterIndex, pIdx, settings.ttsDoubleTapEnabled) {
                  detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = {
                      if (settings.ttsDoubleTapEnabled && paragraph.isNotBlank()) {
                        onStartTtsAtParagraph(book.id, activeLang, currentChapterIndex, pIdx)
                      }
                    },
                    onLongPress = {
                      if (paragraph.isNotBlank()) {
                        actionParagraphPair = pIdx to paragraph
                      }
                    },
                  )
                }
                .padding(
                  horizontal = if (bgColor != Color.Transparent) 10.dp else 0.dp,
                  vertical = if (bgColor != Color.Transparent) 6.dp else 0.dp,
                )
                .testTag("reader_paragraph_$pIdx"),
          ) {
            if (hasBookmark || hasNote || isTtsSpeakingThis) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp),
              ) {
                if (isTtsSpeakingThis) {
                  Text(
                    text = "Reading aloud",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
                if (hasBookmark) {
                  Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Bookmarked",
                    tint = palette.accent,
                    modifier = Modifier.size(14.dp),
                  )
                }
                if (hasNote) {
                  Icon(
                    Icons.AutoMirrored.Filled.NoteAdd,
                    contentDescription = "Has note",
                    tint = palette.accent,
                    modifier = Modifier.size(14.dp),
                  )
                }
              }
            }

            Text(
              text = paragraph,
              fontFamily = resolvedFontFamily,
              fontSize = settings.readerFontSizeSp.sp,
              lineHeight = (settings.readerFontSizeSp * settings.readerLineHeightMultiplier).sp,
              textAlign =
                if (settings.readerTextAlignment == TextAlignmentMode.JUSTIFIED) {
                  TextAlign.Justify
                } else {
                  TextAlign.Start
                },
              color = palette.text,
            )
          }
        }

        // End of chapter navigation
        item(key = "chapter_footer") {
          Spacer(modifier = Modifier.height(24.dp))
          HorizontalDivider(color = palette.divider, thickness = 0.75.dp)
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            OutlinedButton(
              onClick = {
                if (currentChapterIndex > 0) {
                  currentChapterIndex--
                  pendingScrollParagraph = 0
                }
              },
              enabled = currentChapterIndex > 0,
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.Filled.ChevronLeft, contentDescription = null)
              Text("Previous")
            }
            Button(
              onClick = {
                if (currentChapterIndex + 1 < chapters.size) {
                  currentChapterIndex++
                  pendingScrollParagraph = 0
                }
              },
              enabled = currentChapterIndex + 1 < chapters.size,
              modifier = Modifier.height(48.dp),
            ) {
              Text("Next Chapter")
              Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
          }
        }
      }
    }

    // Minimal Top Reader Toolbar
    AnimatedVisibility(
      visible = showControls,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter),
    ) {
      Surface(
        color = palette.background.copy(alpha = 0.96f),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier.size(48.dp).testTag("reader_back_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = palette.text,
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = currentChapterMeta?.title ?: book.title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${currentChapterIndex + 1} / ${chapters.size.coerceAtLeast(1)} • ${activeLang.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText,
              )
            }
            IconButton(
              onClick = {
                val existing =
                  bookmarks.firstOrNull {
                    it.languageCode == activeLang &&
                      it.chapterIndex == currentChapterIndex &&
                      it.paragraphIndex == visibleParaIndex
                  }
                if (existing != null) {
                  onRemoveBookmark(book.id, existing.id)
                } else {
                  val excerpt = chapterParagraphs.getOrElse(visibleParaIndex) { book.title }
                  onAddBookmark(
                    book.id,
                    activeLang,
                    currentChapterIndex,
                    currentChapterMeta?.title ?: "Chapter ${currentChapterIndex + 1}",
                    visibleParaIndex,
                    excerpt,
                  )
                }
              },
              modifier = Modifier.size(48.dp).testTag("reader_bookmark_button"),
            ) {
              Icon(
                imageVector =
                  if (isCurrentPositionBookmarked) Icons.Filled.Bookmark
                  else Icons.Filled.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (isCurrentPositionBookmarked) palette.accent else palette.text,
              )
            }
            IconButton(
              onClick = { showSearchSheet = true },
              modifier = Modifier.size(48.dp).testTag("reader_search_button"),
            ) {
              Icon(Icons.Filled.Search, contentDescription = "Search book", tint = palette.text)
            }
            IconButton(
              onClick = { showTypographySheet = true },
              modifier = Modifier.size(48.dp).testTag("reader_typography_button"),
            ) {
              Icon(
                Icons.Filled.FormatSize,
                contentDescription = "Typography & themes",
                tint = palette.text,
              )
            }
            IconButton(
              onClick = { showTocSheet = true },
              modifier = Modifier.size(48.dp).testTag("reader_toc_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = "Contents & annotations",
                tint = palette.text,
              )
            }
          }
          HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
        }
      }
    }

    // Minimal Bottom Progress & Chapter Bar
    AnimatedVisibility(
      visible = showControls && !ttsState.isPlayerVisible,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.BottomCenter),
    ) {
      Surface(
        color = palette.background.copy(alpha = 0.96f),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
          val chProgress =
            if (chapterParagraphs.isNotEmpty()) {
              ((visibleParaIndex + 1).toFloat() / chapterParagraphs.size.toFloat()).coerceIn(0f, 1f)
            } else 0f
          val overall =
            if (chapters.isNotEmpty()) {
              ((currentChapterIndex + chProgress) / chapters.size.toFloat()).coerceIn(0f, 1f)
            } else 0f

          LinearProgressIndicator(
            progress = { overall },
            color = palette.accent,
            trackColor = palette.divider,
            modifier = Modifier.fillMaxWidth().height(3.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Ch ${(chProgress * 100).toInt()}% • Book ${(overall * 100).toInt()}%",
              style = MaterialTheme.typography.labelSmall,
              color = palette.secondaryText,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  onStartTtsAtParagraph(book.id, activeLang, currentChapterIndex, visibleParaIndex)
                },
                modifier = Modifier.size(44.dp).testTag("reader_tts_start_button"),
              ) {
                Icon(
                  Icons.Filled.RecordVoiceOver,
                  contentDescription = "Listen offline (TTS)",
                  tint = palette.text,
                )
              }
              IconButton(
                onClick = {
                  isFullscreen = !isFullscreen
                  if (isFullscreen) showControls = false
                },
                modifier = Modifier.size(44.dp),
              ) {
                Icon(
                  imageVector =
                    if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                  contentDescription = "Toggle fullscreen",
                  tint = palette.text,
                )
              }
            }
          }
        }
      }
    }

    // Floating Offline TTS Player (completely hidden during normal reading)
    FloatingTtsPlayer(
      state = ttsState,
      onTogglePlayPause = onToggleTtsPlayPause,
      onPreviousParagraph = onTtsPrev,
      onNextParagraph = onTtsNext,
      onStop = onTtsStop,
      onCycleSpeed = onTtsCycleSpeed,
      onToggleMinimize = onTtsToggleMinimize,
      onOpenSystemVoiceSetup = onOpenVoiceSetup,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 16.dp),
    )
  }

  // Paragraph Action Sheet (Highlight, Bookmark, Note, Select/Copy Text, Listen)
  actionParagraphPair?.let { (pIdx, pText) ->
    var noteTitle by remember(pIdx) { mutableStateOf("") }
    var noteContent by remember(pIdx) { mutableStateOf("") }
    var showNoteInput by remember(pIdx) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = { actionParagraphPair = null }) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Paragraph ${pIdx + 1}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Long-press text below to copy words directly:",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth(),
        ) {
          SelectionContainer {
            Text(
              text = pText,
              fontFamily = resolvedFontFamily,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
              maxLines = 6,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = {
              onStartTtsAtParagraph(book.id, activeLang, currentChapterIndex, pIdx)
              actionParagraphPair = null
            }
          ) {
            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Read Aloud")
          }
          OutlinedButton(
            onClick = {
              onAddHighlight(book.id, activeLang, currentChapterIndex, pIdx, pText, "")
              actionParagraphPair = null
            }
          ) {
            Icon(Icons.Filled.Highlight, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Highlight")
          }
          OutlinedButton(
            onClick = {
              onAddBookmark(
                book.id,
                activeLang,
                currentChapterIndex,
                currentChapterMeta?.title ?: "Chapter ${currentChapterIndex + 1}",
                pIdx,
                pText,
              )
              actionParagraphPair = null
            }
          ) {
            Icon(Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bookmark")
          }
          OutlinedButton(onClick = { showNoteInput = !showNoteInput }) {
            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Note")
          }
        }

        if (showNoteInput) {
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = noteTitle,
            onValueChange = { noteTitle = it },
            label = { Text("Note Title (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = noteContent,
            onValueChange = { noteContent = it },
            label = { Text("Your reflection or note") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = {
              onSaveNote(book.id, activeLang, currentChapterIndex, pIdx, noteTitle, noteContent)
              actionParagraphPair = null
            },
            modifier = Modifier.align(Alignment.End),
          ) {
            Text("Save Note")
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Independent Reader Typography & Theme Sheet
  if (showTypographySheet) {
    ModalBottomSheet(onDismissRequest = { showTypographySheet = false }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("Reader Appearance", style = MaterialTheme.typography.titleLarge)

        Text("Reader Theme", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ReaderTheme.entries.forEach { rt ->
            FilterChip(
              selected = settings.readerTheme == rt,
              onClick = { onUpdateSettings { it.copy(readerTheme = rt) } },
              label = { Text(rt.labelEn) },
            )
          }
        }

        if (book.editions.size > 1) {
          Text("Edition Language", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            book.editions.keys.forEach { code ->
              FilterChip(
                selected = activeLang == code,
                onClick = {
                  activeLang = code
                  val savedProg = book.edition(code)?.progress
                  currentChapterIndex = savedProg?.chapterIndex ?: 0
                  pendingScrollParagraph = savedProg?.paragraphIndex ?: 0
                },
                label = { Text(if (code == "am") "አማርኛ" else "English") },
              )
            }
          }
        }

        Text("Font Family", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ReaderFont.entries.forEach { rf ->
            FilterChip(
              selected = settings.readerFont == rf,
              onClick = { onUpdateSettings { it.copy(readerFont = rf) } },
              label = { Text(rf.labelEn) },
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Size (${settings.readerFontSizeSp.toInt()}sp)", modifier = Modifier.width(96.dp))
          Slider(
            value = settings.readerFontSizeSp,
            onValueChange = { v -> onUpdateSettings { it.copy(readerFontSizeSp = v) } },
            valueRange = 14f..28f,
            modifier = Modifier.weight(1f),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "Line (${String.format("%.1f", settings.readerLineHeightMultiplier)}x)",
            modifier = Modifier.width(96.dp),
          )
          Slider(
            value = settings.readerLineHeightMultiplier,
            onValueChange = { v -> onUpdateSettings { it.copy(readerLineHeightMultiplier = v) } },
            valueRange = 1.3f..2.1f,
            modifier = Modifier.weight(1f),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Para (${settings.readerParagraphSpacingDp.toInt()}dp)", modifier = Modifier.width(96.dp))
          Slider(
            value = settings.readerParagraphSpacingDp,
            onValueChange = { v -> onUpdateSettings { it.copy(readerParagraphSpacingDp = v) } },
            valueRange = 6f..28f,
            modifier = Modifier.weight(1f),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Margin (${settings.readerMarginDp.toInt()}dp)", modifier = Modifier.width(96.dp))
          Slider(
            value = settings.readerMarginDp,
            onValueChange = { v -> onUpdateSettings { it.copy(readerMarginDp = v) } },
            valueRange = 10f..40f,
            modifier = Modifier.weight(1f),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          FilterChip(
            selected = settings.readerTextAlignment == TextAlignmentMode.LEFT,
            onClick = {
              onUpdateSettings { it.copy(readerTextAlignment = TextAlignmentMode.LEFT) }
            },
            label = { Text("Left") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null) },
          )
          FilterChip(
            selected = settings.readerTextAlignment == TextAlignmentMode.JUSTIFIED,
            onClick = {
              onUpdateSettings { it.copy(readerTextAlignment = TextAlignmentMode.JUSTIFIED) }
            },
            label = { Text("Justified") },
            leadingIcon = { Icon(Icons.Filled.FormatAlignJustify, contentDescription = null) },
          )
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }

  // Table of Contents & Bookmarks/Highlights/Notes Sheet
  if (showTocSheet) {
    var tocTab by remember { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = { showTocSheet = false }) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        TabRow(selectedTabIndex = tocTab) {
          Tab(selected = tocTab == 0, onClick = { tocTab = 0 }, text = { Text("Chapters") })
          Tab(selected = tocTab == 1, onClick = { tocTab = 1 }, text = { Text("Bookmarks") })
          Tab(selected = tocTab == 2, onClick = { tocTab = 2 }, text = { Text("Highlights") })
          Tab(selected = tocTab == 3, onClick = { tocTab = 3 }, text = { Text("Notes") })
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
          when (tocTab) {
            0 -> {
              items(chapters, key = { it.id }) { ch ->
                val active = ch.index == currentChapterIndex
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable {
                        currentChapterIndex = ch.index
                        pendingScrollParagraph = 0
                        showTocSheet = false
                      }
                      .padding(vertical = 12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = ch.title,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color =
                      if (active) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                  )
                  Text(
                    text = "${ch.estimatedMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                HorizontalDivider(thickness = 0.5.dp)
              }
            }
            1 -> {
              val list = bookmarks.filter { it.languageCode == activeLang }
              items(list, key = { it.id }) { bm ->
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable {
                        currentChapterIndex = bm.chapterIndex
                        pendingScrollParagraph = bm.paragraphIndex
                        showTocSheet = false
                      }
                      .padding(vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(bm.chapterTitle, style = MaterialTheme.typography.labelMedium)
                    Text(bm.excerpt, maxLines = 2, overflow = TextOverflow.Ellipsis)
                  }
                  IconButton(onClick = { onRemoveBookmark(book.id, bm.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove bookmark")
                  }
                }
                HorizontalDivider(thickness = 0.5.dp)
              }
            }
            2 -> {
              val list = highlights.filter { it.languageCode == activeLang }
              items(list, key = { it.id }) { hl ->
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable {
                        currentChapterIndex = hl.chapterIndex
                        pendingScrollParagraph = hl.paragraphIndex
                        showTocSheet = false
                      }
                      .padding(vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      "Chapter ${hl.chapterIndex + 1} • Para ${hl.paragraphIndex + 1}",
                      style = MaterialTheme.typography.labelMedium,
                    )
                    Text(hl.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                  }
                  IconButton(onClick = { onRemoveHighlight(book.id, hl.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove highlight")
                  }
                }
                HorizontalDivider(thickness = 0.5.dp)
              }
            }
            3 -> {
              val list = notes.filter { it.languageCode == activeLang }
              items(list, key = { it.id }) { note ->
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable {
                        currentChapterIndex = note.chapterIndex
                        pendingScrollParagraph = note.paragraphIndex
                        showTocSheet = false
                      }
                      .padding(vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(note.title, style = MaterialTheme.typography.labelMedium)
                    Text(note.content, maxLines = 3, overflow = TextOverflow.Ellipsis)
                  }
                  IconButton(onClick = { onDeleteNote(book.id, note.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Delete note")
                  }
                }
                HorizontalDivider(thickness = 0.5.dp)
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Search Within Book Sheet
  if (showSearchSheet) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(
      onDismissRequest = {
        showSearchSheet = false
        onClearSearch()
      }
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("Search Inside Book", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = query,
            onValueChange = {
              query = it
              onSearchInBook(book.id, activeLang, it)
            },
            placeholder = { Text("Find word or phrase…") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
          items(searchHits) { hit ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier =
                Modifier.fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clickable {
                    currentChapterIndex = hit.chapterIndex
                    pendingScrollParagraph = hit.paragraphIndex
                    showSearchSheet = false
                    onClearSearch()
                  },
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "${hit.chapterTitle} • Para ${hit.paragraphIndex + 1}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(hit.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 3)
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}
