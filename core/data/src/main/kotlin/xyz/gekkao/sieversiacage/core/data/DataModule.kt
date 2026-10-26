package xyz.gekkao.sieversiacage.core.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import xyz.gekkao.sieversiacage.core.data.db.AlbumEntryDao
import xyz.gekkao.sieversiacage.core.data.db.SieversiaDatabase
import java.io.File
import java.time.Clock
import javax.inject.Singleton

private const val DATABASE_NAME = "sieversia.db"
private const val ALBUM_DIRECTORY = "album"

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    // ADR-0018: どの端末でも同じ SQLite で動かすため、同梱の SQLite を使う
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): SieversiaDatabase =
        Room
            .databaseBuilder<SieversiaDatabase>(context, DATABASE_NAME)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    @Provides
    fun albumEntryDao(database: SieversiaDatabase): AlbumEntryDao = database.albumEntryDao()

    @Provides
    @Singleton
    fun albumDocumentFiles(
        @ApplicationContext context: Context,
    ): AlbumDocumentFiles = AlbumDocumentFiles(File(context.filesDir, ALBUM_DIRECTORY))

    @Provides
    fun clock(): Clock = Clock.systemUTC()
}

@Module
@InstallIn(SingletonComponent::class)
interface DataBindingModule {
    @Binds
    fun albumDocumentGenerator(generator: InAppAlbumDocumentGenerator): AlbumDocumentGenerator
}
