package com.iwakura.an9hack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandCreatorTest {
    @Test
    fun key_unlock_lock_set_commands_have_valid_crc() {
        val creator = CommandCreator()
        // emulate that key was learned
        creator.actualKey = 0x2A

        val cmds = listOf(
            creator.getKeyCommand("12345678"),
            creator.getUnlockCommand(),
            creator.getLockCommand(),
            creator.getSetScooterCommand(1, 2, 3)
        )

        for (cmd in cmds) {
            assertTrue(cmd.isNotEmpty())
            val body = cmd.copyOf(cmd.size - 1)
            val crc = cmd.last().toInt() and 0xFF
            val crcCalc = Crc8().compute(body)
            assertEquals(crcCalc, crc)
        }
    }
}


