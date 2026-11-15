package com.example.data.storage

import com.example.model.Book
import com.example.model.BookType
import com.example.model.CategoryItem
import com.example.model.EditableChapterDraft
import com.example.model.ReadingStatus

object StarterLibrarySeeder {

  fun defaultCategories(): List<CategoryItem> =
    listOf(
      CategoryItem("cat_philosophy", "Philosophy", listOf("Stoicism", "Ethics", "Wisdom"), false),
      CategoryItem("cat_strategy", "Strategy & Leadership", listOf("Classical", "Decision Making"), false),
      CategoryItem("cat_literature", "Literature & Heritage", listOf("Proverbs", "Classics", "Essays"), false),
      CategoryItem("cat_history", "History & Culture", listOf("Ancient", "Ethiopian Heritage"), false),
      CategoryItem("cat_science", "Science & Mind", listOf("Focus", "Learning"), false),
    )

  data class SeedBookSpec(
    val book: Book,
    val editionChapters: Map<String, List<EditableChapterDraft>>,
  )

  fun starterBooks(now: Long): List<SeedBookSpec> {
    val meditations =
      Book(
        id = "book_meditations_aurelius",
        title = "Meditations",
        subtitle = "Personal Philosophical Reflections",
        author = "Marcus Aurelius",
        category = "Philosophy",
        subcategory = "Stoicism",
        description =
          "Private notebooks written between 170 and 180 AD by Roman Emperor Marcus Aurelius (translated by George Long, Public Domain). A timeless guide to equanimity, duty, and inner clarity.",
        publicationYear = "180 AD",
        bookType = BookType.FULL_BOOK,
        defaultLanguageCode = "en",
        tags = listOf("Stoicism", "Public Domain", "Philosophy"),
        readingStatus = ReadingStatus.READING,
        createdAt = now - 86_400_000L * 4,
        updatedAt = now - 3_600_000L,
        lastReadAt = now - 3_600_000L,
      )
    val meditationsChapters =
      listOf(
        EditableChapterDraft(
          id = "med_ch1",
          title = "Book I — Debts and Lessons",
          bodyText =
            """From my grandfather Verus I learned good morals and the government of my temper.

From the reputation and remembrance of my father, modesty and a manly character.

From my mother, piety and beneficence, and abstinence, not only from evil deeds, but even from evil thoughts; and further, simplicity in my way of living, far removed from the habits of the rich.

From Diognetus, not to busy myself about trifling things, and not to give credit to what was said by miracle-workers and jugglers about incantations and the driving away of daemons and such things; and to endure freedom of speech; and to have become intimate with philosophy.

From Rusticus I received the impression that my character required improvement and discipline; and from him I learned not to be led astray to sophistic emulation, nor to writing on speculative matters, nor to delivering little hortatory orations, nor to showing myself off as a man who practices much discipline, or does benevolent acts in order to make a display.""",
        ),
        EditableChapterDraft(
          id = "med_ch2",
          title = "Book II — On Morning Resolve",
          bodyText =
            """Begin the morning by saying to thyself, I shall meet with the busybody, the ungrateful, arrogant, deceitful, envious, unsocial. All these things happen to them by reason of their ignorance of what is good and evil.

But I who have seen the nature of the good that it is beautiful, and of the bad that it is ugly, and the nature of him who does wrong, that it is akin to me, not only of the same blood or seed, but that it participates in the same intelligence and the same portion of the divinity, I can neither be injured by any of them, for no one can fix on me what is ugly, nor can I be angry with my kinsman, nor hate him.

Thou hast power over thy mind—not outside events. Realize this, and you will find strength.

Do not act as if thou wert going to live ten thousand years. Death hangs over thee. While thou livest, while it is in thy power, be good.""",
        ),
        EditableChapterDraft(
          id = "med_ch3",
          title = "Book IV — The Inner Citadel",
          bodyText =
            """Men seek retreats for themselves, houses in the country, sea-shores, and mountains; and thou too art wont to desire such things very much. But this is altogether a mark of the most common sort of men, for it is in thy power whenever thou shalt choose to retire into thyself.

For nowhere either with more quiet or more freedom from trouble does a man retire than into his own soul, particularly when he has within him such thoughts that by looking into them he is immediately in perfect tranquility; and I affirm that tranquility is nothing else than the good ordering of the mind.

Constantly then give to thyself this retreat, and renew thyself; and let thy principles be brief and fundamental, which, as soon as thou shalt recur to them, will be sufficient to cleanse the soul completely, and to send thee back free from all discontent with the things to which thou returnest.

The universe is transformation: life is opinion.""",
        ),
      )

    val artOfWar =
      Book(
        id = "book_art_of_war_sun_tzu",
        title = "The Art of War",
        subtitle = "Classical Treatise on Strategy",
        author = "Sun Tzu",
        category = "Strategy & Leadership",
        subcategory = "Classical",
        description =
          "Ancient Chinese treatise on strategy, preparation, and clarity under pressure (Lionel Giles translation, Public Domain).",
        publicationYear = "5th Century BC",
        bookType = BookType.FULL_BOOK,
        defaultLanguageCode = "en",
        tags = listOf("Strategy", "Classic", "Public Domain"),
        isFavorite = true,
        readingStatus = ReadingStatus.UNREAD,
        createdAt = now - 86_400_000L * 3,
        updatedAt = now - 86_400_000L * 3,
      )
    val artOfWarChapters =
      listOf(
        EditableChapterDraft(
          id = "aow_ch1",
          title = "Chapter I — Laying Plans",
          bodyText =
            """Sun Tzu said: The art of war is of vital importance to the State. It is a matter of life and death, a road either to safety or to ruin. Hence it is a subject of inquiry which can on no account be neglected.

The art of war, then, is governed by five constant factors, to be taken into account in one's deliberations, when seeking to determine the conditions obtaining in the field. These are: The Moral Law; Heaven; Earth; The Commander; Method and discipline.

All warfare is based on deception. Hence, when we are able to attack, we must seem unable; when using our forces, we must seem inactive; when we are near, we must make the enemy believe we are far away; when far away, we must make him believe we are near.

The general who wins a battle makes many calculations in his temple ere the battle is fought. The general who loses a battle makes but few calculations beforehand.""",
        ),
        EditableChapterDraft(
          id = "aow_ch2",
          title = "Chapter III — Attack by Stratagem",
          bodyText =
            """In the practical art of war, the best thing of all is to take the enemy's country whole and intact; to shatter and destroy it is not so good. So, too, it is better to recapture an army entire than to destroy it.

Hence to fight and conquer in all your battles is not supreme excellence; supreme excellence consists in breaking the enemy's resistance without fighting.

If you know the enemy and know yourself, you need not fear the result of a hundred battles. If you know yourself but not the enemy, for every victory gained you will also suffer a defeat. If you know neither the enemy nor yourself, you will succumb in every battle.""",
        ),
      )

    val bilingualWisdom =
      Book(
        id = "book_ethiopian_proverbs_bilingual",
        title = "Ethiopian Proverbs & Wisdom",
        subtitle = "ምሳሌያዊ አነጋገሮች እና ጥበብ — Bilingual Edition",
        author = "Traditional Heritage Collection",
        category = "Literature & Heritage",
        subcategory = "Proverbs",
        description =
          "A dual-language edition pairing classical Amharic proverbs (ምሳሌያዊ አነጋገሮች) with English translations and reflections. Switch editions anytime in Book Details or inside the Reader.",
        publicationYear = "Heritage Edition",
        bookType = BookType.FULL_BOOK,
        defaultLanguageCode = "am",
        tags = listOf("Amharic", "Bilingual", "Proverbs", "Ethiopic"),
        isFavorite = true,
        readingStatus = ReadingStatus.READING,
        createdAt = now - 86_400_000L * 2,
        updatedAt = now - 7_200_000L,
        lastReadAt = now - 7_200_000L,
      )
    val amharicChapters =
      listOf(
        EditableChapterDraft(
          id = "eth_am_ch1",
          title = "ምዕራፍ ፩ — የትብብርና የትዕግሥት ጥበብ",
          bodyText =
            """ድር ቢያብር አንበሳ ያስር።

አንድ ሰው ብቻውን ትልቅ ሥራ ሊሠራ ይከብዳል፤ ነገር ግን ብዙዎች በአንድ ልብ እና በዓላማ ሲተባበሩ የማይቻል የሚመስለው ሁሉ ይሳካል።

ቀስ በቀስ እንቁላል በእግሩ ይሄዳል።

ትዕግሥት እና ጽናት ከችኮላ ይልቅ ወደ ዘላቂ ስኬት ይመራሉ። ዛሬ ትንሽ የሚመስል ጥረት በየቀኑ ሲቀጥል ትልቅ ፍሬ ያፈራል።

ሳይማሩ ማስተማር፣ ሳያዩ መመስከር አይቻልም።

እውነተኛ ጥበብ የሚጀምረው ከማዳመጥ፣ ከመማር እና ከራስ ልምድ በመነሳት ነው።""",
        ),
        EditableChapterDraft(
          id = "eth_am_ch2",
          title = "ምዕራፍ ፪ — የንግግርና የልብ ልኬት",
          bodyText =
            """የቸኮለ አፍ ሳይለካ ይናገራል።

ከመናገር በፊት ማሰብ የአስተዋይ ሰው ምልክት ነው። ቃል ከወጣ በኋላ መመለስ አይቻልምና በጥንቃቄ እና በማስተዋል መናገር ይገባል።

ሰው በልቡ ያለውን በአንደበቱ ይገልጻል።

ትህትና እና መልካም ንግግር የሰውን ልብ ያቀራርባሉ፤ የጠነከረንም ችግር በሰላም ይፈታሉ።""",
        ),
      )
    val englishWisdomChapters =
      listOf(
        EditableChapterDraft(
          id = "eth_en_ch1",
          title = "Chapter 1 — Wisdom of Unity and Patience",
          bodyText =
            """When spider webs unite, they can tie up a lion.

Alone, a single effort may feel fragile; yet when quiet resolve and shared purpose come together, even formidable obstacles give way.

Slowly, slowly, an egg learns to walk on its feet.

Enduring mastery rarely arrives in a rush. Patient daily practice transforms small beginnings into lasting strength.

One cannot teach without first learning, nor bear witness without first seeing.

Authentic wisdom begins with attentive listening and humble reflection.""",
        ),
        EditableChapterDraft(
          id = "eth_en_ch2",
          title = "Chapter 2 — Measure of Speech and Heart",
          bodyText =
            """A hasty tongue speaks before measuring.

Pause before answering; thoughtful words build trust where rash speech leaves regret.

From the abundance of the heart the voice speaks clearly.

Gentleness and clarity bridge distance between people and soften even difficult seasons.""",
        ),
      )

    val meditationsSummary =
      Book(
        id = "book_meditations_summary",
        title = "Meditations — Core Stoic Notes",
        subtitle = "Concise Reading Summary",
        author = "Marcus Aurelius (Editorial Summary)",
        category = "Philosophy",
        subcategory = "Stoicism",
        description =
          "A structured summary distilling the central habits of mind from Marcus Aurelius's Meditations for quick offline review.",
        publicationYear = "Summary",
        bookType = BookType.SUMMARY,
        defaultLanguageCode = "en",
        tags = listOf("Summary", "Stoicism", "Reflection"),
        readingStatus = ReadingStatus.UNREAD,
        createdAt = now - 86_400_000L,
        updatedAt = now - 86_400_000L,
      )
    val summaryChapters =
      listOf(
        EditableChapterDraft(
          id = "sum_ch1",
          title = "1. Dichotomy of Control",
          bodyText =
            """External events, public praise, and the actions of others sit outside our direct command. Our judgments, intentions, and responses remain wholly ours.

Peace begins the moment we stop demanding that the world conform to our convenience and focus instead on acting with justice and clarity.""",
        ),
        EditableChapterDraft(
          id = "sum_ch2",
          title = "2. Turning Obstacles Into Practice",
          bodyText =
            """An impediment to action advances action; what stands in the way becomes the way.

Every interruption is an invitation to practice patience, restraint, and practical resourcefulness.""",
        ),
      )

    return listOf(
      SeedBookSpec(
        book = meditations,
        editionChapters = mapOf("en" to meditationsChapters),
      ),
      SeedBookSpec(
        book = artOfWar,
        editionChapters = mapOf("en" to artOfWarChapters),
      ),
      SeedBookSpec(
        book = bilingualWisdom,
        editionChapters = mapOf("am" to amharicChapters, "en" to englishWisdomChapters),
      ),
      SeedBookSpec(
        book = meditationsSummary,
        editionChapters = mapOf("en" to summaryChapters),
      ),
    )
  }
}
