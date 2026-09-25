package com.example.xboxcloudviewer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.xboxcloudviewer.R
import com.example.xboxcloudviewer.models.UserProfile
import com.example.xboxcloudviewer.services.AccountManager
import com.example.xboxcloudviewer.services.XboxProfileService

class AccountsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onCloseClicked: (() -> Unit)? = null
    var onEditUser: ((UserProfile?) -> Unit)? = null
    var onSwitchUser: ((UserProfile) -> Unit)? = null
    
    private lateinit var accountManager: AccountManager
    private val usersContainer: LinearLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_accounts_view, this, true)

        val btnClose = findViewById<ImageButton>(R.id.btn_close_accounts)
        val btnAddUser = findViewById<Button>(R.id.btn_add_user)
        usersContainer = findViewById(R.id.users_container)

        btnClose.setOnClickListener { onCloseClicked?.invoke() }
        btnAddUser.setOnClickListener { onEditUser?.invoke(null) } // null means new user
    }

    fun setup(accountManager: AccountManager) {
        this.accountManager = accountManager
        refreshList()
    }

    fun refreshList() {
        usersContainer.removeAllViews()
        val users = accountManager.getUsers()
        val currentUserId = accountManager.getCurrentUserId()

        val inflater = LayoutInflater.from(context)
        for (user in users) {
            val itemView = inflater.inflate(R.layout.item_user_profile, usersContainer, false)
            
            val avatar = itemView.findViewById<ImageView>(R.id.item_avatar)
            val name = itemView.findViewById<TextView>(R.id.item_name)
            val activeBadge = itemView.findViewById<TextView>(R.id.item_active_badge)
            val btnEdit = itemView.findViewById<ImageButton>(R.id.item_edit)

            name.text = user.name
            if (user.avatarUrl != null) {
                XboxProfileService.loadImageInto(user.avatarUrl!!, avatar)
            }
            
            if (user.id == currentUserId) {
                activeBadge.visibility = VISIBLE
            } else {
                activeBadge.visibility = GONE
            }

            btnEdit.setOnClickListener {
                onEditUser?.invoke(user)
            }

            itemView.setOnClickListener {
                if (user.id != currentUserId) {
                    onSwitchUser?.invoke(user)
                }
            }

            usersContainer.addView(itemView)
        }
    }
}