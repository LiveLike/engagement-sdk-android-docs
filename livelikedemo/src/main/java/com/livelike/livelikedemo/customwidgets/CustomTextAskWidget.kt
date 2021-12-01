package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.widget.widgetModel.TextAskWidgetModel
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_text_ask.view.bodyText
import kotlinx.android.synthetic.main.custom_text_ask.view.confirmationMessageTv
import kotlinx.android.synthetic.main.custom_text_ask.view.inputTxt
import kotlinx.android.synthetic.main.custom_text_ask.view.sendBtn
import kotlinx.android.synthetic.main.custom_text_ask.view.titleView

class CustomTextAskWidget : ConstraintLayout {
    var askWidgetModel: TextAskWidgetModel? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        inflate(context, R.layout.custom_text_ask, this@CustomTextAskWidget)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        askWidgetModel?.widgetData?.let { liveLikeWidget ->
            titleView.text = liveLikeWidget.title
            bodyText.text = liveLikeWidget.prompt
            confirmationMessageTv.text = liveLikeWidget.confirmationMessage
            confirmationMessageTv.visibility = View.GONE

            inputTxt.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(arg0: Editable) {
                    if (inputTxt.isEnabled) enableSendBtn() // send button is enabled
                }

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            })

            sendBtn.setOnClickListener {
                if (inputTxt.text.toString().trim().isNotEmpty()) {
                    disableUserInput()// user input edit text disbaled
                    disableSendBtn() // send button disbaled
                    askWidgetModel?.submitReply(inputTxt.text.toString().trim())
                    hideKeyboard()
                    confirmationMessageTv.visibility = VISIBLE
                }
            }

        }
    }

    fun enableSendBtn(){
        val isReady: Boolean = inputTxt.text.toString().isNotEmpty()
        sendBtn.isEnabled = isReady
    }

    private fun disableSendBtn(){
        sendBtn.isEnabled = false
    }


    private fun disableUserInput(){
        inputTxt.isFocusableInTouchMode = false
        inputTxt.isCursorVisible = false
        inputTxt.clearFocus()
    }

    private fun hideKeyboard(){
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            inputTxt.windowToken,
            0
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }



}