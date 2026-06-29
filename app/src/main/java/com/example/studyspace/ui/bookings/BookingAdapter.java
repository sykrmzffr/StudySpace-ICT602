package com.example.studyspace.ui.bookings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studyspace.R;
import com.example.studyspace.data.model.Booking;
import com.example.studyspace.databinding.ItemBookingBinding;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookings = new ArrayList<>();
    private final BookingClickListener listener;

    public interface BookingClickListener {
        void onCheckInClick(Booking booking);
        void onCancelClick(Booking booking);
    }

    public BookingAdapter(BookingClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Booking> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBookingBinding binding = ItemBookingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new BookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        holder.bind(bookings.get(position));
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {

        private final ItemBookingBinding binding;

        BookingViewHolder(ItemBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(final Booking booking) {
            binding.tvRoomName.setText(booking.getRoomName());
            binding.tvDate.setText(booking.getDate());
            binding.tvTime.setText(booking.getStartTime() + " - " + booking.getEndTime());
            binding.tvStatus.setText(booking.getStatus().toUpperCase());

            if (booking.getStatus().equalsIgnoreCase("Confirmed") || booking.getStatus().equalsIgnoreCase("Checked In")) {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_available);
                binding.tvStatus.setTextColor(binding.getRoot().getContext().getColor(android.R.color.white));
            } else {
                binding.tvStatus.setBackgroundResource(android.R.color.darker_gray);
            }

            // Phase 5: Check-in button logic
            if (booking.getStatus().equalsIgnoreCase("Checked In")) {
                binding.btnCheckIn.setVisibility(View.GONE);
                binding.btnCancel.setVisibility(View.GONE);
            } else {
                binding.btnCheckIn.setVisibility(View.VISIBLE);
                binding.btnCancel.setVisibility(View.VISIBLE);
                
                binding.btnCheckIn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onCheckInClick(booking);
                    }
                });
                
                binding.btnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onCancelClick(booking);
                    }
                });
            }
        }
    }
}
