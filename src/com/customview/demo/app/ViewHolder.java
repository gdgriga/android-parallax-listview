
package com.customview.demo.app;

import android.util.SparseArray;
import android.view.View;

/**
 * ViewHolder pattern.
 */
public class ViewHolder {

    private SparseArray<View> holder = new SparseArray<View>();

    @SuppressWarnings("unchecked")
    public <T extends View> T get(Integer id) {
        return (T) holder.get(id);
    }

    public void put(View view) {
        put(view, view.getId());
    }

    public void put(View view, Integer id) {
        holder.put(id, view);
    }

    public void putViewFromParent(View parent, Integer id) {
        View view = parent.findViewById(id);
        put(view, id);
    }

    public void clear() {
        holder.clear();
    }
}
