package com.tvmime.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tvmime.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE portalId = :portalId AND type = :type ORDER BY sortOrder ASC, categoryName ASC")
    fun getCategories(portalId: String, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE portalId = :portalId AND type = :type ORDER BY sortOrder ASC, categoryName ASC")
    suspend fun getCategoriesSync(portalId: String, type: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE portalId = :portalId AND type = :type")
    suspend fun deleteCategoriesForPortal(portalId: String, type: String)

    @Query("DELETE FROM categories WHERE portalId = :portalId")
    suspend fun deleteAllForPortal(portalId: String)
}
