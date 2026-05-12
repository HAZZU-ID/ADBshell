package com.example.adbshell.adb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AdbProtocol {
    const val CMD_CNXN = 0x4e584e43
    const val CMD_AUTH = 0x48545541
    const val CMD_OPEN = 0x4e45504f
    const val CMD_OKAY = 0x59414b4f
    const val CMD_CLSE = 0x45534c43
    const val CMD_WRTE = 0x45545257
    const val VERSION  = 0x01000000
    const val MAX_DATA = 256 * 1024
    const val AUTH_RSAPUBLICKEY = 3

    data class AdbMessage(val command: Int, val arg0: Int, val arg1: Int, val data: ByteArray = ByteArray(0))

    fun encode(msg: AdbMessage): ByteArray {
        val crc = msg.data.sumOf { it.toInt() and 0xFF }
        val buf = ByteBuffer.allocate(24 + msg.data.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(msg.command); buf.putInt(msg.arg0); buf.putInt(msg.arg1)
        buf.putInt(msg.data.size); buf.putInt(crc); buf.putInt(msg.command.inv())
        if (msg.data.isNotEmpty()) buf.put(msg.data)
        return buf.array()
    }

    fun read(connection: UsbDeviceConnection, endpointIn: UsbEndpoint, timeoutMs: Int = 5000): AdbMessage? {
        val header = ByteArray(24)
        if (connection.bulkTransfer(endpointIn, header, 24, timeoutMs) < 24) return null
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = buf.int; val arg0 = buf.int; val arg1 = buf.int; val dataLength = buf.int; buf.int; buf.int
        val data = if (dataLength > 0) { val d = ByteArray(dataLength); connection.bulkTransfer(endpointIn, d, dataLength, timeoutMs); d } else ByteArray(0)
        return AdbMessage(command, arg0, arg1, data)
    }

    fun write(connection: UsbDeviceConnection, endpointOut: UsbEndpoint, msg: AdbMessage): Boolean {
        val bytes = encode(msg)
        return connection.bulkTransfer(endpointOut, bytes, bytes.size, 5000) == bytes.size
    }
}
