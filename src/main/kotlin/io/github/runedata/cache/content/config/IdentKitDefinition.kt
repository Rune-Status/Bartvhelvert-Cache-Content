/*
   Copyright 2018 Bart van Helvert

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package io.github.runedata.cache.content.config

import io.github.runedata.cache.content.IndexType
import io.github.runedata.cache.filesystem.Archive
import io.github.runedata.cache.filesystem.CacheStore
import io.github.runedata.cache.filesystem.util.getUnsignedByte
import io.github.runedata.cache.filesystem.util.getUnsignedShort
import java.nio.ByteBuffer

class IdentKitDefinition(id: Int) : ConfigEntry(id) {
    var colorFind: IntArray? = null
    var colorReplace: IntArray? = null
    var textureFind: IntArray? = null
    var textureReplace: IntArray? = null
    var bodyPartId = -1
    var modelIds: IntArray? = null
    var models = intArrayOf(-1, -1, -1, -1, -1)
    var nonSelectable = false

    override fun decode(buffer: ByteBuffer): IdentKitDefinition {
        while (true) {
            val opcode = buffer.getUnsignedByte()
            when (opcode) {
                0 -> return this
                1 -> bodyPartId = buffer.getUnsignedByte()
                2 -> {
                    val length = buffer.getUnsignedByte()
                    modelIds = IntArray(length) { buffer.getUnsignedShort() }
                }
                3 -> nonSelectable = true
                40 -> {
                    val colors = buffer.getUnsignedByte()
                    colorFind = IntArray(colors)
                    colorReplace = IntArray(colors)
                    for (i in 0 until colors) {
                        colorFind!![i] = buffer.getUnsignedShort()
                        colorReplace!![i] = buffer.getUnsignedShort()
                    }
                }
                41 -> {
                    val textures = buffer.getUnsignedByte()
                    textureFind = IntArray(textures)
                    textureReplace = IntArray(textures)
                    for (i in 0 until textures) {
                        textureFind!![i] = buffer.getUnsignedShort()
                        textureReplace!![i] = buffer.getUnsignedShort()
                    }
                }
                in 60..69 -> models[opcode - 60] = buffer.getUnsignedShort()
                else -> error(opcode)
            }
        }
    }

    companion object {
        private const val ARCHIVE_INDEX = 3

        fun load(store: CacheStore): Map<Int, IdentKitDefinition> {
            val refTable = store.getReferenceTable(IndexType.CONFIGS.id)
            val entry = refTable.getEntry(ARCHIVE_INDEX)
            val archive = Archive.decode(store.readData(
                IndexType.CONFIGS.id,
                ARCHIVE_INDEX
            ).data,
                    refTable.getEntry(ARCHIVE_INDEX)!!.amountOfChildren
            )

            var defCount = 0
            val idkDefs = mutableMapOf<Int, IdentKitDefinition>()
            for(id in 0 until entry!!.capacity) {
                val child = entry.getEntry(id) ?: continue
                idkDefs[id] = IdentKitDefinition(id).decode(archive.getEntry(child.index))
                defCount++
            }
            return idkDefs
        }
    }
}