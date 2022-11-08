package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import java.io.*
import java.lang.Exception
import java.lang.NumberFormatException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var clientThread: ClientThread? = null
    private var thread: Thread? = null
    private var msgList: LinearLayout? = null
    private var handler: Handler? = null
    private var clientTextColor = 0
    private var edMessage: EditText? = null
    private var etIP: EditText? = null
    private var etPort: EditText? = null
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Client"
        threadPool = Executors.newFixedThreadPool(2)
        clientTextColor = ContextCompat.getColor(this, R.color.black)
        handler = Handler()
        msgList = findViewById(R.id.msgList)
        edMessage = findViewById(R.id.edMessage)
        etIP = findViewById(R.id.etIP)
        etPort = findViewById(R.id.etPort)
    }

    fun textView(message: String?, color: Int): TextView {
        if (null == message || message.trim { it <= ' ' }.isEmpty()) {
        }
        val tv = TextView(this)
        tv.setTextColor(color)
        tv.text = "$message [$time]"
        tv.textSize = 20f
        tv.setPadding(0, 5, 0, 0)
        return tv
    }

    fun showMessage(message: String?, color: Int) {
        handler!!.post { msgList!!.addView(textView(message, color)) }
    }

    override fun onClick(view: View) {
        try {
            if (view.id == R.id.connect_server) {
                Toast.makeText(this, "check your wifi connection", Toast.LENGTH_SHORT).show()
                SERVER_IP = etIP!!.text.toString().trim { it <= ' ' }
                SERVER_PORT = etPort!!.text.toString().trim { it <= ' ' }.toInt()
                msgList!!.removeAllViews()
                showMessage("Connecting...", clientTextColor)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        clientThread = ClientThread()
                        thread = Thread(clientThread)
                        thread!!.start()
                    } else {
                        val intent = Intent()
                        intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                        val uri = Uri.fromParts("package", this@MainActivity.packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                }
                clientThread = ClientThread()
                thread = Thread(clientThread)
                thread!!.start()
                return
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            Toast.makeText(this, "Please Enter IP address and Port Number", Toast.LENGTH_SHORT)
                .show()
        }
    }

    internal inner class ClientThread : Runnable {
        private var socket: Socket? = null
        private var input: BufferedReader? = null
        override fun run() {
            try {
                val serverAddr = InetAddress.getByName(SERVER_IP)
                socket = Socket(serverAddr, SERVER_PORT)
                while (!Thread.currentThread().isInterrupted) {
                    if (socket!!.isConnected) {
                        showMessage("Connected to Server...", clientTextColor)
                        input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                        val message = input!!.read()
                        val value_char = message.toChar()
                        msg = value_char.toString()
                        threadPool!!.execute(TextThread())
                        showMessage("Server: " + msg, clientTextColor)
                        Log.d("messsage:", msg!!)
                        val clientMessage = msg
                        showMessage(clientMessage, Color.BLUE)
                        if (null != clientThread) {
                            clientThread!!.sendMessage(clientMessage)
                        }
                        if (message == -1) {
                            Thread.currentThread().interrupt()
                            showMessage("Server Disconnected..........!", Color.RED)
                            break
                        }
                    } else {
                        showMessage("Check your network...", clientTextColor)
                    }
                }
            } catch (e1: UnknownHostException) {
                e1.printStackTrace()
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
        }

        fun sendMessage(message: String?) {
            Thread {
                try {
                    if (null != socket) {
                        val out = PrintWriter(
                            BufferedWriter(
                                OutputStreamWriter(socket!!.getOutputStream())
                            ),
                            true
                        )
                        out.println(message)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    val time: String
        get() {
            @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("HH:mm:ss")
            return sdf.format(Date())
        }

    override fun onDestroy() {
        super.onDestroy()
        if (null != clientThread) {
            clientThread!!.sendMessage("Disconnect")
            clientThread = null
        }
    }

    internal class TextThread : Runnable {
        override fun run() {
            val fileName = "Control" + ".txt"
            try {
                val root = File(
                    Environment.getExternalStorageDirectory().toString() + File.separator + "Socket"
                )
                root.mkdirs()
                val text_file = File(root, fileName)
                val writer = FileWriter(text_file, true)
                Log.d("File created", writer.toString())
                writer.append(msg).append("\n")
                writer.flush()
                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        var SERVER_PORT = 0
        var SERVER_IP: String? = null
        var threadPool: ExecutorService? = null
        var msg: String? = null
    }
}