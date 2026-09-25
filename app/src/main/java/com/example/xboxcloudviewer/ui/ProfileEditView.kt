package com.example.xboxcloudviewer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import com.example.xboxcloudviewer.R
import com.example.xboxcloudviewer.models.UserProfile
import com.example.xboxcloudviewer.services.AccountManager
import com.example.xboxcloudviewer.services.XboxProfileService
import java.util.UUID

class ProfileEditView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onBackClicked: (() -> Unit)? = null
    var onSaveComplete: (() -> Unit)? = null
    private var currentUserToEdit: UserProfile? = null
    private lateinit var accountManager: AccountManager

    private val inputName: EditText
    private val avatarImage: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_profile_edit_view, this, true)

        val btnBack = findViewById<ImageButton>(R.id.btn_back_accounts)
        val btnSave = findViewById<Button>(R.id.btn_save_profile)
        inputName = findViewById(R.id.profile_name_input)
        avatarImage = findViewById(R.id.profile_avatar)

        btnBack.setOnClickListener { onBackClicked?.invoke() }

        inputName.addTextChangedListener { text ->
            val newName = text.toString().trim()
            if (newName.isNotEmpty()) {
                val avatarUrl = XboxProfileService.getAvatarUrlForName(newName)
                XboxProfileService.loadImageInto(avatarUrl, avatarImage)
            } else {
                avatarImage.setImageResource(R.drawable.ic_person)
            }
        }

        btnSave.setOnClickListener {
            val finalName = inputName.text.toString().trim()
            if (finalName.isNotEmpty()) {
                val user = currentUserToEdit ?: UserProfile(UUID.randomUUID().toString(), finalName)
                user.name = finalName
                user.avatarUrl = XboxProfileService.getAvatarUrlForName(finalName)
                accountManager.saveUser(user)
                onSaveComplete?.invoke()
            }
        }
    }

    fun setup(accountManager: AccountManager, userToEdit: UserProfile? = null) {
        this.accountManager = accountManager
        this.currentUserToEdit = userToEdit

        if (userToEdit != null) {
            inputName.setText(userToEdit.name)
            if (userToEdit.avatarUrl != null) {
                XboxProfileService.loadImageInto(userToEdit.avatarUrl!!, avatarImage)
            }
        } else {
            inputName.setText("")
            avatarImage.setImageResource(R.drawable.ic_person)
        }
    }
}