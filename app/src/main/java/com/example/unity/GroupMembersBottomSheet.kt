package com.example.unity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class GroupMembersBottomSheet(
    private val members: List<UserResponse>,
    private val currentUserId: Int,
    private val onAddMember: () -> Unit,
    private val onLeaveGroup: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_group_members_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvMembers = view.findViewById<RecyclerView>(R.id.rvMembers)
        rvMembers.layoutManager = LinearLayoutManager(context)
        rvMembers.adapter = GroupMemberAdapter(members, currentUserId)

        view.findViewById<MaterialButton>(R.id.btnAddMember).setOnClickListener {
            dismiss()
            onAddMember()
        }

        view.findViewById<MaterialButton>(R.id.btnLeaveGroup).setOnClickListener {
            dismiss()
            onLeaveGroup()
        }
    }
}
