package me.aap.fermata.ui.view;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.aap.fermata.media.lib.MediaLib;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.fermata.media.lib.MediaLib.PlayableItem;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.fermata.ui.activity.MainActivityPrefs;
import me.aap.fermata.ui.fragment.MediaLibFragment;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.ui.view.NavBarView;
import me.aap.utils.ui.view.ToolBarView;

import static java.util.Objects.requireNonNull;
import static me.aap.utils.ui.UiUtils.isVisible;

/**
 * @author Andrey Pavlenko
 */
public class MediaItemListView extends RecyclerView implements PreferenceStore.Listener {
	private boolean isSelectionActive;
	private int focusReq;
	private boolean grid;

	public MediaItemListView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		configure(ctx.getResources().getConfiguration());
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		MainActivityDelegate.get(ctx).getPrefs().addBroadcastListener(this);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		configure(newConfig);
	}

	private void configure(Configuration cfg) {
		Context ctx = getContext();
		MainActivityDelegate a = getActivity();
		MainActivityPrefs prefs = a.getPrefs();
		grid = prefs.getGridViewPref();

		if (grid) {
			float scale = prefs.getMediaItemScalePref();
			int span = (int) Math.max(cfg.screenWidthDp / (128 * scale), 2);
			setLayoutManager(new GridLayoutManager(ctx, span));
		} else {
			setLayoutManager(new LinearLayoutManager(ctx));
		}
	}

	@NonNull
	@Override
	public MediaItemListViewAdapter getAdapter() {
		return (MediaItemListViewAdapter) requireNonNull(super.getAdapter());
	}

	public boolean isSelectionActive() {
		return isSelectionActive;
	}

	public void select(boolean select) {
		if (!select && !isSelectionActive) return;

		boolean selectAll = isSelectionActive;
		isSelectionActive = select;

		for (MediaItemWrapper w : getAdapter().getList()) {
			if (selectAll) w.setSelected(select, true);
			else w.refreshViewCheckbox();
		}
	}

	public void discardSelection() {
		select(false);
	}

	public void refresh() {
		for (int childCount = getChildCount(), i = 0; i < childCount; ++i) {
			MediaItemViewHolder h = (MediaItemViewHolder) getChildViewHolder(getChildAt(i));
			h.getItemView().refresh();
		}
	}

	public void refreshState() {
		for (int childCount = getChildCount(), i = 0; i < childCount; ++i) {
			MediaItemViewHolder h = (MediaItemViewHolder) getChildViewHolder(getChildAt(i));
			h.getItemView().refreshState();
		}
	}

	@Override
	public void smoothScrollToPosition(int position) {
		scrollToPosition(position);
	}

	@Override
	public void scrollToPosition(int position) {
		List<MediaItemWrapper> list = getAdapter().getList();
		if ((position < 0) || (position >= list.size())) return;

		focusReq = -1;
		super.scrollToPosition(position);
		if (!isVisible(this)) return;
		MainActivityDelegate a = getActivity();
		if (a.getBody().isVideoMode()) return;
		MediaItemWrapper w = list.get(position);
		MediaItemViewHolder h = w.getViewHolder();
		if ((h != null) && h.isAttached() && (h.getItemWrapper() == w)) h.getItemView().requestFocus();
		else focusReq = position;
	}

	void holderAttached(MediaItemViewHolder h) {
		if ((focusReq != -1) && (h.getAdapterPosition() == focusReq)) {
			focusReq = -1;
			h.getItemView().requestFocus();
		}
	}

	@Nullable
	public static View focusFirst(View focused) {
		ActivityFragment f = MainActivityDelegate.get(focused.getContext()).getActiveFragment();
		if (f instanceof MediaLibFragment) {
			MediaItemListView v = ((MediaLibFragment) f).getListView();
			if (v != null) {
				List<MediaItemWrapper> list = v.getAdapter().getList();
				return ((list != null) && !list.isEmpty()) ? v.focusTo(focused, list, 0) : v.focusEmpty();
			}
		}
		return null;
	}

	@Nullable
	public static View focusLast(View focused) {
		ActivityFragment f = MainActivityDelegate.get(focused.getContext()).getActiveFragment();
		if (f instanceof MediaLibFragment) {
			MediaItemListView v = ((MediaLibFragment) f).getListView();
			if (v != null) {
				List<MediaItemWrapper> list = v.getAdapter().getList();
				return ((list != null) && !list.isEmpty())
						? v.focusTo(focused, list, list.size() - 1)
						: v.focusEmpty();
			}
		}
		return null;
	}

	@Nullable
	public static View focusActive(View focused) {
		ActivityFragment f = MainActivityDelegate.get(focused.getContext()).getActiveFragment();
		if (f instanceof MediaLibFragment) {
			MediaItemListView v = ((MediaLibFragment) f).getListView();
			return (v != null) ? v.focusSearch() : null;
		}
		return null;
	}

	public View focusSearch() {
		List<MediaItemWrapper> list = getAdapter().getList();
		MainActivityDelegate a = getActivity();
		PlayableItem p = a.getCurrentPlayable();

		if (p != null) {
			for (int i = 0, n = list.size(); i < n; i++) {
				MediaItemWrapper w = list.get(i);
				if (w.getItem() == p) return focusTo(this, w, i);
			}
		}

		int n = list.size();
		if (n == 0) return focusEmpty();

		for (int i = 0; i < n; i++) {
			MediaItemWrapper w = list.get(i);
			Item item = w.getItem();
			if ((item instanceof PlayableItem) && ((PlayableItem) item).isLastPlayed()) {
				return focusTo(this, w, i);
			}
		}

		return focusTo(this, list.get(0), 0);
	}

	private View focusEmpty() {
		View v = getActivity().getFloatingButton();
		return isVisible(v) ? v : this;
	}

	@Override
	public View focusSearch(View focused, int direction) {
		if (grid) return super.focusSearch(focused, direction);
		if (!(focused instanceof MediaItemView) || (focused.getParent() != this)) return focused;

		if (direction == FOCUS_LEFT) return focusLeft(focused);
		if (direction == FOCUS_RIGHT) return focusRight(focused);

		List<MediaItemWrapper> list = getAdapter().getList();

		if ((list == null) || list.isEmpty()) {
			return (direction == FOCUS_UP) ? focusUp(focused) : focusDown(focused);
		}

		ViewHolder vh = getChildViewHolder(focused);
		if (!(vh instanceof MediaItemViewHolder)) return focused;

		int pos = ((MediaItemViewHolder) vh).getAdapterPosition();
		if ((pos < 0) || (pos >= list.size())) return focused;
		return focusTo(focused, list, (direction == FOCUS_UP) ? (pos - 1) : (pos + 1));
	}

	public View focusLeft(View focused) {
		MainActivityDelegate a = getActivity();
		NavBarView n = a.getNavBar();
		if (isVisible(n) && n.isLeft()) return n.focusSearch();

		ToolBarView tb = getActivity().getToolBar();
		if (isVisible(tb)) return tb.focusSearch();

		List<MediaItemWrapper> list = getAdapter().getList();
		return ((list != null) && !list.isEmpty()) ? focusTo(focused, list, 0) : focused;
	}

	public View focusRight(View focused) {
		MainActivityDelegate a = getActivity();
		BodyLayout b = a.getBody();
		if (b.isBothMode()) return b;
		View v = a.getFloatingButton();
		if (isVisible(v)) return v;
		NavBarView n = a.getNavBar();
		return (isVisible(n) && n.isRight()) ? n.focusSearch() : focused;
	}

	public View focusUp(View focused) {
		ToolBarView tb = getActivity().getToolBar();
		if (isVisible(tb)) return tb.focusSearch();

		List<MediaItemWrapper> list = getAdapter().getList();
		return ((list != null) && !list.isEmpty()) ? focusTo(focused, list, list.size() - 1) : focused;
	}

	public View focusDown(View focused) {
		MainActivityDelegate a = getActivity();
		ControlPanelView p = a.getControlPanel();
		if (isVisible(p)) return p.focusSearch();

		NavBarView n = a.getNavBar();
		if (isVisible(n) && n.isBottom()) return n.focusSearch();

		List<MediaItemWrapper> list = getAdapter().getList();
		return ((list != null) && !list.isEmpty()) ? focusTo(focused, list, 0) : focused;
	}

	public View focusTo(View focused, List<MediaItemWrapper> list, int pos) {
		if (pos < 0) return focusUp(focused);
		if (pos >= list.size()) return focusDown(focused);
		return focusTo(focused, list.get(pos), pos);
	}

	private View focusTo(View focused, MediaItemWrapper w, int pos) {
		MediaItemViewHolder h = w.getViewHolder();

		if ((h != null) && h.isAttached() && (h.getItemWrapper() == w)) {
			super.scrollToPosition(pos);
			return h.getItemView();
		} else {
			focusReq = pos;
			super.scrollToPosition(pos);
			return focused;
		}
	}

	@Override
	public void onPreferenceChanged(PreferenceStore store, List<PreferenceStore.Pref<?>> prefs) {
		if (prefs.contains(MainActivityPrefs.GRID_VIEW) || prefs.contains(MainActivityPrefs.MEDIA_ITEM_SCALE)) {
			configure(getContext().getResources().getConfiguration());
			MainActivityDelegate a = getActivity();
			MediaLibFragment f = a.getActiveMediaLibFragment();

			if ((f != null) && (f.getView() == this)) {
				MediaLib.BrowsableItem i = getAdapter().getParent();

				if (i != null) {
					i.getLastPlayedItem().onSuccess(pi -> {
						if (pi != null) f.revealItem(pi);
					});
				}
			}
		}
	}

	@NonNull
	private MainActivityDelegate getActivity() {
		return MainActivityDelegate.get(getContext());
	}
}
