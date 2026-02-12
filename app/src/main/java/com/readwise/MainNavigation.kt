package com.readwise

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.readwise.ai.ui.AIConfigScreen
import com.readwise.ai.ui.AIChatScreen
import com.readwise.ai.ui.XRayScreen
import com.readwise.bookshelf.ui.BookshelfScreen
import com.readwise.reader.ui.EpubReaderScreen
import com.readwise.reader.ui.PdfReaderScreen
import com.readwise.reader.ui.TxtReaderScreen
import com.readwise.reader.ui.UnifiedReaderScreen

/**
 * Navigation routes
 */
object Routes {
    const val BOOKSHELF = "bookshelf"
    const val PDF_READER = "pdf_reader/{bookId}"
    const val EPUB_READER = "epub_reader/{bookId}"
    const val TXT_READER = "txt_reader/{bookId}"
    const val UNIFIED_READER = "reader/{bookId}"
    const val X_RAY = "xray/{bookId}"
    const val AI_CHAT = "ai_chat"
    const val AI_CONFIG = "ai_config"

    fun pdfReader(bookId: String) = "pdf_reader/$bookId"
    fun epubReader(bookId: String) = "epub_reader/$bookId"
    fun txtReader(bookId: String) = "txt_reader/$bookId"
    fun unifiedReader(bookId: String) = "reader/$bookId"
    fun xRay(bookId: String) = "xray/$bookId"
}

/**
 * Main NavHost
 */
@Composable
fun MainNavHost(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.BOOKSHELF,
        modifier = modifier
    ) {
        // Bookshelf
        composable(Routes.BOOKSHELF) {
            BookshelfScreen(
                onBookClick = { bookId ->
                    val book = // TODO: Get book and determine format
                    navController.navigate(Routes.pdfReader(bookId))
                }
            )
        }

        // PDF Reader
        composable(
            route = Routes.PDF_READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) {
            PdfReaderScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // EPUB Reader
        composable(
            route = Routes.EPUB_READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) {
            EpubReaderScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // TXT Reader
        composable(
            route = Routes.TXT_READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) {
            TxtReaderScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Unified Reader
        composable(
            route = Routes.UNIFIED_READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) {
            UnifiedReaderScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // X-ray Screen
        composable(
            route = Routes.X_RAY,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) {
            XRayScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // AI Chat Screen
        composable(Routes.AI_CHAT) {
            AIChatScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // AI Configuration Screen
        composable(Routes.AI_CONFIG) {
            AIConfigScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
