package info.dvkr.screenstream.ui.fragments

import android.graphics.Color.parseColor
import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.color.colorChooser
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.google.android.material.textfield.TextInputEditText
import info.dvkr.screenstream.R
import info.dvkr.screenstream.data.other.getTag
import info.dvkr.screenstream.data.settings.Settings
import info.dvkr.screenstream.data.settings.SettingsReadOnly
import info.dvkr.screenstream.ui.router.FragmentRouter
import kotlinx.android.synthetic.main.dialog_settings_resize.view.*
import kotlinx.android.synthetic.main.fragment_settings.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class SettingsFragment : Fragment() {

    companion object {
        fun getFragmentCreator() = object : FragmentRouter.FragmentCreator {
            override fun getMenuItemId(): Int = R.id.menu_settings_fragment
            override fun getTag(): String = SettingsFragment::class.java.name
            override fun newInstance(): Fragment = SettingsFragment()
        }
    }

    private val settings: Settings by inject()
    private val settingsListener = object : SettingsReadOnly.OnSettingsChangeListener {
        override fun onSettingsChanged(key: String) = when (key) {
            Settings.Key.HTML_BACK_COLOR ->
                v_fragment_settings_html_back_color.setBackgroundColor(settings.htmlBackColor)

            Settings.Key.RESIZE_FACTOR ->
                tv_fragment_settings_resize_image_value.text =
                        getString(R.string.pref_resize_value).format(settings.resizeFactor)

            Settings.Key.JPEG_QUALITY ->
                tv_fragment_settings_jpeg_quality_value.text = settings.jpegQuality.toString()

            Settings.Key.PIN ->
                tv_fragment_settings_set_pin_value.text = settings.pin

            Settings.Key.SERVER_PORT ->
                tv_fragment_settings_server_port_value.text = settings.severPort.toString()

            else -> Unit
        }
    }

    private val screenSize: Point by lazy {
        Point().apply {
            ContextCompat.getSystemService(requireContext(), WindowManager::class.java)
                ?.defaultDisplay?.getRealSize(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag(getTag("onViewCreated")).w("Invoked")

        // Interface - Minimize on stream
        with(cb_fragment_settings_minimize_on_stream) {
            isChecked = settings.minimizeOnStream
            setOnClickListener { settings.minimizeOnStream = isChecked }
            cl_fragment_settings_minimize_on_stream.setOnClickListener { performClick() }
        }

        // Interface - Stop on sleep
        with(cb_fragment_settings_stop_on_sleep) {
            isChecked = settings.stopOnSleep
            setOnClickListener { settings.stopOnSleep = isChecked }
            cl_fragment_settings_stop_on_sleep.setOnClickListener { performClick() }
        }

        // Interface - StartService on boot
        with(cb_fragment_settings_start_on_boot) {
            isChecked = settings.startOnBoot
            setOnClickListener { settings.startOnBoot = isChecked }
            cl_fragment_settings_start_on_boot.setOnClickListener { performClick() }
        }

        // Interface - HTML MJPEG check
        with(cb_fragment_settings_mjpeg_check) {
            isChecked = settings.disableMJPEGCheck
            setOnClickListener { settings.disableMJPEGCheck = isChecked }
            cl_fragment_settings_mjpeg_check.setOnClickListener { performClick() }
        }

        // Interface - HTML Back color
        v_fragment_settings_html_back_color.setBackgroundColor(settings.htmlBackColor)
        cl_fragment_settings_html_back_color.setOnClickListener {
            MaterialDialog(requireActivity()).show {
                title(R.string.pref_html_back_color_title)
                icon(R.drawable.ic_settings_color_back_24dp)
                colorChooser(
                    colors = intArrayOf(
                        parseColor("#F44336"), parseColor("#E91E63"), parseColor("#9C27B0"),
                        parseColor("#673AB7"), parseColor("#3F51B5"), parseColor("#2196F3"),
                        parseColor("#03A9F4"), parseColor("#00BCD4"), parseColor("#009688"),
                        parseColor("#4CAF50"), parseColor("#8BC34A"), parseColor("#CDDC39"),
                        parseColor("#FFEB3B"), parseColor("#FFC107"), parseColor("#FF9800"),
                        parseColor("#FF5722"), parseColor("#795548"), parseColor("#9E9E9E"),
                        parseColor("#607D8B"), parseColor("#000000")
                    ),
                    initialSelection = settings.htmlBackColor,
                    allowCustomArgb = true
                ) { _, color -> if (settings.htmlBackColor != color) settings.htmlBackColor = color }
                positiveButton(android.R.string.ok)
                negativeButton(android.R.string.cancel)
            }
        }

        // Image - Resize factor
        tv_fragment_settings_resize_image_value.text =
                getString(R.string.pref_resize_value).format(settings.resizeFactor)

        val resizePictureSizeString = getString(R.string.pref_resize_dialog_result)
        cl_fragment_settings_resize_image.setOnClickListener {
            val resizeDialog = MaterialDialog(requireActivity())
                .title(R.string.pref_resize)
                .icon(R.drawable.ic_settings_resize_24dp)
                .customView(R.layout.dialog_settings_resize)
                .positiveButton(android.R.string.ok) { dialog ->
                    val tietView =
                        dialog.getCustomView()?.findViewById<TextInputEditText>(R.id.tiet_dialog_settings_resize)
                    val newValue = tietView?.text?.toString()?.toInt() ?: settings.resizeFactor
                    if (settings.resizeFactor != newValue) settings.resizeFactor = newValue
                }
                .negativeButton(android.R.string.cancel)

            resizeDialog.getCustomView()?.apply DialogView@{
                tv_dialog_settings_resize_content.text =
                        getString(R.string.pref_resize_dialog_text).format(screenSize.x, screenSize.y)

                ti_dialog_settings_resize.isCounterEnabled = true
                ti_dialog_settings_resize.counterMaxLength = 3

                with(tiet_dialog_settings_resize) {
                    addTextChangedListener(SimpleTextWatcher { text ->
                        val isValid = text.length in 2..3 && text.toString().toInt() in 10..150
                        resizeDialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                        val newResizeFactor = (if (isValid) text.toString().toInt() else settings.resizeFactor) / 100f
                        this@DialogView.tv_dialog_settings_resize_result.text = resizePictureSizeString.format(
                            (screenSize.x * newResizeFactor).toInt(),
                            (screenSize.y * newResizeFactor).toInt()
                        )
                    })
                    setText(settings.resizeFactor.toString())
                    setSelection(settings.resizeFactor.toString().length)
                    filters = arrayOf<InputFilter>(InputFilter.LengthFilter(3))
                }

                tv_dialog_settings_resize_result.text = resizePictureSizeString.format(
                    (screenSize.x * settings.resizeFactor / 100f).toInt(),
                    (screenSize.y * settings.resizeFactor / 100f).toInt()
                )

            }
            resizeDialog.show()
        }

        // Image - Jpeg Quality
        tv_fragment_settings_jpeg_quality_value.text = settings.jpegQuality.toString()
        cl_fragment_settings_jpeg_quality.setOnClickListener {
            MaterialDialog(requireActivity()).show {
                title(R.string.pref_jpeg_quality)
                icon(R.drawable.ic_settings_high_quality_24dp)
                message(R.string.pref_jpeg_quality_dialog)
                input(
                    prefill = settings.jpegQuality.toString(),
                    inputType = InputType.TYPE_CLASS_NUMBER,
                    maxLength = 3,
                    waitForPositiveButton = false
                ) { dialog, text ->
                    val isValid = text.length in 2..3 && text.toString().toInt() in 10..100
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                }
                positiveButton(android.R.string.ok) { dialog ->
                    val newValue = dialog.getInputField()?.text?.toString()?.toInt() ?: settings.jpegQuality
                    if (settings.jpegQuality != newValue) settings.jpegQuality = newValue
                }
                negativeButton(android.R.string.cancel)
                getInputField()?.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(3))
                getInputField()?.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }
        }


        // Security - Enable pin
        with(cb_fragment_settings_enable_pin)
        {
            isChecked = settings.enablePin
            enableDisableViewWithChildren(cl_fragment_settings_hide_pin_on_start, settings.enablePin)
            enableDisableViewWithChildren(cl_fragment_settings_new_pin_on_app_start, settings.enablePin)
            enableDisableViewWithChildren(cl_fragment_settings_auto_change_pin, settings.enablePin)
            enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            setOnClickListener {
                settings.enablePin = isChecked
                enableDisableViewWithChildren(cl_fragment_settings_hide_pin_on_start, isChecked)
                enableDisableViewWithChildren(cl_fragment_settings_new_pin_on_app_start, isChecked)
                enableDisableViewWithChildren(cl_fragment_settings_auto_change_pin, isChecked)
                enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            }
            cl_fragment_settings_enable_pin.setOnClickListener { performClick() }
        }

        // Security - Hide pin on start
        with(cb_fragment_settings_hide_pin_on_start)
        {
            isChecked = settings.hidePinOnStart
            setOnClickListener { settings.hidePinOnStart = isChecked }
            cl_fragment_settings_hide_pin_on_start.setOnClickListener { performClick() }
        }

        // Security - New pin on app start
        with(cb_fragment_settings_new_pin_on_app_start)
        {
            isChecked = settings.newPinOnAppStart
            enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            setOnClickListener {
                settings.newPinOnAppStart = isChecked
                enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            }
            cl_fragment_settings_new_pin_on_app_start.setOnClickListener { performClick() }
        }

        // Security - Auto change pin
        with(cb_fragment_settings_auto_change_pin)
        {
            isChecked = settings.autoChangePin
            enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            setOnClickListener {
                settings.autoChangePin = isChecked
                enableDisableViewWithChildren(cl_fragment_settings_set_pin, canEnableSetPin())
            }
            cl_fragment_settings_auto_change_pin.setOnClickListener { performClick() }
        }

        // Security - Set pin
        tv_fragment_settings_set_pin_value.text = settings.pin
        cl_fragment_settings_set_pin.setOnClickListener {
            MaterialDialog(requireActivity()).show {
                title(R.string.pref_set_pin)
                icon(R.drawable.ic_settings_key_24dp)
                message(R.string.pref_set_pin_dialog)
                input(
                    prefill = settings.pin,
                    inputType = InputType.TYPE_CLASS_NUMBER,
                    maxLength = 4,
                    waitForPositiveButton = false
                ) { dialog, text ->
                    val isValid = text.length in 4..4 && text.toString().toInt() in 0..9999
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                }
                positiveButton(android.R.string.ok) { dialog ->
                    val newValue = dialog.getInputField()?.text?.toString() ?: settings.pin
                    if (settings.pin != newValue) settings.pin = newValue
                }
                negativeButton(android.R.string.cancel)
                getInputField()?.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))
                getInputField()?.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }
        }

        // Advanced - Use WiFi Only
        with(cb_fragment_settings_use_wifi_only)
        {
            isChecked = settings.useWiFiOnly
            setOnClickListener { settings.useWiFiOnly = isChecked }
            cl_fragment_settings_use_wifi_only.setOnClickListener { performClick() }
        }

        // Advanced - Server port
        tv_fragment_settings_server_port_value.text = settings.severPort.toString()
        cl_fragment_settings_server_port.setOnClickListener {
            MaterialDialog(requireActivity()).show {
                title(R.string.pref_server_port)
                icon(R.drawable.ic_settings_http_24dp)
                message(R.string.pref_server_port_dialog)
                input(
                    prefill = settings.severPort.toString(),
                    inputType = InputType.TYPE_CLASS_NUMBER,
                    maxLength = 5,
                    waitForPositiveButton = false
                ) { dialog, text ->
                    val isValid = text.length in 4..5 && text.toString().toInt() in 1025..65535
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                }
                positiveButton(android.R.string.ok) { dialog ->
                    val newValue = dialog.getInputField()?.text?.toString()?.toInt() ?: settings.severPort
                    if (settings.severPort != newValue) settings.severPort = newValue
                }
                negativeButton(android.R.string.cancel)
                getInputField()?.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))
                getInputField()?.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }
        }
    }

    override fun onStart() {
        super.onStart()
        settings.registerChangeListener(settingsListener)
        Timber.tag(getTag("onStart")).w("Invoked")
    }

    override fun onStop() {
        Timber.tag(getTag("onStop")).w("Invoked")
        settings.unregisterChangeListener(settingsListener)
        super.onStop()
    }

    private fun canEnableSetPin(): Boolean =
        cb_fragment_settings_enable_pin.isChecked &&
                cb_fragment_settings_new_pin_on_app_start.isChecked.not() &&
                cb_fragment_settings_auto_change_pin.isChecked.not()

    private fun enableDisableViewWithChildren(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else .5f
        if (view is ViewGroup)
            for (idx in 0 until view.childCount) enableDisableViewWithChildren(view.getChildAt(idx), enabled)
    }

    private class SimpleTextWatcher(private val afterTextChangedBlock: (s: Editable) -> Unit) : TextWatcher {
        override fun afterTextChanged(s: Editable?) = s?.let { afterTextChangedBlock(it) } as Unit
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
    }
}