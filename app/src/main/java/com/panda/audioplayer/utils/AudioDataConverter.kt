package com.panda.audioplayer.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    fun convert24BitTo16Bit(data: ByteArray): ByteArray {
        val numSamples = data.size / 3
        val convertedData = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val low = data[i * 3].toInt() and 0xFF
            val mid = data[i * 3 + 1].toInt() and 0xFF
            val high = data[i * 3 + 2].toInt() and 0xFF
            val raw = (low shl 0) or (mid shl 8) or (high shl 16)
            val sample24 = (raw shl 8) shr 8
            val sample16Bit = ((sample24 + 128) shr 8).coerceIn(-32768, 32767)
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

    fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val floatArray = FloatArray(byteArray.size / 2)
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val shortBuffer = byteBuffer.asShortBuffer()
        for (i in 0 until floatArray.size) {
            val shortValue = shortBuffer.get(i)
            floatArray[i] = shortValue.toFloat() / 32768.0f
        }
        return floatArray
    }

    fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val byteArray = ByteArray(floatArray.size * 2)
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val shortBuffer = byteBuffer.asShortBuffer()
        for (i in 0 until floatArray.size) {
            val floatValue = floatArray[i].coerceIn(-1.0f, 1.0f)
            val shortValue = (floatValue * 32768.0f).toInt().toShort()
            shortBuffer.put(i, shortValue)
        }
        return byteArray
    }
}
