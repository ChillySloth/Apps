package com.chillysloth.neomusic

import android.app.Application
import com.chillysloth.neomusic.data.MusicRepository
import com.chillysloth.neomusic.data.PlaylistStore
import com.chillysloth.neomusic.data.Prefs
import com.chillysloth.neomusic.playback.PlayerConnection
import com.chillysloth.neomusic.ui.components.ArtworkLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Application-wide singletons. Small enough that a DI framework would be overkill. */
class NeoMusicApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var repository: MusicRepository private set
    lateinit var playlists: PlaylistStore private set
    lateinit var prefs: Prefs private set
    lateinit var player: PlayerConnection private set
    lateinit var artwork: ArtworkLoader private set

    override fun onCreate() {
        super.onCreate()
        init(this)
    }

    private fun init(context: android.content.Context) {
        repository = MusicRepository(context, appScope)
        playlists = PlaylistStore(context, appScope)
        prefs = Prefs(context)
        player = PlayerConnection(context, appScope)
        artwork = ArtworkLoader(context)
    }

    companion object {
        /** Builds a container on any context, for Compose previews and screenshot tests. */
        fun forPreview(context: android.content.Context): NeoMusicApp = NeoMusicApp().apply { init(context) }
    }
}
