package me.aap.fermata.addon.tv.m3u;

import java.util.List;

import me.aap.fermata.BuildConfig;
import me.aap.fermata.addon.tv.R;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.fermata.vfs.m3u.M3uFile;
import me.aap.fermata.vfs.m3u.M3uFileSystemProvider;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.log.Log;
import me.aap.utils.pref.BasicPreferenceStore;
import me.aap.utils.pref.PreferenceSet;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.ui.fragment.FilePickerFragment;
import me.aap.utils.vfs.VirtualFileSystem;

import static me.aap.fermata.addon.tv.m3u.TvM3uFile.CATCHUP_DAYS;
import static me.aap.fermata.addon.tv.m3u.TvM3uFile.CATCHUP_QUERY;
import static me.aap.fermata.addon.tv.m3u.TvM3uFile.CATCHUP_TYPE;
import static me.aap.fermata.addon.tv.m3u.TvM3uFile.EPG_FILE_AGE;
import static me.aap.fermata.addon.tv.m3u.TvM3uFile.EPG_SHIFT;
import static me.aap.fermata.addon.tv.m3u.TvM3uFile.EPG_URL;
import static me.aap.fermata.addon.tv.m3u.TvM3uFile.LOGO_PREFER_EPG;
import static me.aap.fermata.addon.tv.m3u.TvM3uFile.LOGO_URL;
import static me.aap.fermata.vfs.m3u.M3uFile.NAME;
import static me.aap.fermata.vfs.m3u.M3uFile.URL;
import static me.aap.utils.async.Completed.completedNull;
import static me.aap.utils.net.http.HttpFileDownloader.AGENT;

/**
 * @author Andrey Pavlenko
 */
public class TvM3uFileSystemProvider extends M3uFileSystemProvider {

	@Override
	public FutureSupplier<TvM3uFile> select(MainActivityDelegate a, List<? extends VirtualFileSystem> fs) {
		PreferenceSet prefs = new PreferenceSet();
		PreferenceStore ps = PrefsHolder.instance;
		PreferenceSet sub;

		prefs.addStringPref(o -> {
			o.store = ps;
			o.pref = NAME;
			o.title = me.aap.fermata.R.string.m3u_playlist_name;
		});
		prefs.addFilePref(o -> {
			o.store = ps;
			o.pref = URL;
			o.mode = FilePickerFragment.FILE;
			o.title = me.aap.fermata.R.string.m3u_playlist_location;
			o.stringHint = a.getString(me.aap.fermata.R.string.m3u_playlist_location_hint);
		});

		sub = prefs.subSet(o -> o.title = R.string.epg);
		sub.addStringPref(o -> {
			o.store = ps;
			o.pref = EPG_URL;
			o.title = R.string.epg_url;
			o.stringHint = "http://example.com/epg.xml.gz";
		});
		sub.addBooleanPref(o -> {
			o.store = ps;
			o.pref = LOGO_PREFER_EPG;
			o.title = R.string.logo_prefer_epg;
		});
		sub.addFloatPref(o -> {
			o.store = ps;
			o.pref = EPG_SHIFT;
			o.seekMin = -12;
			o.seekMax = 12;
			o.title = R.string.epg_time_shift;
		});

		/* sub = prefs.subSet(o -> o.title = R.string.catchup);
		sub.addStringPref(o -> {
			o.store = ps;
			o.pref = CATCHUP_QUERY;
			o.title = R.string.catchup_query;
		});
		sub.addListPref(o -> {
			o.store = ps;
			o.pref = CATCHUP_TYPE;
			o.title = R.string.catchup_type;
			o.subtitle = R.string.catchup_type_cur;
			o.formatSubtitle = true;
			o.values = new int[]{R.string.catchup_type_default, R.string.catchup_type_append, R.string.catchup_type_shift};
		});
		sub.addIntPref(o -> {
			o.store = ps;
			o.pref = CATCHUP_DAYS;
			o.seekMax = 30;
			o.title = R.string.catchup_days;
		});

		sub = prefs.subSet(o -> o.title = R.string.logo);
		sub.addFilePref(o -> {
			o.store = ps;
			o.pref = LOGO_URL;
			o.mode = FilePickerFragment.FILE;
			o.title = R.string.logo_location;
			o.stringHint = a.getString(R.string.logo_location_hint);
		}); */

		sub = prefs.subSet(o -> o.title = R.string.connection_settings);
		sub.addStringPref(o -> {
			o.store = ps;
			o.pref = AGENT;
			o.title = me.aap.fermata.R.string.m3u_playlist_agent;
			o.stringHint = "Fermata/" + BuildConfig.VERSION_NAME;
		});

		return requestPrefs(a, prefs, ps).then(ok -> {
			if (!ok) return completedNull();
			return load(ps, TvM3uFileSystem.getInstance()).cast();
		});
	}

	@Override
	protected void setPrefs(PreferenceStore ps, M3uFile m3u) {
		TvM3uFile f = (TvM3uFile) m3u;
		super.setPrefs(ps, f);
		f.setVideo(true);
		f.setEpgUrl(ps.getStringPref(EPG_URL));
		f.setEpgShift(ps.getFloatPref(EPG_SHIFT));
		f.setCatchupQuery(ps.getStringPref(CATCHUP_QUERY));
		f.setCatchupType(ps.getIntPref(CATCHUP_TYPE));
		f.setCatchupDays(ps.getIntPref(CATCHUP_DAYS));
		f.setLogoUrl(ps.getStringPref(LOGO_URL));
		f.setPreferEpgLogo(ps.getBooleanPref(LOGO_PREFER_EPG));
		f.setEpgMaxAge(EPG_FILE_AGE);
	}

	public static void removeSource(TvM3uFile f) {
		Log.d("Removing TV source ", f);
		f.cleanUp();
	}

	protected String getTitle(MainActivityDelegate a) {
		return a.getString(R.string.add_tv_source);
	}

	private static final class PrefsHolder extends BasicPreferenceStore {
		static final PrefsHolder instance = new PrefsHolder();
	}
}
