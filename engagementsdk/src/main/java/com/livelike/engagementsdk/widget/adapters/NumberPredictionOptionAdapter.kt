package com.livelike.engagementsdk.widget.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.FontFamilyProvider
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.data.models.NumberPredictionVotes
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.NumberPredictionOptionsTheme
import com.livelike.engagementsdk.widget.ViewStyleProps
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Option
import kotlinx.android.synthetic.main.livelike_user_input.view.userInput
import kotlinx.android.synthetic.main.widget_number_prediction_item.view.bkgrd
import kotlinx.android.synthetic.main.widget_number_prediction_item.view.correct_answer
import kotlinx.android.synthetic.main.widget_number_prediction_item.view.description
import kotlinx.android.synthetic.main.widget_number_prediction_item.view.imgView


internal class NumberPredictionOptionAdapter(
    var myDataset: List<Option>,
    private val widgetType: WidgetType,
    var submitListener: EnableSubmitListener? = null,
    var component: NumberPredictionOptionsTheme? = null


) : RecyclerView.Adapter<NumberPredictionOptionAdapter.OptionViewHolder>() {

    var fontFamilyProvider: FontFamilyProvider? = null
    var selectionLocked = false
    var needToEnableSubmit = false
    var isCorrect = false
    var selectedUserVotes = mutableListOf<NumberPredictionVotes>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        return OptionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.widget_number_prediction_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        val item = myDataset[position]

        // sets the data
        holder.setData(
            item,
            widgetType,
            component,
            fontFamilyProvider
        )
    }

    override fun getItemCount() = myDataset.size


    inner class OptionViewHolder(view: View) :
        RecyclerView.ViewHolder(view){


        @SuppressLint("ClickableViewAccessibility")
        fun setData(
            option: Option,
            widgetType: WidgetType,
            component: NumberPredictionOptionsTheme?,
            fontFamilyProvider: FontFamilyProvider?
        ) {
            if (!option.image_url.isNullOrEmpty()) {
                Glide.with(itemView.context.applicationContext)
                    .load(option.image_url)
                    .into(itemView.imgView)
                itemView.imgView.visibility = View.VISIBLE
            } else {
                itemView.imgView.visibility = View.GONE
            }

            itemView.description.text = option.description
            itemView.userInput.setTextColor(ContextCompat.getColor(itemView.context, R.color.livelike_number_prediction_user_input))

            if (option.number != null) {
                itemView.userInput.setText(option.number.toString())
                itemView.userInput.isFocusableInTouchMode = false
                itemView.userInput.isCursorVisible = false
            }

            if(widgetType == WidgetType.IMAGE_NUMBER_PREDICTION_FOLLOW_UP ||
                widgetType == WidgetType.TEXT_NUMBER_PREDICTION_FOLLOW_UP){
                showCorrectOptionWithBackground(option)
            }else{
                itemView.correct_answer.visibility = View.GONE
            }

            itemView.userInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(arg0: Editable) {
                    if (selectionLocked) return

                    val value = arg0.toString()
                    if(value.isNotEmpty()) {
                        try {
                            myDataset[adapterPosition].number = value.toInt()
                        } catch (ex: NumberFormatException) {
                            logError { "Invalid input" }
                            return
                        }
                        updateOrAddVote()
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    //nothing to be done here
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    // nothing to be done here
                }
            })


            itemView.userInput.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val imm =
                        v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    itemView.userInput.isFocusable = false
                    itemView.userInput.isFocusableInTouchMode = true
                    true
                } else false
            }

            itemView.userInput.setOnTouchListener { v, _ -> // Disallow the touch request for parent scroll on touch of child view
                if (itemView.userInput.text.toString().isNotEmpty()) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                false
            }
            setItemBackground(component, fontFamilyProvider)

        }

        // add votes for submission
        private fun updateOrAddVote() {
            val userVote: NumberPredictionVotes? =
                selectedUserVotes.find { it.optionId == myDataset[adapterPosition].id }
            if (userVote == null) {
                selectedUserVotes.add(
                    NumberPredictionVotes(
                        myDataset[adapterPosition].id,
                        myDataset[adapterPosition].number!!
                    )
                )
            } else {
                userVote.number = myDataset[adapterPosition].number!!
            }

            needToEnableSubmit = selectedUserVotes.size == myDataset.size
            submitListener?.onSubmitEnabled(needToEnableSubmit)
        }


        // this is mainly used for theming purpose
        private fun setItemBackground(
            layoutPickerComponent: NumberPredictionOptionsTheme?,
            fontFamilyProvider: FontFamilyProvider?
        ) {
            logDebug { "number-prediction option background theme" }
            val optionDescTheme: ViewStyleProps? = layoutPickerComponent?.option
            AndroidResource.updateThemeForView(itemView.description, optionDescTheme, fontFamilyProvider)

            if (layoutPickerComponent?.option != null){
               itemView.bkgrd.background = AndroidResource.createDrawable(layoutPickerComponent?.option)
            }

            //input placeholder
            val optionInputPlaceholder: ViewStyleProps? = layoutPickerComponent?.optionInputFieldPlaceholder
            AndroidResource.updateThemeForView( itemView.userInput, optionInputPlaceholder, fontFamilyProvider)


            // user input with state
            if (layoutPickerComponent?.optionInputFieldEnabled?.background != null &&
                layoutPickerComponent?.optionInputFieldDisabled?.background != null
            ) {
                val userInputEnabledDrawable = AndroidResource.createDrawable(
                    layoutPickerComponent.optionInputFieldEnabled
                )
                val userInputDisabledDrawable = AndroidResource.createDrawable(
                    layoutPickerComponent.optionInputFieldDisabled
                )
                val inputState = StateListDrawable()
                inputState.addState(intArrayOf(android.R.attr.state_focused), userInputEnabledDrawable)
                inputState.addState(intArrayOf(), userInputDisabledDrawable)
                itemView.userInput.background = inputState
            }
        }

        // shows correct option
        private fun showCorrectOptionWithBackground(option: Option){
            if(!isCorrect){
                showUserVotesWithBackground(option)
            }else{
                itemView.userInput.visibility = View.GONE
            }
            itemView.userInput.isFocusable = false
            itemView.userInput.isFocusableInTouchMode = false
            itemView.correct_answer.visibility = View.VISIBLE
            itemView.correct_answer.isFocusable = false
            itemView.correct_answer.isFocusableInTouchMode = false
            itemView.correct_answer.setText(option.correct_number.toString())
            itemView.correct_answer.background = ContextCompat.getDrawable(itemView.context, R.drawable.correct_background)
        }

        // show user votes with background
        private fun showUserVotesWithBackground(option: Option){
            if (option.number != null) {
                itemView.userInput.visibility = View.VISIBLE
                itemView.userInput.setText(option.number.toString())
                itemView.userInput.background = (ContextCompat.getDrawable(itemView.context, R.drawable.wrong_background))
                itemView.userInput.setTextColor(ContextCompat.getColor(itemView.context, R.color.livelike_number_prediction_wrong_answer))

            }else{
                itemView.userInput.background = (ContextCompat.getDrawable(itemView.context, R.drawable.increment_decrement_input_box))
                itemView.userInput.setTextColor(ContextCompat.getColor(itemView.context, R.color.livelike_number_prediction_user_input_hint))
            }
        }
    }

    // used for enabling/disabling button when all options are entered
    internal interface EnableSubmitListener {
        fun onSubmitEnabled(isSubmitBtnEnabled: Boolean)
    }

    // restores interacted data
    fun restoreSelectedVotes(options: List<NumberPredictionVotes>?) {
        options?.let {
            this.selectedUserVotes =  options.toMutableList()
             if(options.isNotEmpty()){
               for (i in myDataset.indices) {
                 if (myDataset[i].id == options[i].optionId) myDataset[i].number = options[i].number
               }
            }
        }
    }

}