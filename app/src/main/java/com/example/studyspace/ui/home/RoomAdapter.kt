package com.example.studyspace.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.studyspace.R
import com.example.studyspace.data.model.Room
import com.example.studyspace.databinding.ItemRoomBinding

/**
 * RoomAdapter — RecyclerView Adapter for the Home Screen
 * Tutorial Reference: T02 — Layout (RecyclerView)
 */
class RoomAdapter(
    private var roomList: List<Room>,
    private val onRoomClick: (Room) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    fun updateData(newList: List<Room>) {
        roomList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemRoomBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = roomList[position]
        holder.bind(room)
    }

    override fun getItemCount(): Int = roomList.size

    inner class RoomViewHolder(private val binding: ItemRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(room: Room) {
            binding.tvRoomName.text = room.name
            binding.tvLocation.text = room.location
            binding.tvCapacity.text = "Capacity: ${room.capacity} people"

            // T01 / T02 — Conditional formatting based on room availability
            val context = binding.root.context
            if (room.isAvailable) {
                binding.tvStatus.text = context.getString(R.string.status_available)
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_available)
            } else {
                binding.tvStatus.text = context.getString(R.string.status_booked)
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_booked)
            }

            // Handle click event to open room details
            binding.root.setOnClickListener {
                onRoomClick(room)
            }
        }
    }
}
