package com.rr.CPing.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rr.CPing.R;
import com.rr.CPing.SharedPref.SharedPrefConfig;
import com.rr.CPing.model.BottomSheetHandler;
import com.rr.CPing.model.ContestDetails;
import com.rr.CPing.model.DateTimeHandler;

import java.util.ArrayList;
import java.util.Calendar;

public class ContestDetailsRecyclerViewAdapter extends RecyclerView.Adapter {

    private final ArrayList<ContestDetails> contestDetailsArrayList;
    private final DateTimeHandler dateTimeHandler;
    Context context;
    private ContestDetailsRecyclerViewAdapter.OnItemClickListener itemClickListener;

    public ContestDetailsRecyclerViewAdapter(ArrayList<ContestDetails> contestDetailsArrayList) {
        this.contestDetailsArrayList = contestDetailsArrayList;
        dateTimeHandler = new DateTimeHandler();
    }

    public void setOnItemClickListener(ContestDetailsRecyclerViewAdapter.OnItemClickListener listener) {
        itemClickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.contest_details_item, parent, false);
        return new MyViewHolder(view, itemClickListener);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ContestDetailsRecyclerViewAdapter.MyViewHolder myViewHolder = (MyViewHolder) holder;

        ContestDetails contest = contestDetailsArrayList.get(position);
        Calendar start = Calendar.getInstance();
        dateTimeHandler.setCalender(start, contest.getContestStartTime());

        myViewHolder.name.setText(contest.getContestName());

        String text = "On: " + dateTimeHandler.getDate(start);
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        myViewHolder.startDate.setText(spannableString);

        text = "At: " + new BottomSheetHandler().hr_24To12Format(new StringBuilder(dateTimeHandler.getTime(start)));
        spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        myViewHolder.startTime.setText(spannableString);

        text = "Duration: " + findDuration(contest.getContestDuration());
        spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        myViewHolder.duration.setText(spannableString);

        if (contest.getContestStatus().equals("CODING"))
            myViewHolder.remainderIcon.setImageResource(R.drawable.ic_contest_running);
        else {
            ArrayList<Pair<String, Long>> currentList = SharedPrefConfig.readInIdsOfReminderContests(context);
            if (currentList.size() != 0 && isContains(currentList, contestDetailsArrayList.get(position).getContestName()))
                myViewHolder.remainderIcon.setImageResource(R.drawable.ic_reminder_added);
            else myViewHolder.remainderIcon.setImageResource(R.drawable.ic_add_reminder);
        }
    }

    private boolean isContains(ArrayList<Pair<String, Long>> currentList, String contestName) {
        for (Pair<String, Long> p : currentList) {
            if (p.first.equals(contestName)) return true;
        }
        return false;
    }

    private String findDuration(int contestDuration) {
        String result = "";
        result += contestDuration / 3600 + ":";
        if (contestDuration % 3600 == 0) result += "00 hrs";
        else {
            int minutes = (int) (((contestDuration / 3600.0) - (contestDuration / 3600)) * 60);
            result += minutes + " hrs";
        }
        return result;
    }

    @Override
    public int getItemCount() {
        return contestDetailsArrayList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(String platFormName, int position);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView name, startDate, startTime, duration;
        ImageView remainderIcon;

        public MyViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null) {
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onItemClick("null", pos);
                    }
                }
            });

            name = itemView.findViewById(R.id.contestName);
            startDate = itemView.findViewById(R.id.contestStartDate);
            startTime = itemView.findViewById(R.id.contestStartTime);
            duration = itemView.findViewById(R.id.contestDuration);
            remainderIcon = itemView.findViewById(R.id.remainder_imageView);
        }
    }
}
