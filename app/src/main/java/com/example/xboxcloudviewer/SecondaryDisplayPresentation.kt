package com.example.xboxcloudviewer

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.xboxcloudviewer.models.UserProfile
import com.example.xboxcloudviewer.services.AccountManager
import com.example.xboxcloudviewer.services.XboxProfileService
import com.example.xboxcloudviewer.ui.AccountsView
import com.example.xboxcloudviewer.ui.ProfileEditView

class SecondaryDisplayPresentation(outerContext: Context, display: Display) : 
    Presentation(outerContext, display, R.style.Theme_XboxCloudViewer) {

    var onProfileSwitchRequested: ((UserProfile) -> Unit)? = null
    
    private lateinit var accountManager: AccountManager
    
    // UI Elements
    private lateinit var headerWelcome: TextView
    private lateinit var headerAvatar: ImageView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Empêcher l'écran secondaire de "voler" le focus de la manette
        // On retire FLAG_NOT_TOUCHABLE pour permettre le clic tactile sur le bouton de paramètres
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        
        setContentView(R.layout.layout_secondary_screen)
        
        accountManager = AccountManager(context)

        val btnSettings = findViewById<ImageButton>(R.id.btn_settings)
        val settingsView = findViewById<SettingsView>(R.id.settings_view)
        
        val btnAccount = findViewById<LinearLayout>(R.id.btn_account)
        headerWelcome = findViewById(R.id.header_welcome)
        headerAvatar = findViewById(R.id.header_avatar)
        
        val accountsView = findViewById<AccountsView>(R.id.accounts_view)
        val profileEditView = findViewById<ProfileEditView>(R.id.profile_edit_view)

        // Setup Accounts View
        accountsView.setup(accountManager)
        accountsView.onCloseClicked = {
            accountsView.visibility = View.GONE
        }
        
        accountsView.onEditUser = { user ->
            profileEditView.setup(accountManager, user)
            profileEditView.visibility = View.VISIBLE
        }
        
        accountsView.onSwitchUser = { user ->
            onProfileSwitchRequested?.invoke(user)
            accountsView.visibility = View.GONE
            updateHeaderInfo()
        }
        
        // Setup Profile Edit View
        profileEditView.onBackClicked = {
            profileEditView.visibility = View.GONE
        }
        
        profileEditView.onSaveComplete = {
            profileEditView.visibility = View.GONE
            accountsView.refreshList()
            updateHeaderInfo()
        }

        // Header click
        btnAccount.setOnClickListener {
            accountsView.refreshList()
            accountsView.visibility = View.VISIBLE
        }

        btnSettings?.setOnClickListener {
            settingsView?.visibility = View.VISIBLE
        }

        settingsView?.onCloseClicked = {
            settingsView?.visibility = View.GONE
        }
        
        updateHeaderInfo()
    }
    
    fun updateHeaderInfo() {
        val user = accountManager.getCurrentUser()
        headerWelcome.text = "Bienvenue ${user.name}"
        if (user.avatarUrl != null) {
            XboxProfileService.loadImageInto(user.avatarUrl!!, headerAvatar)
        } else {
            headerAvatar.setImageResource(R.drawable.ic_person)
        }
    }
}
