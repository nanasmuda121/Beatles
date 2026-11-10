package com.nanzbeatles.innertube.models.response

import com.nanzbeatles.innertube.models.NavigationEndpoint
import com.nanzbeatles.innertube.models.PlaylistPanelRenderer
import com.nanzbeatles.innertube.models.Tabs
import com.nanzbeatles.innertube.models.YouTubeDataPage
import kotlinx.serialization.Serializable

@Serializable
data class NextResponse(
    val contents: Contents = Contents(),
    val continuationContents: ContinuationContents? = null,
    val currentVideoEndpoint: NavigationEndpoint? = null,
    val frameworkUpdates: FrameworkUpdates? = null,
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer? = null,
        val twoColumnWatchNextResults: YouTubeDataPage.Contents.TwoColumnWatchNextResults? = null,
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer? = null,
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer? = null,
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tabs.Tab> = emptyList(),
                )
            }
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer? = null,
    )

    @Serializable
    data class FrameworkUpdates(
        val entityBatchUpdate: EntityBatchUpdate?,
    ) {
        @Serializable
        data class EntityBatchUpdate(
            val mutations: List<Mutation>,
        ) {
            @Serializable
            data class Mutation(
                val entityKey: String?,
                val payload: MutationPayload?,
            ) {
                @Serializable
                data class MutationPayload(
                    val commentEntityPayload: CommentEntityPayload?,
                ) {
                    @Serializable
                    data class CommentEntityPayload(
                        val properties: CommentProperties?,
                    ) {
                        @Serializable
                        data class CommentProperties(
                            val commentId: String?,
                            val content: Content?,
                            val publishedTime: String?,
                        ) {
                            @Serializable
                            data class Content(
                                val content: String?,
                            )
                        }
                    }
                }
            }
        }
    }
}
