package io.github.runedata.cache.content.config

import io.github.runedata.cache.content.IndexType
import io.github.runedata.cache.filesystem.Archive
import io.github.runedata.cache.filesystem.CacheStore
import io.github.runedata.cache.filesystem.util.getString
import io.github.runedata.cache.filesystem.util.getUnsignedByte
import java.nio.ByteBuffer

class ParamDefinition(id: Int) : ConfigEntry(id) {
    var stackType: Char = '\u0000'
    var autoDisable = true
    var defaultInt: Int = 0
    lateinit var defaultString: String

    override fun decode(buffer: ByteBuffer): ParamDefinition {
        while (true) {
            val opcode = buffer.getUnsignedByte()
            when (opcode) {
                0 -> return this
                1 -> {
                    var charId = buffer.get().toInt() and 0xFF
                    if (charId !in 128..159)
                    if (charId in 128..159) {
                        var extChar = asciiExtension[charId - 128]
                        if (extChar.toInt() == 0) {
                            extChar = 63.toChar()
                        }
                        charId = extChar.toInt()
                    } else {
                        throw IllegalArgumentException("Not a valid character")
                    }
                    stackType = charId.toChar()
                }
                2 -> defaultInt = buffer.int
                4 -> autoDisable = false
                5 -> defaultString = buffer.getString()
            }
        }
    }

    companion object {
        val asciiExtension = charArrayOf('€', '\u0000', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š',
            '‹', 'Œ', '\u0000', 'Ž', '\u0000', '\u0000', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ',
            '\u0000', 'ž', 'Ÿ'
        )

        private const val ARCHIVE_INDEX = 11

        fun load(store: CacheStore): Map<Int, ParamDefinition> {
            val refTable = store.getReferenceTable(IndexType.CONFIGS.id)
            val entry = refTable.getEntry(ARCHIVE_INDEX)
            val archive = Archive.decode(store.readData(
                IndexType.CONFIGS.id,
                ARCHIVE_INDEX
            ).data,
                refTable.getEntry(ARCHIVE_INDEX)!!.amountOfChildren
            )

            var defCount = 0
            val paramDefs = mutableMapOf<Int, ParamDefinition>()
            for(id in 0 until entry!!.capacity) {
                val child = entry.getEntry(id) ?: continue
                paramDefs[id] = ParamDefinition(id).decode(archive.getEntry(child.index))
                defCount++
            }
            return paramDefs
        }
    }
}