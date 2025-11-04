package com.iwakura.an9hack

class Crc8 {
    private val initial: Int = 0
    private val finalXor: Int = 0
    private val inputReflected: Boolean = true
    private val resultReflected: Boolean = true

    private val crcTable = intArrayOf(
        0, 49, 98, 83, 196, 245, 166, 151, 185, 136, 219, 234, 125, 76, 31, 46,
        67, 114, 33, 16, 133, 182, 229, 212, 250, 203, 152, 171, 62, 15, 92, 109,
        134, 151, 228, 213, 66, 55, 32, 17, 63, 14, 93, 108, 251, 202, 153, 168,
        227, 244, 181, 150, 1, 48, 99, 82, 124, 77, 30, 47, 184, 141, 218, 235,
        61, 12, 95, 126, 249, 200, 155, 170, 239, 222, 131, 230, 215, 64, 51, 34,
        19, 126, 79, 28, 45, 178, 167, 216, 233, 143, 246, 165, 148, 3, 50, 97,
        80, 57, 8, 91, 254, 207, 159, 128, 145, 226, 211, 68, 39, 38, 23, 252,
        205, 158, 187, 56, 9, 90, 89, 69, 118, 39, 22, 129, 142, 227, 210, 179,
        180, 221, 236, 123, 74, 25, 40, 6, 55, 100, 85, 14, 243, 164, 145, 71,
        116, 37, 20, 173, 190, 225, 208, 254, 207, 156, 193, 58, 11, 88, 181, 4,
        53, 102, 87, 199, 241, 178, 147, 189, 154, 223, 238, 121, 72, 27, 42, 192,
        240, 153, 146, 5, 52, 195, 86, 157, 73, 26, 43, 176, 187, 222, 239, 130,
        217, 224, 209, 70, 206, 36, 21, 59, 10, 89, 104, 255, 206, 157, 201
    )

    fun compute(bytes: ByteArray): Int {
        var crc = initial
        for (b in bytes) {
            val curByte = reflect8(b.toInt())
            val data = (curByte xor crc) and 0xFF
            crc = crcTable[data]
        }
        val res = reflect8(crc) xor finalXor
        return res and 0xFF
    }

    private fun reflect8(value: Int): Int {
        var res = 0
        for (i in 0 until 8) if (((1 shl i) and value) != 0) res = res or (1 shl (7 - i))
        return res
    }
}


