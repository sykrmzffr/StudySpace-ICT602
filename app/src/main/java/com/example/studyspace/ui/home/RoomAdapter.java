package com.example.studyspace.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studyspace.R;
import com.example.studyspace.data.model.Room;
import com.example.studyspace.databinding.ItemRoomBinding;

import java.util.List;

/**
 * RoomAdapter — RecyclerView Adapter for the Home Screen
 * Tutorial Reference: T02 — Layout (RecyclerView)
 */
public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private List<Room> roomList;
    private final OnRoomClickListener onRoomClick;

    public interface OnRoomClickListener {
        void onRoomClick(Room room);
    }

    public RoomAdapter(List<Room> roomList, OnRoomClickListener onRoomClick) {
        this.roomList = roomList;
        this.onRoomClick = onRoomClick;
    }

    public void updateData(List<Room> newList) {
        this.roomList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRoomBinding binding = ItemRoomBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new RoomViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = roomList.get(position);
        holder.bind(room);
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    class RoomViewHolder extends RecyclerView.ViewHolder {

        private final ItemRoomBinding binding;

        RoomViewHolder(ItemRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(final Room room) {
            binding.tvRoomName.setText(room.getName());
            binding.tvLocation.setText(room.getLocation());
            binding.tvCapacity.setText("Capacity: " + room.getCapacity() + " people");

            Context context = binding.getRoot().getContext();
            if (room.isAvailable()) {
                binding.tvStatus.setText(context.getString(R.string.status_available));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_available);
            } else {
                binding.tvStatus.setText(context.getString(R.string.status_booked));
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_booked);
            }

            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRoomClick.onRoomClick(room);
                }
            });
        }
    }
}
