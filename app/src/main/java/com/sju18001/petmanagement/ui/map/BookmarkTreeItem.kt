package com.sju18001.petmanagement.ui.map

import com.sju18001.petmanagement.restapi.dao.Bookmark

data class BookmarkTreeItem(
    val isBookmark: Boolean,
    var bookmark: Bookmark?,
    var folder: String?,
    var isOpened: Boolean // for folder
)
