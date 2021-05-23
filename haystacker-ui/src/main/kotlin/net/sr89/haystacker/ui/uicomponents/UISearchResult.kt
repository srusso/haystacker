package net.sr89.haystacker.ui.uicomponents

import org.springframework.util.unit.DataSize
import java.time.Instant

data class UISearchResult(
    val filename: String,
    val size: DataSize,
    val created: Instant,
    val lastModified: Instant
)
