package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** 植えた株の情報（ADR-0020）。MVP は 1 株だけなので、行は id = [THE_PLANT_ID] の 1 つだけ。 */
@Entity(tableName = "plant")
data class PlantEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "planted_on")
    val plantedOn: String,
)

const val THE_PLANT_ID = 1
