package com.nanzbeatles.innertube.models.response

import com.nanzbeatles.innertube.models.Button
import com.nanzbeatles.innertube.models.Continuation
import com.nanzbeatles.innertube.models.GridRenderer
import com.nanzbeatles.innertube.models.Menu
import com.nanzbeatles.innertube.models.MusicDetailHeaderRenderer
import com.nanzbeatles.innertube.models.MusicEditablePlaylistDetailHeaderRenderer
import com.nanzbeatles.innertube.models.MusicResponsiveHeaderRenderer
import com.nanzbeatles.innertube.models.MusicShelfRenderer
import com.nanzbeatles.innertube.models.ResponseContext
import com.nanzbeatles.innertube.models.Runs
import com.nanzbeatles.innertube.models.SectionListRenderer
import com.nanzbeatles.innertube.models.SubscriptionButton
import com.nanzbeatles.innertube.models.Tabs
import com.nanzbeatles.innertube.models.ThumbnailRenderer
import kotlinx.serialization.Serializable

@Serializable
data class BrowseResponse(
    val contents: Contents? = null,
    val continuationContents: ContinuationContents? = null,
    val onResponseReceivedActions: List<ResponseAction>? = null,
    val header: Header? = null,
    val microformat: Microformat? = null,
    val responseContext: ResponseContext = ResponseContext(null, null),
    val background: ThumbnailRenderer? = null,
    val frameworkUpdates: FrameworkUpdates? = null,
) {
    @Serializable
    data class Contents(
        val singleColumnBrowseResultsRenderer: Tabs? = null,
        val sectionListRenderer: SectionListRenderer? = null,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer? = null,
    )

    @Serializable
    data class TwoColumnBrowseResultsRenderer(
        val tabs: List<Tabs.Tab?>? = null,
        val secondaryContents: SecondaryContents? = null,
    )
    @Serializable
    data class SecondaryContents(
        val sectionListRenderer: SectionListRenderer? = null,
    )

    @Serializable
    data class ContinuationContents(
        val sectionListContinuation: SectionListContinuation? = null,
        val musicPlaylistShelfContinuation: MusicPlaylistShelfContinuation? = null,
        val gridContinuation: GridContinuation? = null,
        val musicShelfContinuation: MusicShelfRenderer? = null,
    ) {
        @Serializable
        data class SectionListContinuation(
            val contents: List<SectionListRenderer.Content> = emptyList(),
            val continuations: List<Continuation>? = null,
        )

        @Serializable
        data class MusicPlaylistShelfContinuation(
            val contents: List<MusicShelfRenderer.Content> = emptyList(),
            val continuations: List<Continuation>? = null,
        )

        @Serializable
        data class GridContinuation(
            val items: List<GridRenderer.Item> = emptyList(),
            val continuations: List<Continuation>? = null,
        )
    }

    @Serializable
    data class ResponseAction(
        val appendContinuationItemsAction: ContinuationItems? = null,
    ) {
        @Serializable
        data class ContinuationItems(
            val continuationItems: List<MusicShelfRenderer.Content>? = null,
        )
    }

    @Serializable
    data class Header(
        val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer? = null,
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer? = null,
        val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer? = null,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer? = null,
        val musicVisualHeaderRenderer: MusicVisualHeaderRenderer? = null,
        val musicHeaderRenderer: MusicHeaderRenderer? = null,
    ) {
        @Serializable
        data class MusicImmersiveHeaderRenderer(
            val title: Runs = Runs(),
            val description: Runs? = null,
            val thumbnail: ThumbnailRenderer? = null,
            val playButton: Button? = null,
            val startRadioButton: Button? = null,
            val subscriptionButton: SubscriptionButton? = null,
            val menu: Menu = Menu(),
            val subscriptionButton2: SubscriptionButton2? = null,
            val monthlyListenerCount: Runs? = null,
        ) {
            @Serializable
            data class SubscriptionButton2(
                val subscribeButtonRenderer: SubscribeButtonRenderer? = null,
            ) {
                @Serializable
                data class SubscribeButtonRenderer(
                    val subscriberCountWithSubscribeText: Runs? = null,
                )
            }
        }

        @Serializable
        data class MusicVisualHeaderRenderer(
            val title: Runs = Runs(),
            val foregroundThumbnail: ThumbnailRenderer = ThumbnailRenderer(null, null, null),
            val thumbnail: ThumbnailRenderer? = null,
        )

        @Serializable
        data class Buttons(
            val menuRenderer: Menu.MenuRenderer? = null,
        )

        @Serializable
        data class MusicHeaderRenderer(
            val buttons: List<Buttons>? = null,
            val title: Runs? = null,
            val thumbnail: MusicThumbnailRenderer? = null,
            val subtitle: Runs? = null,
            val secondSubtitle: Runs? = null,
            val straplineTextOne: Runs? = null,
            val straplineThumbnail: MusicThumbnailRenderer? = null,
        )
        @Serializable
        data class MusicThumbnail(
            val url: String? = null,
        )
        @Serializable
        data class MusicThumbnailRenderer(
            val musicThumbnailRenderer: MusicThumbnailRenderer? = null,
            val thumbnails: List<MusicThumbnail>? = null,
        )
    }

    @Serializable
    data class Microformat(
        val microformatDataRenderer: MicroformatDataRenderer? = null,
    ) {
        @Serializable
        data class MicroformatDataRenderer(
            val urlCanonical: String? = null,
        )
    }

    @Serializable
    data class FrameworkUpdates(
        val entityBatchUpdate: EntityBatchUpdate? = null,
    ) {
        @Serializable
        data class EntityBatchUpdate(
            val mutations: List<Mutation> = emptyList(),
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