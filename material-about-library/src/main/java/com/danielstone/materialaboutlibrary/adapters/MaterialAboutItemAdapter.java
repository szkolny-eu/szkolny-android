package com.danielstone.materialaboutlibrary.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.danielstone.materialaboutlibrary.holders.MaterialAboutItemViewHolder;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager;
import com.danielstone.materialaboutlibrary.util.ViewTypeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaterialAboutItemAdapter extends RecyclerView.Adapter<MaterialAboutItemViewHolder> {

    private final AsyncListDiffer<MaterialAboutItem> differ = new AsyncListDiffer<MaterialAboutItem>(this, DIFF_CALLBACK);

    private ViewTypeManager viewTypeManager;

    private Context context;

    private List<MaterialAboutItem> data = new ArrayList<>();
    private List<MaterialAboutItem> oldData = new ArrayList<>();

    MaterialAboutItemAdapter() {
        setHasStableIds(true);
        this.viewTypeManager = new DefaultViewTypeManager();
    }

    MaterialAboutItemAdapter(ViewTypeManager customViewTypeManager) {
        //Log.d("MaterialItem", "Created adapter");
        setHasStableIds(true);
        this.viewTypeManager = customViewTypeManager;
    }

    @NonNull
    @Override
    public MaterialAboutItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        context = viewGroup.getContext();
        if (!(viewGroup instanceof RecyclerView)) {
            throw new RuntimeException("Not bound to RecyclerView");
        }

        int layoutId = viewTypeManager.getLayout(viewType);

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false);
        view.setFocusable(true);

        return viewTypeManager.getViewHolder(viewType, view);
    }

    @Override
    public void onBindViewHolder(MaterialAboutItemViewHolder holder, int position) {
        /*if (data.get(position) instanceof MaterialAboutSwitchItem) {
            Log.d("MaterialAdapter", "Item "+((MaterialAboutSwitchItem) data.get(position)).getText()+" checked "+((MaterialAboutSwitchItem) data.get(position)).getChecked());
        }*/
        //Log.d("MaterialItem", "Binding "+data.get(position).getDetailString());
        viewTypeManager.setupItem(getItemViewType(position), holder, data.get(position), context);
    }


    @Override
    public long getItemId(int position) {
        return UUID.fromString(data.get(position).getId()).getMostSignificantBits() & Long.MAX_VALUE;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).getType();
    }

    public void setData(ArrayList<MaterialAboutItem> data) {
        this.data = data;

        notifyDataSetChanged();
        // diff this with previous data
        /*DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffCallback(oldData, data));
        try {
            diffResult.dispatchUpdatesTo(this);
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }

        // clone the new data to make further diffs
        oldData = new ArrayList<>();
        for (MaterialAboutItem item : data) {
            oldData.add(item.clone());
        }*/

        /*List<MaterialAboutItem> newData = new ArrayList<>();
        for (MaterialAboutItem item : data) {
            newData.add(item.clone());
        }
        differ.submitList(newData);*/
    }

    public List<MaterialAboutItem> getData() {
        return data;
    }


    public static final DiffUtil.ItemCallback<MaterialAboutItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<MaterialAboutItem>() {
        @Override
        public boolean areItemsTheSame(MaterialAboutItem oldItem, MaterialAboutItem newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(MaterialAboutItem oldItem, MaterialAboutItem newItem) {
            return oldItem.getDetailString().equals(newItem.getDetailString());
        }
    };

    public class MyDiffCallback extends DiffUtil.Callback{

        List<MaterialAboutItem> oldPersons;
        List<MaterialAboutItem> newPersons;

        public MyDiffCallback(List<MaterialAboutItem> newPersons, List<MaterialAboutItem> oldPersons) {
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
            return oldPersons.get(oldItemPosition).getDetailString().equals(newPersons.get(newItemPosition).getDetailString());
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            //you can return particular field for changed item.
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }

}
