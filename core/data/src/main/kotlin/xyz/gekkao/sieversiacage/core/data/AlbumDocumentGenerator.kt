package xyz.gekkao.sieversiacage.core.data

import xyz.gekkao.sieversiacage.core.compiler.DocumentTarget
import xyz.gekkao.sieversiacage.core.compiler.PLANT_COMPILER_VERSION
import xyz.gekkao.sieversiacage.core.compiler.REMOTE_COMPOSE_VERSION
import xyz.gekkao.sieversiacage.core.compiler.WriterPlantDocumentCompiler
import xyz.gekkao.sieversiacage.core.sim.PlantState
import javax.inject.Inject

/** アルバム用のドキュメントを作る。生成メタデータ（ADR-0011）も一緒に持つ。テストではこれを偽物に差し替える。 */
interface AlbumDocumentGenerator {
    val remoteComposeVersion: String
    val compilerVersion: Int
    val target: DocumentTarget

    fun generate(state: PlantState): ByteArray
}

/** 今の Compiler でアプリ内（ショーケース・アルバム）向けに作る。 */
class InAppAlbumDocumentGenerator
    @Inject
    constructor() : AlbumDocumentGenerator {
        override val remoteComposeVersion: String = REMOTE_COMPOSE_VERSION
        override val compilerVersion: Int = PLANT_COMPILER_VERSION
        override val target: DocumentTarget = DocumentTarget.IN_APP

        private val compiler = WriterPlantDocumentCompiler(target)

        override fun generate(state: PlantState): ByteArray = compiler.compile(state)
    }
