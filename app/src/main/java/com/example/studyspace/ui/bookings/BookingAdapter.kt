package com.example.studyspace.ui.bookings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studyspace.data.model.Booking
import com.example.studyspace.databinding.ItemBookingBinding

class BookingAdapter(
    private val onCheckInClicked: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private var bookings: List<Booking> = emptyList()

    fun updateData(newBookings: List<Booking>) {
        this.bookings = newBookings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount() = bookings.size

    inner class BookingViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.tvRoomName.text = booking.roomName
            binding.tvDate.text = booking.date
            binding.tvTime.text = "${booking.startTime} - ${booking.endTime}"
            binding.tvStatus.text = booking.status.uppercase()

            if (booking.status.equals("Confirmed", true) || booking.status.equals("Checked In", true)) {
                binding.tvStatus.setBackgroundResource(com.example.studyspace.R.drawable.bg_status_available)
                binding.tvStatus.setTextColor(binding.root.context.getColor(android.R.color.white))
            } else {
                binding.tvStatus.setBackgroundResource(android.R.color.darker_gray)
            }

            // Phase 5: Check-in button logic
            if (booking.status.equals("Checked In", true)) {
                binding.btnCheckIn.visibility = View.GONE
            } else {
                binding.btnCheckIn.visibility = View.VISIBLE
                binding.btnCheckIn.setOnClickListener {
                    onCheckInClicked(booking)
                }
            }
        }
    }
}
