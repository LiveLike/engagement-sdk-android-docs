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
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.databinding.WidgetNumberPredictionItemBinding
import com.livelike.engagementsdk.widget.NumberPredictionOptionsTheme
import com.livelike.engagementsdk.widget.ViewStyleProps
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Option


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
    var selectedPosition = RecyclerView.NO_POSITION
    var selectedUserVotes = mutableListOf<NumberPredictionVotes>()
    var binding:WidgetNumberPredictionItemBinding? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = WidgetNumberPredictionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OptionViewHolder(binding)
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


    inner class OptionViewHolder(val binding: WidgetNumberPredictionItemBinding) :
        RecyclerView.ViewHolder(binding.root){


        fun setData(
            option: Option,
            widgetType: WidgetType,
            component: NumberPredictionOptionsTheme?,
            fontFamilyProvider: FontFamilyProvider?
        ) {
            if (!option.image_url.isNullOrEmpty()) {
                Glide.with(itemView.context.applicationContext)
                    .load(option.image_url)
                    .into(binding.imgView)
                binding.imgView.visibility = View.VISIBLE
            } else {
                binding.imgView.visibility = View.GONE
            }

            binding.description.text = option.description
            binding.incrementDecrementLayout.userInput.setTextColor(ContextCompat.getColor(itemView.context, R.color.livelike_number_prediction_user_input))

            if (option.number != null) {
                binding.incrementDecrementLayout.userInput.apply{
                    setText(option.number.toString())
                    isFocusableInTouchMode = false
                    isCursorVisible = false
                }
            }


            if(selectionLocked){
                binding.incrementDecrementLayout.userInput.apply {
                    isFocusableInTouchMode = false
                    isCursorVisible = false
                }
            }


            if(widgetType == WidgetType.IMAGE_NUMBER_PREDICTION_FOLLOW_UP ||
                widgetType == WidgetType.TEXT_NUMBER_PREDICTION_FOLLOW_UP){
                binding.correctAnswer.visibility = View.VISIBLE
                showCorrectPrediction(option)

            }else{
                binding.correctAnswer.visibility = View.GONE //this shows only for followup
            }

            setListeners()
            setItemBackground(component, fontFamilyProvider,widgetType)

        }

        // add/removes votes for submission
        private fun addRemovePredictionData() {
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
                if(myDataset[adapterPosition].number != null) {
                    userVote.number = myDataset[adapterPosition].number!!
                }else{
                    selectedUserVotes.remove(userVote)
                }
            }
            needToEnableSubmit = selectedUserVotes.size == myDataset.size
            submitListener?.onSubmitEnabled(needToEnableSubmit)
        }


        // set listeners
        @SuppressLint("ClickableViewAccessibility")
        private fun setListeners(){
            binding.incrementDecrementLayout.userInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(arg0: Editable) {
                    if (adapterPosition == RecyclerView.NO_POSITION || selectionLocked) return
                    val value = arg0.toString()
                    if(value.isEmpty()){
                        selectedPosition = adapterPosition
                        myDataset[selectedPosition].number = null
                    }else{
                        try {
                            selectedPosition = adapterPosition
                            myDataset[selectedPosition].number = value.toInt()
                        } catch (ex: NumberFormatException) {
                            logError { "Invalid input" }
                            return
                        }
                    }
                    addRemovePredictionData()
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


            binding.incrementDecrementLayout.userInput.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val imm =
                        v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    binding.incrementDecrementLayout.userInput.apply {
                        isFocusable = false
                        isFocusableInTouchMode = true
                    }
                    true
                } else false
            }

            binding.incrementDecrementLayout.userInput.setOnTouchListener { v, _ -> // Disallow the touch request for parent scroll on touch of child view
                if ( binding.incrementDecrementLayout.userInput.text.toString().isNotEmpty()) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                false
            }
        }


        // this is mainly used for theming purpose
        private fun setItemBackground(
            layoutPickerComponent: NumberPredictionOptionsTheme?,
            fontFamilyProvider: FontFamilyProvider?,
            widgetType: WidgetType
        ) {

            val optionDescTheme: ViewStyleProps? = layoutPickerComponent?.option
            AndroidResource.updateThemeForView(binding.description, optionDescTheme, fontFamilyProvider)

            if (layoutPickerComponent?.option != null){
               binding.bkgrd.background = AndroidResource.createDrawable(layoutPickerComponent.option)
            }

            //input placeholder
            val optionInputPlaceholder: ViewStyleProps? = layoutPickerComponent?.optionInputFieldPlaceholder
            AndroidResource.updateThemeForView(  binding.incrementDecrementLayout.userInput, optionInputPlaceholder, fontFamilyProvider)


            // user input with state
            if (layoutPickerComponent?.optionInputFieldEnabled?.background != null &&
                layoutPickerComponent.optionInputFieldDisabled?.background != null
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
                binding.incrementDecrementLayout.userInput.background = inputState
            }

            // followup correct/incorrect input background
            if(widgetType == WidgetType.IMAGE_NUMBER_PREDICTION_FOLLOW_UP ||
                widgetType == WidgetType.TEXT_NUMBER_PREDICTION_FOLLOW_UP) {

                    if(layoutPickerComponent?.optionInputFieldCorrect?.background != null){
                        binding.correctAnswer.background =
                            AndroidResource.createDrawable(layoutPickerComponent.optionInputFieldCorrect)
                    }else{
                        binding.correctAnswer.background = (ContextCompat.getDrawable(itemView.context, R.drawable.correct_background))
                    }
                    if ( binding.incrementDecrementLayout.userInput.visibility == View.VISIBLE && layoutPickerComponent?.optionInputFieldIncorrect?.background != null)  binding.incrementDecrementLayout.userInput.background =
                        AndroidResource.createDrawable(layoutPickerComponent.optionInputFieldIncorrect)
            }
        }

        // shows correct predictions
        private fun showCorrectPrediction(option: Option){
            if(!isCorrect){
                showUserSelectedPrediction(option) //for wrong answer
            }else{
                binding.incrementDecrementLayout.userInput.visibility = View.GONE //this is only visible if correct answer is predicted
            }
            binding.incrementDecrementLayout.userInput.isFocusable = false
            binding.incrementDecrementLayout.userInput.isFocusableInTouchMode = false
            binding.correctAnswer.setText(option.correct_number.toString())
        }


        // show user prediction with background
        private fun showUserSelectedPrediction(option: Option){
            if (option.number != null) {
                binding.incrementDecrementLayout.userInput.apply {
                    visibility =  View.VISIBLE
                    setText(option.number.toString())
                    background = (ContextCompat.getDrawable(itemView.context, R.drawable.wrong_background))
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.livelike_number_prediction_wrong_answer))
                }
            }else{
                binding.incrementDecrementLayout.userInput.apply {
                    background = (ContextCompat.getDrawable(itemView.context, R.drawable.user_input_background))
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.livelike_number_prediction_user_input_hint))
                }
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