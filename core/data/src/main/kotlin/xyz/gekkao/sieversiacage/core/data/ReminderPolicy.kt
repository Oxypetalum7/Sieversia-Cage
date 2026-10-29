package xyz.gekkao.sieversiacage.core.data

import xyz.gekkao.sieversiacage.core.sim.PlantState

/** これより乾いていたら知らせる。水をやらない日が 2 日続くと下回る（1 日 0.35 ずつ乾く、ADR-0020）。 */
internal const val DRY_SOIL_THRESHOLD = 0.5f

/** 夕方に水やりを知らせるか（ADR-0021）。土が乾いていて、その日まだ水をやっていないときだけ。 */
internal fun shouldRemind(
    state: PlantState,
    wateredToday: Boolean,
): Boolean = !wateredToday && state.soilMoisture < DRY_SOIL_THRESHOLD
