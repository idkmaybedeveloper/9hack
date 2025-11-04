package com.iwakura.an9hack

import android.util.Log
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.Random

class CommandCreator {
    private val header = byteArrayOf((-93).toByte(), (-92).toByte())
    private val commandBytes = byteArrayOf(1, 16, 5, 21, 97, 127)
    var actualKey: Byte = 0
    private lateinit var controlActivity: ControlActivity

    fun interceptMessage(message: ByteArray) {
        Log.i("CommandCreator", "Intercepting Command")
        val mHeader = byteArrayOf(message[0], message[1])
        if (Arrays.equals(mHeader, header)) {
            val length = message[2]
            val rand = (message[3] - 50).toByte()
            val cmd = (message[5].toInt() xor rand.toInt()).toByte()
            val out = ByteArrayOutputStream()
            for (i in 0 until length) out.write((message[i + 6].toInt() xor rand.toInt()))
            val data = out.toByteArray()
            for (b in commandBytes) if (b == cmd) {
                decodeMessage(cmd, data)
                Log.i("CommandCreator", "Got Something")
            }
        }
    }

    private fun decodeMessage(command: Byte, data: ByteArray) {
        if (command == commandBytes[0]) {
            if (data[0] == 1.toByte()) {
                actualKey = data[1]
                controlActivity.controlTabs(true)
                controlActivity.addLogText("meow~ Key is Correct!")
                controlActivity.addLogText("Key: 0x" + toHex(byteArrayOf(actualKey)))
            } else {
                controlActivity.addLogText("uh oh, key is incorrect! :(")
            }
        }
        if (command == commandBytes[1]) {
            controlActivity.addLogText("Uh oh, we got an error from the scooter!??")
        }
        if (command == commandBytes[2]) {
            if (data[0] == 1.toByte()) controlActivity.addLogText("Scooter Unlocked Successfully! :D")
            else controlActivity.addLogText("Scooter Unlock Failed! :(")
        }
        if (command == commandBytes[3]) {
            if (data[0] == 1.toByte()) controlActivity.addLogText("Scooter Locked Successfully! :D")
            else controlActivity.addLogText("Scooter Lock Failed! :(")
        }
        if (command == commandBytes[4]) {
            if (data[0] == 0.toByte()) controlActivity.addLogText("Scooter Set Successfully! :D")
            else controlActivity.addLogText("Scooter Set Failed! :(")
        }
    }

    fun getKeyCommand(keyStr: String): ByteArray {
        val originalRand = (Random().nextInt(254).toByte() + 1).toByte()
        val originalCmd = commandBytes[0]
        val originalDeviceKey = keyStr.toByteArray()
        val rand = (originalRand + 50).toByte()
        val key = (0 xor originalRand.toInt()).toByte()
        val cmd = (originalCmd.toInt() xor originalRand.toInt()).toByte()
        val out = ByteArrayOutputStream()
        for (b in originalDeviceKey) out.write((b.toInt() xor originalRand.toInt()))
        val deviceKey = out.toByteArray()
        val combined = ByteArray(header.size + 4 + deviceKey.size)
        val buffer = ByteBuffer.wrap(combined)
        buffer.put(header)
        buffer.put(8.toByte())
        buffer.put(rand)
        buffer.put(key)
        buffer.put(cmd)
        buffer.put(deviceKey)
        val hex1 = buffer.array()
        val crc = Crc8().compute(hex1).toByte()
        val combined2 = ByteArray(hex1.size + 1)
        val buffer2 = ByteBuffer.wrap(combined2)
        buffer2.put(hex1)
        buffer2.put(crc)
        return buffer2.array()
    }

    fun getUnlockCommand(): ByteArray {
        val originalRand = (Random().nextInt(254).toByte() + 1).toByte()
        val originalKey = actualKey
        val originalCmd = commandBytes[2]
        val originalData = byteArrayOf(1,1,1,1,1,1,1,1,1,1)
        val rand = (originalRand + 50).toByte()
        val key = (originalKey.toInt() xor originalRand.toInt()).toByte()
        val cmd = (originalCmd.toInt() xor originalRand.toInt()).toByte()
        val out = ByteArrayOutputStream()
        for (b in originalData) out.write((b.toInt() xor originalRand.toInt()))
        val data = out.toByteArray()
        val combined = ByteArray(header.size + 4 + data.size)
        val buffer = ByteBuffer.wrap(combined)
        buffer.put(header)
        buffer.put(10.toByte())
        buffer.put(rand)
        buffer.put(key)
        buffer.put(cmd)
        buffer.put(data)
        val hex1 = buffer.array()
        val crc = Crc8().compute(hex1).toByte()
        val combined2 = ByteArray(hex1.size + 1)
        val buffer2 = ByteBuffer.wrap(combined2)
        buffer2.put(hex1)
        buffer2.put(crc)
        return buffer2.array()
    }

    fun getLockCommand(): ByteArray {
        val originalRand = (Random().nextInt(254).toByte() + 1).toByte()
        val originalKey = actualKey
        val originalCmd = commandBytes[3]
        val originalData = byteArrayOf(1)
        val rand = (originalRand + 50).toByte()
        val key = (originalKey.toInt() xor originalRand.toInt()).toByte()
        val cmd = (originalCmd.toInt() xor originalRand.toInt()).toByte()
        val out = ByteArrayOutputStream()
        for (b in originalData) out.write((b.toInt() xor originalRand.toInt()))
        val data = out.toByteArray()
        val combined = ByteArray(header.size + 4 + data.size)
        val buffer = ByteBuffer.wrap(combined)
        buffer.put(header)
        buffer.put(1.toByte())
        buffer.put(rand)
        buffer.put(key)
        buffer.put(cmd)
        buffer.put(data)
        val hex1 = buffer.array()
        val crc = Crc8().compute(hex1).toByte()
        val combined2 = ByteArray(hex1.size + 1)
        val buffer2 = ByteBuffer.wrap(combined2)
        buffer2.put(hex1)
        buffer2.put(crc)
        return buffer2.array()
    }

    fun getSetScooterCommand(headlight: Byte, mode: Byte, throttle: Byte): ByteArray {
        val originalRand = (Random().nextInt(254).toByte() + 1).toByte()
        val originalKey = actualKey
        val originalCmd = commandBytes[4]
        val originalData = byteArrayOf(headlight, mode, throttle, 0)
        val rand = (originalRand + 50).toByte()
        val key = (originalKey.toInt() xor originalRand.toInt()).toByte()
        val cmd = (originalCmd.toInt() xor originalRand.toInt()).toByte()
        val out = ByteArrayOutputStream()
        for (b in originalData) out.write((b.toInt() xor originalRand.toInt()))
        val data = out.toByteArray()
        val combined = ByteArray(header.size + 4 + data.size)
        val buffer = ByteBuffer.wrap(combined)
        buffer.put(header)
        buffer.put(4.toByte())
        buffer.put(rand)
        buffer.put(key)
        buffer.put(cmd)
        buffer.put(data)
        val hex1 = buffer.array()
        val crc = Crc8().compute(hex1).toByte()
        val combined2 = ByteArray(hex1.size + 1)
        val buffer2 = ByteBuffer.wrap(combined2)
        buffer2.put(hex1)
        buffer2.put(crc)
        return buffer2.array()
    }

    private fun toHex(bytes: ByteArray): String {
        val bi = BigInteger(1, bytes)
        return String.format("%0" + (bytes.size shl 1) + "X", bi)
    }

    fun init(activity: ControlActivity) { this.controlActivity = activity }
}


