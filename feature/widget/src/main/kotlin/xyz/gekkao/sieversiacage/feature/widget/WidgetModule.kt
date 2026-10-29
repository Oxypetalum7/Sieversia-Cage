package xyz.gekkao.sieversiacage.feature.widget

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.gekkao.sieversiacage.core.data.WidgetRefresher

@Module
@InstallIn(SingletonComponent::class)
interface WidgetModule {
    @Binds
    fun widgetRefresher(updater: PlantWidgetUpdater): WidgetRefresher
}
