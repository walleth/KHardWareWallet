package org.walleth.khartwarewallet.demo.trezor

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.trezor.deviceprotocol.Features
import io.trezor.deviceprotocol.GetFeatures
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import org.walleth.khartwarewallet.trezor.tryConnectTrezor

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        connectAndShowResult()
    }

    private fun connectAndShowResult() {
        lifecycleScope.launch {

            tryConnectTrezor(
                context = this@MainActivity,
                onPermissionDenied = {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("Without this permission we cannot talk to the device")
                        .setPositiveButton("try again") { _, _ -> connectAndShowResult() }
                        .setNegativeButton("exit") { _, _ -> finish() }

                },
                onDeviceConnected = { comm ->
                    when (val receivedMsg = comm.exchangeMessage(GetFeatures())) {
                        is Features -> {
                            info_text.text = getFeaturesText(receivedMsg)
                        }
                    }
                    comm.disconnect()
                })
        }
    }

    private fun getFeaturesText(res: Features) = """Vendor: ${res.vendor}
        |Version: ${res.major_version}.${res.minor_version}.${res.patch_version}
        |initialized: ${res.initialized}
        |label: ${res.label}""".trimMargin()

}