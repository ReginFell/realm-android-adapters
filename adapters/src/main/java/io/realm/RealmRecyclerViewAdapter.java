/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;

import java.util.List;

/**
 * The RealmBaseRecyclerAdapter class is an abstract utility class for binding RecyclerView UI elements to Realm data.
 * <p>
 * This adapter will automatically handle any updates to its data and call notifyDataSetChanged() as appropriate.
 * Currently there is no support for RecyclerView's data callback methods like notifyItemInserted(int), notifyItemRemoved(int),
 * notifyItemChanged(int) etc.
 * It means that, there is no possibility to use default data animations.
 * <p>
 * The RealmAdapter will stop receiving updates if the Realm instance providing the {@link OrderedRealmCollection} is
 * closed.
 *
 * @param <T>  type of {@link RealmModel} stored in the adapter.
 * @param <VH> type of RecyclerView.ViewHolder used in the adapter.
 */
public abstract class RealmRecyclerViewAdapter<T extends RealmModel & DiffEquals<T>, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements ListUpdateCallback {

    private final boolean hasAutoUpdates;
    private final RealmChangeListener realmChangeListener;

    private RealmResults<T> adapterData;
    private List<T> realmResultSnapshot;

    private final Realm realm;

    public RealmRecyclerViewAdapter(Context context, @NonNull RealmResults<T> data, boolean autoUpdate) {
        this.adapterData = data;
        this.hasAutoUpdates = autoUpdate;

        this.realm = Realm.getDefaultInstance();

        this.realmChangeListener = hasAutoUpdates ? getRealmChangeListener() : null;
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (hasAutoUpdates && isDataValid()) {
            addListener(adapterData);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (hasAutoUpdates && isDataValid()) {
            removeListener(adapterData);
        }
    }

    @Override
    public int getItemCount() {
        //noinspection ConstantConditions
        return isDataValid() ? adapterData.size() : 0;
    }


    /**
     * Returns the item associated with the specified position.
     * Can return {@code null} if provided Realm instance by {@link OrderedRealmCollection} is closed.
     *
     * @param index index of the item.
     * @return the item at the specified position, {@code null} if adapter data is not valid.
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public T getItem(int index) {
        //noinspection ConstantConditions
        return isDataValid() ? adapterData.get(index) : null;
    }

    /**
     * Returns data associated with this adapter.
     *
     * @return adapter data.
     */
    @Nullable
    public OrderedRealmCollection<T> getData() {
        return adapterData;
    }

    /**
     * Updates the data associated to the Adapter. Useful when the query has been changed.
     * If the query does not change you might consider using the automaticUpdate feature.
     *
     * @param data the new {@link OrderedRealmCollection} to display.
     */
    @SuppressWarnings("WeakerAccess")
    public void updateData(@Nullable RealmResults<T> data) {
        if (hasAutoUpdates) {
            if (adapterData != null) {
                removeListener(adapterData);
            }
            if (data != null) {
                addListener(data);
            }
        }

        this.adapterData = data;
        notifyDataSetChanged();
    }

    @SuppressWarnings("unchecked")
    private void addListener(@NonNull OrderedRealmCollection<T> data) {
        if (data instanceof RealmResults) {
            RealmResults realmResults = (RealmResults) data;
            realmResults.addChangeListener(realmChangeListener);
        } else if (data instanceof RealmList) {
            RealmList realmList = (RealmList) data;
            realmList.realm.handlerController.addChangeListenerAsWeakReference(realmChangeListener);
        } else {
            throw new IllegalArgumentException("RealmCollection not supported: " + data.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    private void removeListener(@NonNull OrderedRealmCollection<T> data) {
        if (data instanceof RealmResults) {
            RealmResults realmResults = (RealmResults) data;
            realmResults.removeChangeListener(realmChangeListener);
        } else if (data instanceof RealmList) {
            RealmList realmList = (RealmList) data;
            realmList.realm.handlerController.removeWeakChangeListener(realmChangeListener);
        } else {
            throw new IllegalArgumentException("RealmCollection not supported: " + data.getClass());
        }
    }

    private boolean isDataValid() {
        return adapterData != null && adapterData.isValid();
    }

    private RealmChangeListener getRealmChangeListener() {
        return new RealmChangeListener<RealmResults<T>>() {
            @Override
            public void onChange(RealmResults<T> element) {
                if (realmResultSnapshot != null && !realmResultSnapshot.isEmpty()) {
                    if (adapterData.isEmpty()) {
                        realmResultSnapshot = realm.copyFromRealm(adapterData);
                        notifyDataSetChanged();
                        return;
                    }
                    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(difCallback);
                    diffResult.dispatchUpdatesTo((ListUpdateCallback) RealmRecyclerViewAdapter.this);
                    realmResultSnapshot = realm.copyFromRealm(adapterData);

                } else {
                    notifyDataSetChanged();
                    realmResultSnapshot = realm.copyFromRealm(adapterData);
                }
            }
        };
    }

    private final DiffUtil.Callback difCallback = new DiffUtil.Callback() {
        @Override
        public int getOldListSize() {
            return realmResultSnapshot.size();
        }

        @Override
        public int getNewListSize() {
            return adapterData.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return realmResultSnapshot.get(oldItemPosition).diffEquals(adapterData.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return realmResultSnapshot.get(oldItemPosition).diffEquals(adapterData.get(newItemPosition));
        }
    };

    @Override
    public void onInserted(int position, int count) {
        notifyItemInserted(position);
    }

    @Override
    public void onRemoved(int position, int count) {
        notifyItemRemoved(position);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {

    }

    @Override
    public void onChanged(int position, int count, Object payload) {
        notifyItemRangeChanged(position, count, payload);
    }
}
