package com.example.xboxcloudviewer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.view.inputmethod.InputMethodManager
import android.os.Handler
import android.os.Looper
import androidx.core.widget.addTextChangedListener
import com.example.xboxcloudviewer.R
import com.example.xboxcloudviewer.models.UserProfile
import com.example.xboxcloudviewer.services.AccountManager
import com.example.xboxcloudviewer.services.XboxProfileService
import java.util.UUID
import kotlin.concurrent.thread

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

        val typingHandler = Handler(Looper.getMainLooper())
        var typingRunnable: Runnable? = null

        inputName.addTextChangedListener { text ->
            val newName = text.toString().trim()
            
            // Annuler l'ancienne requête si l'utilisateur continue de taper
            typingRunnable?.let { typingHandler.removeCallbacks(it) }

            if (newName.isNotEmpty()) {
                // Créer un délai de 800ms avant de charger l'image générée (anti-spam / debounce)
                typingRunnable = Runnable {
                    val fallbackUrl = XboxProfileService.getFallbackAvatarUrl(newName)
                    XboxProfileService.loadImageInto(fallbackUrl, avatarImage)
                }
                typingHandler.postDelayed(typingRunnable!!, 2000)
            } else {
                avatarImage.setImageResource(R.drawable.ic_person)
            }
        }

        btnSave.setOnClickListener {
            val finalName = inputName.text.toString().trim()
            if (finalName.isNotEmpty()) {
                // Désactiver le bouton pendant le chargement
                btnSave.isEnabled = false
                btnSave.text = "Searching Xbox Live..."

                thread {
                    val realAvatar = XboxProfileService.resolveRealXboxAvatarUrl(finalName)
                    val finalAvatarUrl = realAvatar ?: XboxProfileService.getFallbackAvatarUrl(finalName)
                    
                    Handler(Looper.getMainLooper()).post {
                        val user = currentUserToEdit ?: UserProfile(UUID.randomUUID().toString(), finalName)
                        user.name = finalName
                        user.avatarUrl = finalAvatarUrl
                        accountManager.saveUser(user)
                        
                        btnSave.isEnabled = true
                        btnSave.text = "Save Profile"
                        onSaveComplete?.invoke()
                    }
                }
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
        
        // Focus automatique et ouverture du clavier
        inputName.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(inputName, InputMethodManager.SHOW_IMPLICIT)
    }
}