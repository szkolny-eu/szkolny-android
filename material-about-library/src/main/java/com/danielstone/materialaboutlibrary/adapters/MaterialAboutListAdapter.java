package com.danielstone.materialaboutlibrary.adapters;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.danielstone.materialaboutlibrary.R;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager;
import com.danielstone.materialaboutlibrary.util.ViewTypeManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaterialAboutListAdapter extends RecyclerView.Adapter<MaterialAboutListAdapter.MaterialAboutListViewHolder> {

    private final AsyncListDiffer<MaterialAboutCard> differ = new AsyncListDiffer<MaterialAboutCard>(this, DIFF_CALLBACK);

    private ViewTypeManager viewTypeManager;

    private Context context;

    private List<MaterialAboutCard> data = new ArrayList<>();
    private List<MaterialAboutCard> oldData = new ArrayList<>();

    public MaterialAboutListAdapter() {
        setHasStableIds(true);
        this.viewTypeManager = new DefaultViewTypeManager();
    }

    public MaterialAboutListAdapter(ViewTypeManager customViewTypeManager) {
        //Log.d("MaterialList", "Created list adapter");
        setHasStableIds(true);
        this.viewTypeManager = customViewTypeManager;
    }

    @Override
    public MaterialAboutListViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        context = viewGroup.getContext();
        if (viewGroup instanceof RecyclerView) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.mal_material_about_list_card, viewGroup, false);
            view.setFocusable(true);
            return new MaterialAboutListViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }

    @Override
    public void onBindViewHolder(MaterialAboutListViewHolder holder, int position) {
        MaterialAboutCard card = data.get(position);

        if (holder.cardView instanceof CardView) {
            CardView cardView = (CardView) holder.cardView;
            int cardColor = card.getCardColor();
            if (cardColor != 0) {
                cardView.setCardBackgroundColor(cardColor);
            }
        }

        CharSequence title = card.getTitle();
        int titleRes = card.getTitleRes();

        holder.title.setVisibility(View.VISIBLE);
        if (title != null) {
            holder.title.setText(title);
        } else if (titleRes != 0) {
            holder.title.setText(titleRes);
        } else {
            holder.title.setVisibility(View.GONE);
        }

        int titleColor = card.getTitleColor();

        if (holder.title.getVisibility() == View.VISIBLE) {
            if (titleColor != 0) {
                holder.title.setTextColor(titleColor);
            } else {
                holder.title.setTextColor(holder.title.getTextColors().getDefaultColor());
            }
        }

        if (card.getCustomAdapter() != null) {
            holder.useCustomAdapter(card.getCustomAdapter());
        } else {
            holder.useMaterialAboutItemAdapter();
            ((MaterialAboutItemAdapter) holder.adapter).setData(card.getItems());
        }
    }

    @Override
    public long getItemId(int position) {
        return UUID.fromString(data.get(position).getId()).getMostSignificantBits() & Long.MAX_VALUE;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(ArrayList<MaterialAboutCard> data) {
        this.data = data;

        notifyDataSetChanged();

        /*// diff this with previous data
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffCallback(oldData, data));
        try {
            diffResult.dispatchUpdatesTo(this);
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }

        // clone the new data to make further diffs
        oldData = new ArrayList<>();
        for (MaterialAboutCard card : data) {
            oldData.add(card.clone());
        }*/
        //differ.submitList(newData);
    }


    List<MaterialAboutCard> getData() {
        return data;
    }

    class MaterialAboutListViewHolder extends RecyclerView.ViewHolder {

        final View cardView;
        final TextView title;
        final RecyclerView recyclerView;
        RecyclerView.Adapter adapter;

        MaterialAboutListViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.mal_list_card);
            title = (TextView) view.findViewById(R.id.mal_list_card_title);
            recyclerView = (RecyclerView) view.findViewById(R.id.mal_card_recyclerview);
            adapter = new MaterialAboutItemAdapter(viewTypeManager);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(adapter);
            recyclerView.setNestedScrollingEnabled(false);
        }

        public void useMaterialAboutItemAdapter() {
            if (!(adapter instanceof MaterialAboutItemAdapter)) {
                adapter = new MaterialAboutItemAdapter(viewTypeManager);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setAdapter(adapter);
            }
        }

        public void useCustomAdapter(RecyclerView.Adapter newAdapter) {
            if (adapter instanceof MaterialAboutItemAdapter) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setAdapter(newAdapter);
            }
        }
    }


    public static final DiffUtil.ItemCallback<MaterialAboutCard> DIFF_CALLBACK = new DiffUtil.ItemCallback<MaterialAboutCard>() {
        @Override
        public boolean areItemsTheSame(MaterialAboutCard oldItem, MaterialAboutCard newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(MaterialAboutCard oldCard, MaterialAboutCard newCard) {
            boolean result;
            result = oldCard.toString().equals(newCard.toString());
            if (oldCard.getItems().size() != newCard.getItems().size()) return false;
            for (int i = 0; i < oldCard.getItems().size(); i++) {
                if (!oldCard.getItems().get(i).getDetailString().equals(newCard.getItems().get(i).getDetailString())) return false;
            }
            return result;
        }
    };

    public class MyDiffCallback extends DiffUtil.Callback{

        List<MaterialAboutCard> oldPersons;
        List<MaterialAboutCard> newPersons;

        public MyDiffCallback(List<MaterialAboutCard> newPersons, List<MaterialAboutCard> oldPersons) {
            this.newPersons = newPersons;
            this.oldPersons = oldPersons;
        }

        @Override
        public int getOldListSize() {
            return oldPersons.size();
        }

        @Override
        public int getNewListSize() {
            return newPersons.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldPersons.get(oldItemPosition).getId().equals(newPersons.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            boolean result;
            result = oldPersons.get(oldItemPosition).toString().equals(newPersons.get(newItemPosition).toString());
            if (oldPersons.get(oldItemPosition).getItems().size() != newPersons.get(newItemPosition).getItems().size()) return false;
            for (int i = 0; i < oldPersons.get(oldItemPosition).getItems().size(); i++) {
                if (!oldPersons.get(oldItemPosition).getItems().get(i).getDetailString().equals(newPersons.get(newItemPosition).getItems().get(i).getDetailString())) return false;
            }
            return result;
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            //you can return particular field for changed item.
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}
