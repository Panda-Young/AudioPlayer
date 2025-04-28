package com.panda.audioplayer.utils

object AudioDataConverter {

    fun convert8BitTo16Bit(data: ByteArray): ByteArray {
        val convertedData = ByteArray(data.size * 2)
        for (i in data.indices) {
            val sample = data[i].toInt() and 0xFF
            val sample16Bit = (sample - 128) * 256
            convertedData[i * 2] = (sample16Bit and 0xFF).toByte()
            convertedData[i * 2 + 1] = (sample16Bit shr 8 and 0xFF).toByte()
        }
        return convertedData
    }

    fun convert32BitFloatTo16Bit(data: ByteArray): ByteArray {
        val convertedData = ByteArray(data.size / 4 * 2)
        for (i in 0 until data.size / 4) {
            val sample = java.lang.Float.intBitsToFloat(
                (data[i * 4].toInt() and 0xFF) or
                        ((data[i * 4 + 1].toInt() and 0xFF) shl 8) or
                        ((data[i * 4 + 2].toInt() and 0xFF) shl 16) or
                        ((data[i * 4 + 3].toInt() and 0xFF) shl 24)
            )
            val sample16Bit = (sample * 32767).toInt()
            convertedData[i * 2] = (sample16Bit and 0xFF).toByte()
            convertedData[i * 2 + 1] = (sample16Bit shr 8 and 0xFF).toByte()
        }
        return convertedData
    }

    fun convert32BitIntTo16Bit(data: ByteArray): ByteArray {
        val convertedData = ByteArray(data.size / 4 * 2)
        for (i in 0 until data.size / 4) {
            val sample = (data[i * 4].toInt() and 0xFF) or
                    ((data[i * 4 + 1].toInt() and 0xFF) shl 8) or
                    ((data[i * 4 + 2].toInt() and 0xFF) shl 16) or
                    ((data[i * 4 + 3].toInt() and 0xFF) shl 24)
            val sample16Bit = (sample / 65536).coerceIn(-32768, 32767)
            convertedData[i * 2] = (sample16Bit and 0xFF).toByte()
            convertedData[i * 2 + 1] = (sample16Bit shr 8 and 0xFF).toByte()
        }
        return convertedData
    }
}
