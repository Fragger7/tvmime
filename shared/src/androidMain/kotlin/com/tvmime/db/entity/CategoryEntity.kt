package com.tvmime.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tvmime.model.Category
import com.tvmime.model.StreamType

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["portalId", "type"]),
        Index(value = ["portalId", "categoryId"])
    ]
)
data class CategoryEntity(
    @PrimaryKey val id: String, // "${portalId}_${type}_${categoryId}"
    val portalId: String,
    val categoryId: String,
    val categoryName: String,
    val parentId: Int = 0,
    val type: String, // "LIVE", "MOVIE", "SERIES"
    val sortOrder: Int = 0
) {
    fun toDomain(): Category = Category(
        categoryId = categoryId,
        categoryName = categoryName,
        parentId = parentId,
        type = runCatching { StreamType.valueOf(type) }.getOrDefault(StreamType.LIVE)
    )

    companion object {
        fun fromDomain(cat: Category, portalId: String, sortOrder: Int = 0): CategoryEntity = CategoryEntity(
            id = "${portalId}_${cat.type.name}_${cat.categoryId}",
            portalId = portalId,
            categoryId = cat.categoryId,
            categoryName = cat.categoryName,
            parentId = cat.parentId,
            type = cat.type.name,
            sortOrder = sortOrder
        )
    }
}
