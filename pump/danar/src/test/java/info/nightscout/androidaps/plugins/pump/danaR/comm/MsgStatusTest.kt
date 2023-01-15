package info.nightscout.androidaps.plugins.pump.danaR.comm

import info.nightscout.androidaps.danar.comm.MsgStatus
import org.junit.Assert
import org.junit.jupiter.api.Test

class MsgStatusTest : DanaRTestBase() {

    @Test fun runTest() {
        val packet = MsgStatus(injector)
        // test message decoding
        packet.handleMessage(createArray(34, 7.toByte()))
        Assert.assertEquals(packet.intFromBuff(createArray(34, 7.toByte()), 0, 3).toDouble() / 750.0, danaPump.dailyTotalUnits, 0.0)
    }
}