package com.iwakura.an9hack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch
import androidx.fragment.app.Fragment

class ControlTab1 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_control_tab1, container, false)
        val unlockButton: Button = view.findViewById(R.id.t1b1)
        val lockButton: Button = view.findViewById(R.id.t1b2)
        val stsButton: Button = view.findViewById(R.id.t1b3)
        val modeSelect: RadioGroup = view.findViewById(R.id.radioGroup)
        val throttleSwitch: Switch = view.findViewById(R.id.throttleSwitch)
        val headlightSwitch: Switch = view.findViewById(R.id.headlightSwitch)
        unlockButton.setOnClickListener {
            val result = Bundle()
            result.putByteArray("data", byteArrayOf(0))
            parentFragmentManager.setFragmentResult("tab1tx", result)
        }
        lockButton.setOnClickListener {
            val result = Bundle()
            result.putByteArray("data", byteArrayOf(1))
            parentFragmentManager.setFragmentResult("tab1tx", result)
        }
        stsButton.setOnClickListener {
            val selectedMode: Byte = when (modeSelect.checkedRadioButtonId) {
                R.id.mode1RB -> 0
                R.id.mode2RB -> 1
                R.id.mode3RB -> 2
                else -> (-1).toByte()
            }
            val switches: Byte = when {
                headlightSwitch.isChecked && throttleSwitch.isChecked -> 3
                headlightSwitch.isChecked -> 1
                throttleSwitch.isChecked -> 2
                else -> 0
            }
            val result = Bundle()
            result.putByteArray("data", byteArrayOf(2, selectedMode, switches))
            parentFragmentManager.setFragmentResult("tab1tx", result)
        }
        parentFragmentManager.setFragmentResultListener("tab1rx", this) { _, bundle ->
            val enabled = bundle.getBoolean("data")
            requireActivity().runOnUiThread {
                unlockButton.isEnabled = enabled
                lockButton.isEnabled = enabled
                stsButton.isEnabled = enabled
            }
        }
        return view
    }
}


