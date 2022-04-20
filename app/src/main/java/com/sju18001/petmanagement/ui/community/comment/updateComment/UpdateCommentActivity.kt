package com.sju18001.petmanagement.ui.community.comment.updateComment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ActivityUpdatecommentBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dto.UpdateCommentReqDto

class UpdateCommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdatecommentBinding

    private val viewModel: UpdateCommentViewModel by viewModels()
    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()

        isViewDestroyed = false
        supportActionBar?.hide()

        initializeViewModelByIntent()

        Util.setupViewsForHideKeyboard(this, binding.constraintlayoutParent)
        showKeyboard()
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_updatecomment)

        binding.lifecycleOwner = this
        binding.activity = this@UpdateCommentActivity
        binding.viewModel = viewModel
    }

    private fun initializeViewModelByIntent() {
        viewModel.contents.value = intent.getStringExtra("contents")
    }

    private fun showKeyboard() {
        val editTextUpdateComment = binding.edittextUpdatecomment
        editTextUpdateComment.postDelayed({
            Util.showKeyboard(this, editTextUpdateComment)
        }, 100)
    }


    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }


    /** Databinding functions */
    fun onClickBackButton() {
        finish()
    }

    fun updateComment(){
        val updateCommentReq = UpdateCommentReqDto(intent.getLongExtra("id", -1),
            binding.edittextUpdatecomment.text.toString())

        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .updateCommentReq(updateCommentReq)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            setResult(RESULT_OK, makeResultIntent())
            finish()

            Toast.makeText(baseContext, getText(R.string.update_comment_success), Toast.LENGTH_SHORT).show()
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }

    private fun makeResultIntent(): Intent {
        val resultIntent = Intent()
        resultIntent.putExtra("contents", viewModel.contents.value)
        resultIntent.putExtra("position", intent.getIntExtra("position", -1))

        return resultIntent
    }
}