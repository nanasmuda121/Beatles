package com.nanzbeatles.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Continuation(
    @JsonNames("nextRadioContinuationData", "reloadContinuationData")
    val nextContinuationData: NextContinuationData? = null,
) {
    @Serializable
    data class NextContinuationData(
        val continuation: String = "",
    )
}

fun List<Continuation>.getContinuation() =
    firstNotNullOfOrNull { it.nextContinuationData?.continuation?.takeIf { token -> token.isNotBlank() } }