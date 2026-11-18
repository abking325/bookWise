package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.model.SectionRole
import com.example.model.TextAlignmentMode
import com.example.tts.OfflineTtsManager
import com.example.tts.TtsPlaybackState
import com.example.ui.components.FloatingTtsPlayer
import com.example.ui.theme.BodoniModaFamily
import com.example.ui.theme.LiterataFamily
import com.example.ui.theme.LoraFamily
import com.example.ui.theme.ManropeFamily
import com.example.ui.theme.NotoSansEthiopicFamily
import com.example.ui.theme.NotoSerifEthiopicFamily
import com.example.ui.theme.readerPaletteFor
import java.util.Locale

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
  onSaveProgress: (
    bookId: String,
    lang: String,
    chIdx: Int,
    chapterId: String,
    paraIdx: Int,
    scrollOffset: Int,
    totalCh: Int,
    totalPara: Int,
  ) -> Unit,
  onResetProgress: (bookId: String, lang: String) -> Unit,
  onAddBookmark: (bookId: String, lang: String, chIdx: Int, chTitle: String, paraIdx: Int, excerpt: String) -> Unit,
  onRemoveBookmark: (bookId: String, bookmarkId: String) -> Unit,
  onAddHighlight: (bookId: String, lang: String, chIdx: Int, paraIdx: Int, text: String, note: String) -> Unit,
  onRemoveHighlight: (bookId: String, highlightId: String) -> Unit,
  onSaveNote: (bookId: String, lang: String, chIdx: Int, paraIdx: Int, title: String, content: String) -> Unit,
  onDeleteNote: (bookId: String, noteId: String) -> Unit,
  onSearchInBook: (bookId: String, lang: String, query: String) -> Unit,
  onClearSearch: () -> Unit,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onStartTtsAtParagraph: (bookId: String, lang: String, chIdx: Int, paraIdx: Int, startSentenceIndex: Int) -> Unit,
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
  val numberedChapterMap = remember(chapters) { buildBodyChapterNumbers(chapters) }

  var currentChapterIndex by remember(book.id, initialChapterIndex) {
    mutableIntStateOf(initialChapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0)))
  }
  var pendingScrollParagraph by remember(book.id, initialLanguageCode) {
    mutableIntStateOf(initialParagraphIndex)
  }
  var pendingScrollOffset by remember(book.id, initialLanguageCode) {
    val saved = edition?.progress
    val matchesSaved =
      saved != null &&
        saved.chapterIndex == initialChapterIndex &&
        saved.paragraphIndex == initialParagraphIndex
    mutableIntStateOf(if (matchesSaved) saved!!.scrollOffset else 0)
  }
  var isInitialRestoreComplete by remember(book.id, activeLang) { mutableStateOf(false) }

  var showControls by remember { mutableStateOf(true) }
  var isFullscreen by remember { mutableStateOf(false) }
  var showTocSheet by remember { mutableStateOf(false) }
  var showTypographySheet by remember { mutableStateOf(false) }
  var showSearchSheet by remember { mutableStateOf(false) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var actionParagraphPair by remember { mutableStateOf<Pair<Int, String>?>(null) }

  val palette = readerPaletteFor(settings.readerTheme)
  val listState = rememberLazyListState()
  val view = LocalView.current
  val lifecycleOwner = LocalLifecycleOwner.current

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

  LaunchedEffect(chapterContent?.meta?.id, pendingScrollParagraph) {
    val content = chapterContent ?: return@LaunchedEffect
    if (content.meta.index == currentChapterIndex) {
      if (pendingScrollParagraph >= 0) {
        val targetItem =
          if (pendingScrollParagraph == 0 && pendingScrollOffset == 0) 0
          else (pendingScrollParagraph + 1).coerceIn(0, content.paragraphs.size)
        listState.scrollToItem(targetItem, pendingScrollOffset.coerceAtLeast(0))
        pendingScrollParagraph = -1
        pendingScrollOffset = 0
      }
      isInitialRestoreComplete = true
    }
  }

  val isTtsActiveForThisBook =
    ttsState.isPlayerVisible && ttsState.bookId == book.id && ttsState.languageCode == activeLang
  LaunchedEffect(ttsState.chapterIndex, ttsState.paragraphIndex, isTtsActiveForThisBook) {
    if (isTtsActiveForThisBook) {
      if (ttsState.chapterIndex != currentChapterIndex) {
        currentChapterIndex = ttsState.chapterIndex
        pendingScrollParagraph = ttsState.paragraphIndex
        pendingScrollOffset = 0
      } else {
        val itemIdx = (ttsState.paragraphIndex + 1).coerceAtLeast(1)
        listState.animateScrollToItem(itemIdx)
      }
    }
  }

  val visibleParaIndex by remember {
    derivedStateOf {
      (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
    }
  }
  LaunchedEffect(visibleParaIndex, currentChapterIndex, chapterContent?.meta?.id, isInitialRestoreComplete) {
    val content = chapterContent
    if (isInitialRestoreComplete &&
      content != null &&
      content.meta.index == currentChapterIndex &&
      pendingScrollParagraph < 0
    ) {
      onSaveProgress(
        book.id,
        activeLang,
        currentChapterIndex,
        content.meta.id,
        visibleParaIndex,
        listState.firstVisibleItemScrollOffset,
        chapters.size,
        content.paragraphs.size.coerceAtLeast(1),
      )
    }
  }

  val latestChapterIndex by rememberUpdatedState(currentChapterIndex)
  val latestParaIndex by rememberUpdatedState(visibleParaIndex)
  val latestContent by rememberUpdatedState(chapterContent)
  val latestRestoreComplete by rememberUpdatedState(isInitialRestoreComplete)
  val latestChaptersCount by rememberUpdatedState(chapters.size)

  fun flushCurrentProgress() {
    val content = latestContent ?: return
    if (!latestRestoreComplete || content.meta.index != latestChapterIndex) return
    onSaveProgress(
      book.id,
      activeLang,
      latestChapterIndex,
      content.meta.id,
      latestParaIndex,
      listState.firstVisibleItemScrollOffset,
      latestChaptersCount,
      content.paragraphs.size.coerceAtLeast(1),
    )
  }

  DisposableEffect(lifecycleOwner, book.id, activeLang) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
        flushCurrentProgress()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      flushCurrentProgress()
      lifecycleOwner.lifecycle.removeObserver(observer)
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
      else -> {
        flushCurrentProgress()
        onBack()
      }
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
          ReaderFont.LITERATA_SERIF -> LiterataFamily
          ReaderFont.BODONI_MODA -> BodoniModaFamily
          ReaderFont.MANROPE_SANS -> ManropeFamily
          ReaderFont.LORA_SERIF -> LoraFamily
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

  fun toggleCurrentBookmark() {
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
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.background)
        .testTag("reader_screen"),
  ) {
    // Calm Typeset Printed-Page Reader Canvas
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        state = listState,
        modifier =
          Modifier.widthIn(max = 700.dp)
            .fillMaxSize()
            .pointerInput(Unit) {
              detectTapGestures(onTap = { showControls = !showControls })
            },
        contentPadding =
          PaddingValues(
            start = settings.readerMarginDp.dp,
            end = settings.readerMarginDp.dp,
            top = if (showControls && !isFullscreen) 92.dp else 48.dp,
            bottom = 148.dp,
          ),
      ) {
        item(key = "chapter_header_${currentChapterMeta?.id ?: currentChapterIndex}") {
          val sectionEyebrow =
            if (currentChapterMeta != null && currentChapterMeta.role != SectionRole.CHAPTER) {
              currentChapterMeta.role.labelEn.uppercase()
            } else {
              val bodyNum = currentChapterMeta?.let { numberedChapterMap[it.id] } ?: (currentChapterIndex + 1)
              "CHAPTER ${bodyNum.toString().padStart(2, '0')}"
            }
          val headingSize = (settings.readerFontSizeSp + 14f).coerceIn(28f, 38f)

          Column(modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp)) {
            Text(
              text = sectionEyebrow,
              fontFamily = ManropeFamily,
              fontSize = 11.sp,
              letterSpacing = 1.6.sp,
              color = palette.accent,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = currentChapterMeta?.title ?: "Chapter ${currentChapterIndex + 1}",
              fontFamily =
                if (activeLang == "am") NotoSerifEthiopicFamily else BodoniModaFamily,
              fontSize = headingSize.sp,
              lineHeight = (headingSize * 1.18f).sp,
              fontWeight = FontWeight.Normal,
              color = palette.text,
            )
            Spacer(modifier = Modifier.height(18.dp))
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
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .pointerInput(currentChapterIndex, pIdx, settings.ttsDoubleTapEnabled) {
                  detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = {
                      if (settings.ttsDoubleTapEnabled && paragraph.isNotBlank()) {
                        onStartTtsAtParagraph(book.id, activeLang, currentChapterIndex, pIdx, 0)
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

            val displayAnnotated =
              remember(
                paragraph,
                isTtsSpeakingThis,
                settings.ttsHighlightSentence,
                ttsState.activeCharStart,
                ttsState.activeCharEnd,
                palette.highlightSentence,
              ) {
                if (isTtsSpeakingThis &&
                  settings.ttsHighlightSentence &&
                  ttsState.activeCharStart >= 0 &&
                  ttsState.activeCharEnd > ttsState.activeCharStart
                ) {
                  val sStart = ttsState.activeCharStart.coerceIn(0, paragraph.length)
                  val sEnd = ttsState.activeCharEnd.coerceIn(sStart, paragraph.length)
                  buildAnnotatedString {
                    append(paragraph)
                    if (sEnd > sStart) {
                      addStyle(
                        style =
                          SpanStyle(
                            background = palette.highlightSentence,
                            fontWeight = FontWeight.Medium,
                          ),
                        start = sStart,
                        end = sEnd,
                      )
                    }
                  }
                } else {
                  null
                }
              }

            if (displayAnnotated != null) {
              Text(
                text = displayAnnotated,
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
            } else {
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
        }

        // Clean End-of-Chapter Flow Controls
        item(key = "chapter_footer") {
          Spacer(modifier = Modifier.height(28.dp))
          HorizontalDivider(color = palette.divider, thickness = 0.75.dp)
          Spacer(modifier = Modifier.height(18.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = {
                if (currentChapterIndex > 0) {
                  currentChapterIndex--
                  pendingScrollParagraph = 0
                  pendingScrollOffset = 0
                }
              },
              enabled = currentChapterIndex > 0,
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.Filled.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Previous Chapter")
            }
            Button(
              onClick = {
                if (currentChapterIndex + 1 < chapters.size) {
                  currentChapterIndex++
                  pendingScrollParagraph = 0
                  pendingScrollOffset = 0
                }
              },
              enabled = currentChapterIndex + 1 < chapters.size,
              shape = RoundedCornerShape(50),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = palette.text,
                  contentColor = palette.background,
                ),
              elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
              modifier = Modifier.height(48.dp),
            ) {
              Text("Next Chapter")
              Spacer(modifier = Modifier.width(4.dp))
              Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }

    // Slim Minimal Editorial Top Reader Toolbar (Back, Book Title, TOC, Typography Aa, Overflow)
    AnimatedVisibility(
      visible = showControls && !isFullscreen,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().background(palette.background.copy(alpha = 0.97f)),
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 6.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = {
              flushCurrentProgress()
              onBack()
            },
            modifier = Modifier.size(48.dp).testTag("reader_back_button"),
          ) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = palette.text,
            )
          }

          Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
              text = book.title,
              fontFamily = LiterataFamily,
              fontSize = 14.sp,
              color = palette.text,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = currentChapterMeta?.title ?: "${currentChapterIndex + 1} / ${chapters.size.coerceAtLeast(1)}",
              fontFamily = ManropeFamily,
              fontSize = 11.sp,
              color = palette.secondaryText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          IconButton(
            onClick = { showTocSheet = true },
            modifier = Modifier.size(48.dp).testTag("reader_toc_button"),
          ) {
            Icon(
              Icons.AutoMirrored.Filled.List,
              contentDescription = "Table of Contents",
              tint = palette.text,
            )
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

          Box {
            IconButton(
              onClick = { showOverflowMenu = true },
              modifier = Modifier.size(48.dp).testTag("reader_overflow_button"),
            ) {
              Icon(
                Icons.Filled.MoreVert,
                contentDescription = "Reading options",
                tint = palette.text,
              )
            }
            DropdownMenu(
              expanded = showOverflowMenu,
              onDismissRequest = { showOverflowMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text(if (isCurrentPositionBookmarked) "Remove Bookmark" else "Bookmark Page") },
                onClick = {
                  showOverflowMenu = false
                  toggleCurrentBookmark()
                },
              )
              DropdownMenuItem(
                text = { Text("Search Inside Book") },
                onClick = {
                  showOverflowMenu = false
                  showSearchSheet = true
                },
              )
              DropdownMenuItem(
                text = { Text("Listen Offline (TTS)") },
                onClick = {
                  showOverflowMenu = false
                  onStartTtsAtParagraph(book.id, activeLang, currentChapterIndex, visibleParaIndex, 0)
                },
              )
              DropdownMenuItem(
                text = { Text("Fullscreen Reading") },
                onClick = {
                  showOverflowMenu = false
                  isFullscreen = true
                  showControls = false
                },
              )
              DropdownMenuItem(
                text = { Text("Restart Book") },
                onClick = {
                  showOverflowMenu = false
                  onResetProgress(book.id, activeLang)
                  currentChapterIndex = 0
                  pendingScrollParagraph = 0
                  pendingScrollOffset = 0
                },
              )
            }
          }
        }
        HorizontalDivider(color = palette.divider, thickness = 0.75.dp)
      }
    }

    // Subtle Calm Bottom Reading Bar (progress + quick Listen + Prev/Next chapter)
    AnimatedVisibility(
      visible = showControls && !isFullscreen && !ttsState.isPlayerVisible,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.BottomCenter),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().background(palette.background.copy(alpha = 0.97f)),
      ) {
        HorizontalDivider(color = palette.divider, thickness = 0.75.dp)
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
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
            modifier = Modifier.fillMaxWidth().height(2.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Ch ${(chProgress * 100).toInt()}% · Book ${(overall * 100).toInt()}%",
              fontFamily = ManropeFamily,
              fontSize = 11.sp,
              color = palette.secondaryText,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              TextButton(
                onClick = {
                  if (currentChapterIndex > 0) {
                    currentChapterIndex--
                    pendingScrollParagraph = 0
                    pendingScrollOffset = 0
                  }
                },
                enabled = currentChapterIndex > 0,
                contentPadding = PaddingValues(horizontal = 8.dp),
              ) {
                Text("Prev", fontSize = 12.sp, color = if (currentChapterIndex > 0) palette.text else palette.secondaryText.copy(alpha = 0.4f))
              }
              TextButton(
                onClick = {
                  if (currentChapterIndex + 1 < chapters.size) {
                    currentChapterIndex++
                    pendingScrollParagraph = 0
                    pendingScrollOffset = 0
                  }
                },
                enabled = currentChapterIndex + 1 < chapters.size,
                contentPadding = PaddingValues(horizontal = 8.dp),
              ) {
                Text("Next", fontSize = 12.sp, color = if (currentChapterIndex + 1 < chapters.size) palette.text else palette.secondaryText.copy(alpha = 0.4f))
              }
              IconButton(
                onClick = {
                  onStartTtsAtParagraph(book.id, activeLang, currentChapterIndex, visibleParaIndex, 0)
                },
                modifier = Modifier.size(40.dp).testTag("reader_tts_start_button"),
              ) {
                Icon(
                  Icons.Filled.RecordVoiceOver,
                  contentDescription = "Listen offline (TTS)",
                  tint = palette.text,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }

    // Floating Offline TTS Player (remains completely hidden until triggered)
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

  // Paragraph Action Sheet (Listen From Here / Pick Sentence, Highlight, Bookmark, Note, Copy)
  actionParagraphPair?.let { (pIdx, pText) ->
    var noteTitle by remember(pIdx) { mutableStateOf("") }
    var noteContent by remember(pIdx) { mutableStateOf("") }
    var showNoteInput by remember(pIdx) { mutableStateOf(false) }
    var showSentencePicker by remember(pIdx) { mutableStateOf(false) }
    val sentences = remember(pText) { OfflineTtsManager.splitParagraphIntoSentenceSpans(pText) }

    ModalBottomSheet(onDismissRequest = { actionParagraphPair = null }) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Paragraph ${pIdx + 1}", fontFamily = BodoniModaFamily, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Long-press text below to copy words directly:",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
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
              onStartTtsAtParagraph(book.id, activeLang, currentChapterIndex, pIdx, 0)
              actionParagraphPair = null
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier.testTag("paragraph_listen_here_button"),
          ) {
            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Listen From Here")
          }
          if (sentences.size > 1) {
            OutlinedButton(
              onClick = { showSentencePicker = !showSentencePicker },
              shape = RoundedCornerShape(50),
            ) {
              Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Pick Sentence (${sentences.size})")
            }
          }
          OutlinedButton(
            onClick = {
              onAddHighlight(book.id, activeLang, currentChapterIndex, pIdx, pText, "")
              actionParagraphPair = null
            },
            shape = RoundedCornerShape(50),
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
            },
            shape = RoundedCornerShape(50),
          ) {
            Icon(Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bookmark")
          }
          OutlinedButton(
            onClick = { showNoteInput = !showNoteInput },
            shape = RoundedCornerShape(50),
          ) {
            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Note")
          }
        }

        if (showSentencePicker && sentences.size > 1) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Tap a sentence to start listening from it:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            sentences.take(8).forEach { span ->
              Surface(
                onClick = {
                  onStartTtsAtParagraph(book.id, activeLang, currentChapterIndex, pIdx, span.index)
                  actionParagraphPair = null
                },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "${span.index + 1}. ${span.text}",
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
            }
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

  // Reader Typography & Theme Sheet (Independent Reader Surfaces & Editorial Fonts)
  if (showTypographySheet) {
    ModalBottomSheet(onDismissRequest = { showTypographySheet = false }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = "Typography & Paper",
          fontFamily = BodoniModaFamily,
          fontSize = 22.sp,
          fontWeight = FontWeight.Normal,
        )

        Text(
          text = "PAPER & THEME",
          fontFamily = ManropeFamily,
          fontSize = 10.sp,
          letterSpacing = 1.2.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ReaderTheme.entries.forEach { rt ->
            val rtPalette = readerPaletteFor(rt)
            val selected = settings.readerTheme == rt
            Surface(
              onClick = { onUpdateSettings { it.copy(readerTheme = rt) } },
              shape = RoundedCornerShape(50),
              color = rtPalette.background,
              border =
                BorderStroke(
                  width = if (selected) 2.dp else 0.75.dp,
                  color =
                    if (selected) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier = Modifier.height(38.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(12.dp)
                      .clip(CircleShape)
                      .background(rtPalette.text)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = rt.labelEn,
                  fontSize = 12.sp,
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                  color = rtPalette.text,
                )
              }
            }
          }
        }

        if (book.editions.size > 1) {
          Text(
            text = "EDITION LANGUAGE",
            fontFamily = ManropeFamily,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            book.editions.keys.forEach { code ->
              ReaderChoiceChip(
                label = if (code == "am") "አማርኛ" else "English",
                selected = activeLang == code,
                onClick = {
                  flushCurrentProgress()
                  activeLang = code
                  val savedProg = book.edition(code)?.progress
                  currentChapterIndex = savedProg?.chapterIndex ?: 0
                  pendingScrollParagraph = savedProg?.paragraphIndex ?: 0
                  pendingScrollOffset = savedProg?.scrollOffset ?: 0
                },
              )
            }
          }
        }

        Text(
          text = "TYPEFACE",
          fontFamily = ManropeFamily,
          fontSize = 10.sp,
          letterSpacing = 1.2.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ReaderFont.entries.forEach { rf ->
            ReaderChoiceChip(
              label = rf.labelEn,
              selected = settings.readerFont == rf,
              onClick = { onUpdateSettings { it.copy(readerFont = rf) } },
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Size (${settings.readerFontSizeSp.toInt()}sp)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(104.dp))
          Slider(
            value = settings.readerFontSizeSp,
            onValueChange = { v -> onUpdateSettings { it.copy(readerFontSizeSp = v) } },
            valueRange = 14f..28f,
            modifier = Modifier.weight(1f),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "Line (${String.format(Locale.US, "%.2f", settings.readerLineHeightMultiplier)}x)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(104.dp),
          )
          Slider(
            value = settings.readerLineHeightMultiplier,
            onValueChange = { v -> onUpdateSettings { it.copy(readerLineHeightMultiplier = v) } },
            valueRange = 1.3f..2.1f,
            modifier = Modifier.weight(1f),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Para (${settings.readerParagraphSpacingDp.toInt()}dp)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(104.dp))
          Slider(
            value = settings.readerParagraphSpacingDp,
            onValueChange = { v -> onUpdateSettings { it.copy(readerParagraphSpacingDp = v) } },
            valueRange = 6f..28f,
            modifier = Modifier.weight(1f),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Margin (${settings.readerMarginDp.toInt()}dp)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(104.dp))
          Slider(
            value = settings.readerMarginDp,
            onValueChange = { v -> onUpdateSettings { it.copy(readerMarginDp = v) } },
            valueRange = 14f..40f,
            modifier = Modifier.weight(1f),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ReaderChoiceChip(
            label = "Left Aligned",
            selected = settings.readerTextAlignment == TextAlignmentMode.LEFT,
            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
            onClick = { onUpdateSettings { it.copy(readerTextAlignment = TextAlignmentMode.LEFT) } },
          )
          ReaderChoiceChip(
            label = "Justified",
            selected = settings.readerTextAlignment == TextAlignmentMode.JUSTIFIED,
            icon = Icons.Filled.FormatAlignJustify,
            onClick = { onUpdateSettings { it.copy(readerTextAlignment = TextAlignmentMode.JUSTIFIED) } },
          )
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }

  // Traditional Book-Index Table of Contents & Annotations Sheet
  if (showTocSheet) {
    var tocTab by remember { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = { showTocSheet = false }) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.tableOfContents,
            fontFamily = BodoniModaFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal,
          )
          TextButton(
            onClick = {
              onResetProgress(book.id, activeLang)
              currentChapterIndex = 0
              pendingScrollParagraph = 0
              pendingScrollOffset = 0
              showTocSheet = false
            },
            modifier = Modifier.testTag("reader_restart_button"),
          ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Restart Book")
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
          listOf("Chapters", "Bookmarks", "Highlights", "Notes").forEachIndexed { idx, label ->
            val selected = tocTab == idx
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.clickable { tocTab = idx }.padding(top = 6.dp),
            ) {
              Text(
                text = label,
                fontFamily = ManropeFamily,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color =
                  if (selected) MaterialTheme.colorScheme.onBackground
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Box(
                modifier =
                  Modifier.width(32.dp)
                    .height(2.dp)
                    .background(
                      if (selected) MaterialTheme.colorScheme.onBackground
                      else Color.Transparent
                    )
              )
            }
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)

        LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
          when (tocTab) {
            0 -> {
              items(chapters, key = { it.id }) { ch ->
                val active = ch.index == currentChapterIndex
                val bodyNum = numberedChapterMap[ch.id]
                val displayTitle =
                  if (bodyNum != null) "${bodyNum.toString().padStart(2, '0')} — ${ch.title}"
                  else ch.title

                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable {
                        currentChapterIndex = ch.index
                        pendingScrollParagraph = 0
                        pendingScrollOffset = 0
                        showTocSheet = false
                      }
                      .padding(vertical = 14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    if (ch.role != SectionRole.CHAPTER) {
                      Text(
                        text = ch.role.labelEn.uppercase(),
                        fontFamily = ManropeFamily,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.0.sp,
                        color = MaterialTheme.colorScheme.secondary,
                      )
                    }
                    Text(
                      text = displayTitle,
                      fontFamily = LiterataFamily,
                      fontSize = 15.sp,
                      fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                      color =
                        if (active) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                  }
                  Text(
                    text = "${ch.estimatedMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
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
                        pendingScrollOffset = 0
                        showTocSheet = false
                      }
                      .padding(vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(bm.chapterTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(bm.excerpt, maxLines = 2, overflow = TextOverflow.Ellipsis)
                  }
                  IconButton(onClick = { onRemoveBookmark(book.id, bm.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove bookmark")
                  }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
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
                        pendingScrollOffset = 0
                        showTocSheet = false
                      }
                      .padding(vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      "Chapter ${hl.chapterIndex + 1} · Para ${hl.paragraphIndex + 1}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(hl.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                  }
                  IconButton(onClick = { onRemoveHighlight(book.id, hl.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove highlight")
                  }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
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
                        pendingScrollOffset = 0
                        showTocSheet = false
                      }
                      .padding(vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(note.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(note.content, maxLines = 3, overflow = TextOverflow.Ellipsis)
                  }
                  IconButton(onClick = { onDeleteNote(book.id, note.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Delete note")
                  }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
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
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Text("Search Inside Book", fontFamily = BodoniModaFamily, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = query,
          onValueChange = {
            query = it
            onSearchInBook(book.id, activeLang, it)
          },
          placeholder = { Text("Find word or phrase…") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
          items(searchHits) { hit ->
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    currentChapterIndex = hit.chapterIndex
                    pendingScrollParagraph = hit.paragraphIndex
                    pendingScrollOffset = 0
                    showSearchSheet = false
                    onClearSearch()
                  }
                  .padding(vertical = 12.dp),
            ) {
              Text(
                text = "${hit.chapterTitle} · Para ${hit.paragraphIndex + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(hit.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.75.dp)
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun ReaderChoiceChip(
  label: String,
  selected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(50),
    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
    border =
      BorderStroke(
        width = 0.75.dp,
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.height(36.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 14.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = label,
        fontFamily = ManropeFamily,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
