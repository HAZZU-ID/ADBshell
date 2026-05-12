package com.example.adbshell.adb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val ACTION_USB_PERMISSION = "com.example.adbshell.USB_PERMISSION"
private const val ADB_CLASS = 0xFF; private const val ADB_SUBCLASS = 0x42; private const val ADB_PROTOCOL = 0x01

data class UsbAdbResult(val output: String, val isError: Boolean)

class UsbAdbManager(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var localId = 1; private var remoteId = 0

    fun findAdbDevices(): List<UsbDevice> = usbManager.deviceList.values.filter { device ->
        (0 until device.interfaceCount).any { i -> device.getInterface(i).let { it.interfaceClass == ADB_CLASS && it.interfaceSubclass == ADB_SUBCLASS && it.interfaceProtocol == ADB_PROTOCOL } }
    }

    suspend fun requestPermission(device: UsbDevice): Boolean = suspendCancellableCoroutine { cont ->
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                context.unregisterReceiver(this)
                if (cont.isActive) cont.resume(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
            }
        }
        context.registerReceiver(receiver, IntentFilter(ACTION_USB_PERMISSION))
        val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        usbManager.requestPermission(device, PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags))
        cont.invokeOnCancellation { context.unregisterReceiver(receiver) }
    }

    fun open(device: UsbDevice): Boolean {
        val iface = (0 until device.interfaceCount).map { device.getInterface(it) }.firstOrNull { it.interfaceClass == ADB_CLASS && it.interfaceSubclass == ADB_SUBCLASS && it.interfaceProtocol == ADB_PROTOCOL } ?: return false
        val epIn = (0 until iface.endpointCount).map { iface.getEndpoint(it) }.firstOrNull { it.direction == UsbConstants.USB_DIR_IN } ?: return false
        val epOut = (0 until iface.endpointCount).map { iface.getEndpoint(it) }.firstOrNull { it.direction == UsbConstants.USB_DIR_OUT } ?: return false
        val conn = usbManager.openDevice(device) ?: return false
        if (!conn.claimInterface(iface, true)) { conn.close(); return false }
        connection = conn; endpointIn = epIn; endpointOut = epOut
        AdbProtocol.write(conn, epOut, AdbProtocol.AdbMessage(AdbProtocol.CMD_CNXN, AdbProtocol.VERSION, AdbProtocol.MAX_DATA, "host::features=shell_v2".toByteArray()))
        val reply = AdbProtocol.read(conn, epIn) ?: return false
        if (reply.command == AdbProtocol.CMD_AUTH) {
            AdbProtocol.write(conn, epOut, AdbProtocol.AdbMessage(AdbProtocol.CMD_AUTH, AdbProtocol.AUTH_RSAPUBLICKEY, 0, "ADBShellApp
".toByteArray()))
            val r2 = AdbProtocol.read(conn, epIn) ?: return false
            if (r2.command != AdbProtocol.CMD_CNXN) return false
        } else if (reply.command != AdbProtocol.CMD_CNXN) return false
        return true
    }

    fun shell(command: String): UsbAdbResult {
        val conn = connection ?: return UsbAdbResult("Not connected", true)
        val epIn = endpointIn ?: return UsbAdbResult("Not connected", true)
        val epOut = endpointOut ?: return UsbAdbResult("Not connected", true)
        val id = localId++
        AdbProtocol.write(conn, epOut, AdbProtocol.AdbMessage(AdbProtocol.CMD_OPEN, id, 0, "shell:$command".toByteArray()))
        var msg = AdbProtocol.read(conn, epIn) ?: return UsbAdbResult("No response", true)
        if (msg.command != AdbProtocol.CMD_OKAY) return UsbAdbResult("Unexpected", true)
        remoteId = msg.arg0
        val sb = StringBuilder()
        while (true) {
            msg = AdbProtocol.read(conn, epIn, 10_000) ?: break
            when (msg.command) {
                AdbProtocol.CMD_WRTE -> { sb.append(String(msg.data)); AdbProtocol.write(conn, epOut, AdbProtocol.AdbMessage(AdbProtocol.CMD_OKAY, id, remoteId)) }
                AdbProtocol.CMD_CLSE -> break; else -> break
            }
        }
        return UsbAdbResult(sb.toString().trimEnd(), false)
    }

    fun close() { connection?.close(); connection = null; endpointIn = null; endpointOut = null }
                                                                  }
