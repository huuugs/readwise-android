# EPUB Reading Engine Implementation Report

## Completed EPUB Module

### Core Engine Layer

#### 1. EpubEngine.kt
**File**: `app/src/main/java/com/readwise/engine/epub/EpubEngine.kt`

EPUB engine interface defining:
- `openDocument(path)` - Open EPUB document
- `getChapter(index)` - Get chapter content
- `getResource(href)` - Get resources (images, fonts)
- `getCover()` - Get cover bitmap
- `getTableOfContents()` - Get table of contents
- `search(query)` - Full-text search

**Data Models**:
- `EpubDocument` - EPUB document interface
- `EpubChapter` - Chapter content with HTML and plain text
- `EpubResource` - Binary resources (images, CSS)
- `EpubTocItem` - Table of contents item
- `EpubSearchResult` - Search result
- `EpubDocumentInfo` - Document metadata
- `EpubLayoutConfig` - Rendering configuration
- `EpubTextAlign` - Text alignment enum

#### 2. EpubEngineImpl.kt
**File**: `app/src/main/java/com/readwise/engine/epub/EpubEngineImpl.kt`

Implementation based on Jsoup:
- Parses OPF file for metadata
- Extracts chapters from spine
- Loads HTML content
- Finds cover image
- Generates table of contents

**Key Features**:
- OPF metadata parsing
- Chapter list extraction
- HTML content loading with Jsoup
- Cover image extraction
- Plain text extraction for search

### View Model Layer

#### 3. EpubReaderViewModel.kt
**File**: `app/src/main/java/com/readwise/reader/viewmodel/EpubReaderViewModel.kt`

**Responsibilities**:
- Reading state management
- Chapter navigation
- Progress saving
- Layout configuration
- TOC display

**State**:
```kotlin
data class EpubReaderUiState(
    val isLoading: Boolean
    val book: Book?
    val currentChapterIndex: Int
    val chapterCount: Int?
    val showToc: Boolean
    val showSettings: Boolean
    val error: String?
)
```

**Functions**:
- `goToChapter(index)` - Jump to chapter
- `nextChapter() / previousChapter()` - Chapter navigation
- `jumpToToc(item)` - TOC navigation
- `updateLayoutConfig(config)` - Update rendering settings
- `setFontSize(size)` - Adjust font size
- `setLineHeight(height)` - Adjust line height
- `setTextAlign(align)` - Set text alignment

### UI Layer

#### 4. EpubReaderScreen.kt
**File**: `app/src/main/java/com/readwise/reader/ui/EpubReaderScreen.kt`

**Components**:
- `EpubReaderScreen` - Main reader container
- `ChapterContent` - Chapter content display
- `Paragraph` - Paragraph with configurable styling
- `ReaderTopBar` - Title, chapter name, buttons
- `ReaderBottomBar` - Chapter navigation
- `TableOfContentsSidebar` - TOC drawer
- `ReaderSettingsPanel` - Settings dialog
- `TocItem` - TOC item
- `LoadingIndicator` / `ErrorView` - State views

**Features**:
- Responsive chapter rendering
- Configurable typography (size, line height, alignment)
- Smooth scrolling with LazyColumn
- TOC navigation
- Chapter-by-chapter reading
- Auto-save reading progress
- Material 3 design

### Dependency Injection

#### 5. EngineModule.kt (Updated)
**File**: `app/src/main/java/com/readwise/core/di/EngineModule.kt`

Added EpubEngine binding:
```kotlin
@Binds
@Singleton
abstract fun bindEpubEngine(impl: EpubEngineImpl): EpubEngine
```

#### 6. MainNavigation.kt (Updated)
**File**: `app/src/main/java/com/readwise/MainNavigation.kt`

Added EPUB reader route:
```kotlin
const val EPUB_READER = "epub_reader/{bookId}"

fun epubReader(bookId: String) = "epub_reader/$bookId"
```

---

## File Structure

```
app/src/main/java/com/readwise/
├── engine/
│   └── epub/
│       ├── EpubEngine.kt           (Interface + models)
│       └── EpubEngineImpl.kt       (Jsoup implementation)
├── reader/
│   ├── viewmodel/
│   │   └── EpubReaderViewModel.kt
│   └── ui/
│       └── EpubReaderScreen.kt
└── core/
    └── di/
        └── EngineModule.kt         (Updated with EPUB)
```

---

## Implementation Details

### EPUB Parsing Flow
```kotlin
1. Open Document
   Find OPF file in META-INF/container.xml

2. Parse Metadata
   Extract title, author, description from OPF

3. Extract Chapters
   Parse spine items from OPF
   Create chapter list with href and index

4. Load Content
   Read HTML file
   Parse with Jsoup
   Extract plain text for search
```

### Reading Flow
```kotlin
1. Load Book
   Get book from repository

2. Open Document
   Call epubEngine.openDocument(path)

3. Get Chapter
   Call epubEngine.getChapter(index)

4. Render Content
   Display HTML as styled text
   Apply layout config
```

### Layout Configuration
```kotlin
EpubLayoutConfig(
    fontSize: Int = 18,           // sp
    lineHeight: Float = 1.6f,      // multiplier
    paragraphSpacing: Int = 16,     // dp
    marginHorizontal: Int = 16,     // dp
    marginVertical: Int = 16,       // dp
    textColor: Int = 0xFF000000,
    backgroundColor: Int = 0xFFFFFFFF,
    fontFamily: String? = null,
    textAlign: EpubTextAlign.JUSTIFY,
    columnCount: Int = 1,
    verticalScroll: Boolean = false
)
```

---

## Features Implemented

### Completed
- [x] EPUB document opening
- [x] Chapter parsing and loading
- [x] Table of contents extraction
- [x] Cover image extraction
- [x] Chapter navigation
- [x] Reading progress saving
- [x] Configurable typography
- [x] TOC sidebar navigation
- [x] Settings panel
- [x] Responsive content rendering

### Known Limitations
- [ ] Actual EPUB parsing (uses Jsoup, should use Readium)
- [ ] Image loading in content
- [ ] CSS styling support
- [ ] Full-text search implementation
- [ ] External font loading
- [ ] Multi-column layout
- [ ] Vertical scroll mode

---

## Next Steps

### Short Term
1. **Integrate Readium Toolkit**
   - Replace Jsoup implementation
   - Proper EPUB 2.0/3.0 support
   - Better resource handling

2. **Enhance Rendering**
   - CSS style application
   - Image loading and display
   - Better HTML to text conversion

3. **Reading Features**
   - Bookmark integration
   - Highlight support
   - Annotation support

### Long Term
1. **Advanced Features**
   - Text-to-speech
   - Dictionary lookup
   - Translation
   - Night mode
   - Custom fonts

2. **Performance**
   - Chapter preloading
   - Content caching
   - Lazy rendering optimization

---

## Build Instructions

Ensure build.gradle includes EPUB dependencies:
```gradle
implementation 'org.readium.kotlin-toolkit:readium-shared:2.4.0'
implementation 'org.readium.kotlin-toolkit:readium-streamer:2.4.0'
implementation 'org.jsoup:jsoup:1.16.1'
```

---

## Testing Strategy

```kotlin
// Unit Tests
class EpubEngineTest {
    @Test
    fun `should parse epub metadata`() { }
    @Test
    fun `should extract chapters`() { }
    @Test
    fun `should load chapter content`() { }
}

// UI Tests
class EpubReaderScreenTest {
    @Test
    fun `should display chapter content`() { }
    @Test
    fun `should navigate chapters`() { }
}
```
