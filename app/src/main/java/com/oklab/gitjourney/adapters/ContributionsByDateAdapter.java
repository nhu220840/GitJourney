package com.oklab.gitjourney.adapters;

import android.content.Context;
import android.database.Cursor;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.oklab.gitjourney.R;
import com.oklab.gitjourney.data.ContributionsDataLoader;
import com.oklab.gitjourney.utils.Utils;

import java.text.DateFormat;
import java.util.Calendar;

public class ContributionsByDateAdapter extends RecyclerView.Adapter<ContributionsByDateAdapter.ContributionsByDateViewHolder> {

    private static final String TAG = ContributionsByDateAdapter.class.getSimpleName();
    private final Context context;
    private final Cursor cursor;

    public ContributionsByDateAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @Override
    public ContributionsByDateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contributions_list_item, parent, false);
        return new ContributionsByDateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ContributionsByDateViewHolder holder, int position) {
        cursor.moveToPosition(position);
        holder.populateContributionData();
    }

    @Override
    public long getItemId(int position) {
        cursor.moveToPosition(position);
        return cursor.getLong(ContributionsDataLoader.Query._ID);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public class ContributionsByDateViewHolder extends RecyclerView.ViewHolder {

        private TextView date;
        private TextView title;
        private TextView description;

        public ContributionsByDateViewHolder(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            description = (TextView) v.findViewById(R.id.action_desc);
            date = (TextView) v.findViewById(R.id.date);
        }

        private void populateContributionData() {
            Log.v(TAG, "populateContributionData");
            title.setText(cursor.getString(ContributionsDataLoader.Query.TITLE));
            description.setText(cursor.getString(ContributionsDataLoader.Query.DESCRIPTION));
            Calendar calendar = Calendar.getInstance();
            DateFormat formatter = Utils.createDateFormatterWithTimeZone(context, Utils.DMY_DATE_FORMAT_PATTERN);
            long date = cursor.getLong(ContributionsDataLoader.Query.PUBLISHED_DATE);
            calendar.setTimeInMillis(date);
            this.date.setText(formatter.format(calendar.getTime()));
        }
    }
}