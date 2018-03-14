package cn.intret.app.picgo.ui.main;

import android.support.annotation.StringRes;

import java.io.File;

import cn.intret.app.picgo.model.user.UserInitialPreferences;

/**
 * Created by intret on 2018/3/13.
 */

public class MainContractor {

    interface View {
        void onLoadedUserInitialPreferences(UserInitialPreferences userInitialPreferences);

        void onErrorMessage(@StringRes int msg);
    }

    interface Presenter {
        void loadInitialPreference();

        void updateFolderListItemThumbnailList(File directory);
    }
}
