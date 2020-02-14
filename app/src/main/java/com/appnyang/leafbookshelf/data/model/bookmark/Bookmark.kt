package com.appnyang.leafbookshelf.data.model.bookmark

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bookmark Entity.
 *
 * @author Sangwoo <sangwoo@yesang.com> on 2020-02-14.
 */
@Entity(tableName = "bookmarks", indices = [Index(value= ["index"], unique = true)])
data class Bookmark(
    val uri: String,
    val title: String,
    val index: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}