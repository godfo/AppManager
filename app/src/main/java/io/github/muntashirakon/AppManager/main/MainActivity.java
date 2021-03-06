/*
 * Copyright (C) 2020 Muntashir Al-Islam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.muntashirakon.AppManager.main;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.*;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import io.github.muntashirakon.AppManager.BaseActivity;
import io.github.muntashirakon.AppManager.BuildConfig;
import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.backup.BackupDialogFragment;
import io.github.muntashirakon.AppManager.batchops.BatchOpsManager;
import io.github.muntashirakon.AppManager.batchops.BatchOpsService;
import io.github.muntashirakon.AppManager.misc.HelpActivity;
import io.github.muntashirakon.AppManager.misc.Users;
import io.github.muntashirakon.AppManager.oneclickops.OneClickOpsActivity;
import io.github.muntashirakon.AppManager.profiles.ProfilesActivity;
import io.github.muntashirakon.AppManager.rules.RulesTypeSelectionDialogFragment;
import io.github.muntashirakon.AppManager.runningapps.RunningAppsActivity;
import io.github.muntashirakon.AppManager.servermanager.ServerConfig;
import io.github.muntashirakon.AppManager.settings.SettingsActivity;
import io.github.muntashirakon.AppManager.sysconfig.SysConfigActivity;
import io.github.muntashirakon.AppManager.types.ScrollableDialogBuilder;
import io.github.muntashirakon.AppManager.usage.AppUsageActivity;
import io.github.muntashirakon.AppManager.utils.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Locale;

import static androidx.appcompat.app.ActionBar.LayoutParams;

public class MainActivity extends BaseActivity implements
        SearchView.OnQueryTextListener, SwipeRefreshLayout.OnRefreshListener,
        Toolbar.OnMenuItemClickListener {
    private static final String PACKAGE_NAME_APK_UPDATER = "com.apkupdater";
    private static final String ACTIVITY_NAME_APK_UPDATER = "com.apkupdater.activity.MainActivity";
    private static final String PACKAGE_NAME_TERMUX = "com.termux";
    private static final String ACTIVITY_NAME_TERMUX = "com.termux.app.TermuxActivity";

    private static final int[] sSortMenuItemIdsMap = {R.id.action_sort_by_domain,
            R.id.action_sort_by_app_label, R.id.action_sort_by_package_name,
            R.id.action_sort_by_last_update, R.id.action_sort_by_shared_user_id,
            R.id.action_sort_by_target_sdk, R.id.action_sort_by_sha,
            R.id.action_sort_by_disabled_app, R.id.action_sort_by_blocked_components,
            R.id.action_sort_by_backup};

    @IntDef(value = {
            SORT_BY_DOMAIN,
            SORT_BY_APP_LABEL,
            SORT_BY_PACKAGE_NAME,
            SORT_BY_LAST_UPDATE,
            SORT_BY_SHARED_ID,
            SORT_BY_TARGET_SDK,
            SORT_BY_SHA,
            SORT_BY_DISABLED_APP,
            SORT_BY_BLOCKED_COMPONENTS,
            SORT_BY_BACKUP,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SortOrder {
    }

    public static final int SORT_BY_DOMAIN = 0;  // User/system app
    public static final int SORT_BY_APP_LABEL = 1;
    public static final int SORT_BY_PACKAGE_NAME = 2;
    public static final int SORT_BY_LAST_UPDATE = 3;
    public static final int SORT_BY_SHARED_ID = 4;
    public static final int SORT_BY_TARGET_SDK = 5;
    public static final int SORT_BY_SHA = 6;  // Signature
    public static final int SORT_BY_DISABLED_APP = 7;
    public static final int SORT_BY_BLOCKED_COMPONENTS = 8;
    public static final int SORT_BY_BACKUP = 9;

    @IntDef(flag = true, value = {
            FILTER_NO_FILTER,
            FILTER_USER_APPS,
            FILTER_SYSTEM_APPS,
            FILTER_DISABLED_APPS,
            FILTER_APPS_WITH_RULES,
            FILTER_APPS_WITH_ACTIVITIES,
            FILTER_APPS_WITH_BACKUPS,
            FILTER_RUNNING_APPS,
            FILTER_APPS_WITH_SPLITS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Filter {
    }

    public static final int FILTER_NO_FILTER = 0;
    public static final int FILTER_USER_APPS = 1;
    public static final int FILTER_SYSTEM_APPS = 1 << 1;
    public static final int FILTER_DISABLED_APPS = 1 << 2;
    public static final int FILTER_APPS_WITH_RULES = 1 << 3;
    public static final int FILTER_APPS_WITH_ACTIVITIES = 1 << 4;
    public static final int FILTER_APPS_WITH_BACKUPS = 1 << 5;
    public static final int FILTER_RUNNING_APPS = 1 << 6;
    public static final int FILTER_APPS_WITH_SPLITS = 1 << 7;

    MainViewModel mModel;

    private MainRecyclerAdapter mAdapter;
    private SearchView mSearchView;
    private LinearProgressIndicator mProgressIndicator;
    private SwipeRefreshLayout mSwipeRefresh;
    private BottomAppBar mBottomAppBar;
    private MaterialTextView mBottomAppBarCounter;
    private LinearLayoutCompat mMainLayout;
    private CoordinatorLayout.LayoutParams mLayoutParamsSelection;
    private CoordinatorLayout.LayoutParams mLayoutParamsTypical;
    private MenuItem appUsageMenu;
    private MenuItem runningAppsMenu;
    private MenuItem sortByBlockedComponentMenu;
    @SortOrder
    private int mSortBy;

    private final StoragePermission storagePermission = StoragePermission.init(this);

    private final ActivityResultLauncher<String> batchExportRules = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(),
            uri -> {
                if (uri == null) {
                    // Back button pressed.
                    return;
                }
                RulesTypeSelectionDialogFragment dialogFragment = new RulesTypeSelectionDialogFragment();
                Bundle args = new Bundle();
                args.putInt(RulesTypeSelectionDialogFragment.ARG_MODE, RulesTypeSelectionDialogFragment.MODE_EXPORT);
                args.putParcelable(RulesTypeSelectionDialogFragment.ARG_URI, uri);
                args.putStringArrayList(RulesTypeSelectionDialogFragment.ARG_PKG, new ArrayList<>(mModel.getSelectedPackages().keySet()));
                args.putIntArray(RulesTypeSelectionDialogFragment.ARG_USERS, Users.getUsersHandles());
                dialogFragment.setArguments(args);
                dialogFragment.show(getSupportFragmentManager(), RulesTypeSelectionDialogFragment.TAG);
                mAdapter.clearSelection();
                handleSelection();
            });

    private final BroadcastReceiver mBatchOpsBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showProgressIndicator(false);
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    protected void onAuthenticated(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        mModel = new ViewModelProvider(this).get(MainViewModel.class);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setTitle(getString(R.string.loading));

            mSearchView = new SearchView(actionBar.getThemedContext());
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setQueryHint(getString(R.string.search));

            ((ImageView) mSearchView.findViewById(androidx.appcompat.R.id.search_button))
                    .setColorFilter(UIUtils.getAccentColor(this));
            ((ImageView) mSearchView.findViewById(androidx.appcompat.R.id.search_close_btn))
                    .setColorFilter(UIUtils.getAccentColor(this));

            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.END;
            actionBar.setCustomView(mSearchView, layoutParams);
        }

        mProgressIndicator = findViewById(R.id.progress_linear);
        mProgressIndicator.setVisibilityAfterHide(View.GONE);
        RecyclerView recyclerView = findViewById(R.id.item_list);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);
        mBottomAppBar = findViewById(R.id.bottom_appbar);
        mBottomAppBarCounter = findViewById(R.id.bottom_appbar_counter);
        mMainLayout = findViewById(R.id.main_layout);

        mSwipeRefresh.setColorSchemeColors(UIUtils.getAccentColor(this));
        mSwipeRefresh.setProgressBackgroundColorSchemeColor(UIUtils.getPrimaryColor(this));
        mSwipeRefresh.setOnRefreshListener(this);

        int margin = UIUtils.dpToPx(this, 56);
        mLayoutParamsSelection = new CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.MATCH_PARENT);
        mLayoutParamsSelection.setMargins(0, margin, 0, margin);
        mLayoutParamsTypical = new CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.MATCH_PARENT);
        mLayoutParamsTypical.setMargins(0, margin, 0, 0);

        mAdapter = new MainRecyclerAdapter(MainActivity.this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        if ((boolean) AppPref.get(AppPref.PrefKey.PREF_SHOW_DISCLAIMER_BOOL)) {
            @SuppressLint("InflateParams")
            View view = getLayoutInflater().inflate(R.layout.dialog_disclaimer, null);
            new MaterialAlertDialogBuilder(this)
                    .setView(view)
                    .setCancelable(false)
                    .setPositiveButton(R.string.disclaimer_agree, (dialog, which) -> {
                        if (((MaterialCheckBox) view.findViewById(R.id.agree_forever)).isChecked()) {
                            AppPref.set(AppPref.PrefKey.PREF_SHOW_DISCLAIMER_BOOL, false);
                        }
                        checkFirstRun();
                        checkAppUpdate();
                    })
                    .setNegativeButton(R.string.disclaimer_exit, (dialog, which) -> finishAndRemoveTask())
                    .show();
        } else {
            checkFirstRun();
            checkAppUpdate();
        }

        Menu menu = mBottomAppBar.getMenu();
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        mBottomAppBar.setNavigationOnClickListener(v -> {
            if (mAdapter != null) mAdapter.clearSelection();
            handleSelection();
        });
        mBottomAppBar.setOnMenuItemClickListener(this);
        handleSelection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_actions, menu);
        appUsageMenu = menu.findItem(R.id.action_app_usage);
        runningAppsMenu = menu.findItem(R.id.action_running_apps);
        sortByBlockedComponentMenu = menu.findItem(R.id.action_sort_by_blocked_components);
        MenuItem apkUpdaterMenu = menu.findItem(R.id.action_apk_updater);
        try {
            if (!getPackageManager().getApplicationInfo(PACKAGE_NAME_APK_UPDATER, 0).enabled)
                throw new PackageManager.NameNotFoundException();
            apkUpdaterMenu.setVisible(true);
        } catch (PackageManager.NameNotFoundException e) {
            apkUpdaterMenu.setVisible(false);
        }
        MenuItem termuxMenu = menu.findItem(R.id.action_termux);
        try {
            if (!getPackageManager().getApplicationInfo(PACKAGE_NAME_TERMUX, 0).enabled)
                throw new PackageManager.NameNotFoundException();
            termuxMenu.setVisible(true);
        } catch (PackageManager.NameNotFoundException e) {
            termuxMenu.setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(sSortMenuItemIdsMap[mSortBy]).setChecked(true);
        if (mModel != null) {
            int flags = mModel.getFilterFlags();
            if ((flags & FILTER_USER_APPS) != 0) {
                menu.findItem(R.id.action_filter_user_apps).setChecked(true);
            }
            if ((flags & FILTER_SYSTEM_APPS) != 0) {
                menu.findItem(R.id.action_filter_system_apps).setChecked(true);
            }
            if ((flags & FILTER_DISABLED_APPS) != 0) {
                menu.findItem(R.id.action_filter_disabled_apps).setChecked(true);
            }
            if ((flags & FILTER_APPS_WITH_RULES) != 0) {
                menu.findItem(R.id.action_filter_apps_with_rules).setChecked(true);
            }
            if ((flags & FILTER_APPS_WITH_ACTIVITIES) != 0) {
                menu.findItem(R.id.action_filter_apps_with_activities).setChecked(true);
            }
            if ((flags & FILTER_APPS_WITH_SPLITS) != 0) {
                menu.findItem(R.id.action_filter_apps_with_splits).setChecked(true);
            }
            if ((flags & FILTER_RUNNING_APPS) != 0) {
                menu.findItem(R.id.action_filter_running_apps).setChecked(true);
            }
        }
        if (AppPref.isRootOrAdbEnabled()) {
            runningAppsMenu.setVisible(true);
            sortByBlockedComponentMenu.setVisible(true);
        } else {
            runningAppsMenu.setVisible(false);
            sortByBlockedComponentMenu.setVisible(false);
        }
        menu.findItem(R.id.action_sys_config).setVisible(AppPref.isRootEnabled());
        appUsageMenu.setVisible((boolean) AppPref.get(AppPref.PrefKey.PREF_USAGE_ACCESS_ENABLED_BOOL));
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_instructions) {
            Intent helpIntent = new Intent(this, HelpActivity.class);
            startActivity(helpIntent);
        } else if (id == R.id.action_refresh) {
            if (mModel != null) {
                showProgressIndicator(true);
                mModel.loadApplicationItems();
            }
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            // Sort
        } else if (id == R.id.action_sort_by_app_label) {
            setSortBy(SORT_BY_APP_LABEL);
            item.setChecked(true);
        } else if (id == R.id.action_sort_by_package_name) {
            setSortBy(SORT_BY_PACKAGE_NAME);
            item.setChecked(true);
        } else if (id == R.id.action_sort_by_domain) {
            setSortBy(SORT_BY_DOMAIN);
            item.setChecked(true);
        } else if (id == R.id.action_sort_by_last_update) {
            setSortBy(SORT_BY_LAST_UPDATE);
            item.setChecked(true);
        } else if (id == R.id.action_sort_by_shared_user_id) {
            setSortBy(SORT_BY_SHARED_ID);
            item.setChecked(true);
        } else if (id == R.id.action_sort_by_sha) {
            setSortBy(SORT_BY_SHA);
            item.setChecked(true);
        } else if (id == R.id.action_sort_by_target_sdk) {
            setSortBy(SORT_BY_TARGET_SDK);
            item.setChecked(true);
        } else if (id == R.id.action_sort_by_disabled_app) {
            setSortBy(SORT_BY_DISABLED_APP);
            item.setChecked(true);
        } else if (id == R.id.action_sort_by_blocked_components) {
            setSortBy(SORT_BY_BLOCKED_COMPONENTS);
            item.setChecked(true);
        } else if (id == R.id.action_sort_by_backup) {
            setSortBy(SORT_BY_BACKUP);
            item.setChecked(true);
            // Filter
        } else if (id == R.id.action_filter_user_apps) {
            if (!item.isChecked()) mModel.addFilterFlag(FILTER_USER_APPS);
            else mModel.removeFilterFlag(FILTER_USER_APPS);
            item.setChecked(!item.isChecked());
        } else if (id == R.id.action_filter_system_apps) {
            if (!item.isChecked()) mModel.addFilterFlag(FILTER_SYSTEM_APPS);
            else mModel.removeFilterFlag(FILTER_SYSTEM_APPS);
            item.setChecked(!item.isChecked());
        } else if (id == R.id.action_filter_disabled_apps) {
            if (!item.isChecked()) mModel.addFilterFlag(FILTER_DISABLED_APPS);
            else mModel.removeFilterFlag(FILTER_DISABLED_APPS);
            item.setChecked(!item.isChecked());
        } else if (id == R.id.action_filter_apps_with_rules) {
            if (!item.isChecked()) mModel.addFilterFlag(FILTER_APPS_WITH_RULES);
            else mModel.removeFilterFlag(FILTER_APPS_WITH_RULES);
            item.setChecked(!item.isChecked());
        } else if (id == R.id.action_filter_apps_with_activities) {
            if (!item.isChecked()) mModel.addFilterFlag(FILTER_APPS_WITH_ACTIVITIES);
            else mModel.removeFilterFlag(FILTER_APPS_WITH_ACTIVITIES);
            item.setChecked(!item.isChecked());
        } else if (id == R.id.action_filter_apps_with_backups) {
            if (!item.isChecked()) mModel.addFilterFlag(FILTER_APPS_WITH_BACKUPS);
            else mModel.removeFilterFlag(FILTER_APPS_WITH_BACKUPS);
            item.setChecked(!item.isChecked());
        } else if (id == R.id.action_filter_running_apps) {
            if (!item.isChecked()) mModel.addFilterFlag(FILTER_RUNNING_APPS);
            else mModel.removeFilterFlag(FILTER_RUNNING_APPS);
            item.setChecked(!item.isChecked());
        } else if (id == R.id.action_filter_apps_with_splits) {
            if (!item.isChecked()) mModel.addFilterFlag(FILTER_APPS_WITH_SPLITS);
            else mModel.removeFilterFlag(FILTER_APPS_WITH_SPLITS);
            item.setChecked(!item.isChecked());
            // Others
        } else if (id == R.id.action_app_usage) {
            Intent usageIntent = new Intent(this, AppUsageActivity.class);
            startActivity(usageIntent);
        } else if (id == R.id.action_one_click_ops) {
            Intent onClickOpsIntent = new Intent(this, OneClickOpsActivity.class);
            startActivity(onClickOpsIntent);
        } else if (id == R.id.action_apk_updater) {
            try {
                if (!getPackageManager().getApplicationInfo(PACKAGE_NAME_APK_UPDATER, 0).enabled)
                    throw new PackageManager.NameNotFoundException();
                Intent intent = new Intent();
                intent.setClassName(PACKAGE_NAME_APK_UPDATER, ACTIVITY_NAME_APK_UPDATER);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {
            }
        } else if (id == R.id.action_termux) {
            try {
                if (!getPackageManager().getApplicationInfo(PACKAGE_NAME_TERMUX, 0).enabled)
                    throw new PackageManager.NameNotFoundException();
                Intent intent = new Intent();
                intent.setClassName(PACKAGE_NAME_TERMUX, ACTIVITY_NAME_TERMUX);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {
            }
        } else if (id == R.id.action_running_apps) {
            Intent runningAppsIntent = new Intent(this, RunningAppsActivity.class);
            startActivity(runningAppsIntent);
        } else if (id == R.id.action_sys_config) {
            Intent sysConfigIntent = new Intent(this, SysConfigActivity.class);
            startActivity(sysConfigIntent);
        } else if (id == R.id.action_profiles) {
            Intent profilesIntent = new Intent(this, ProfilesActivity.class);
            startActivity(profilesIntent);
        } else return super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    public boolean onMenuItemClick(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_select_all) {
            mAdapter.selectAll();
        } else if (id == R.id.action_backup) {
            if (mModel != null) {
                BackupDialogFragment backupDialogFragment = new BackupDialogFragment();
                Bundle args = new Bundle();
                args.putParcelableArrayList(BackupDialogFragment.ARG_PACKAGE_PAIRS, mModel.getSelectedPackagesWithUsers(false));
                backupDialogFragment.setArguments(args);
                backupDialogFragment.setOnActionBeginListener(mode -> showProgressIndicator(true));
                backupDialogFragment.setOnActionCompleteListener((mode, failedPackages) -> showProgressIndicator(false));
                backupDialogFragment.show(getSupportFragmentManager(), BackupDialogFragment.TAG);
                mAdapter.clearSelection();
                handleSelection();
            }
        } else if (id == R.id.action_backup_apk) {
            storagePermission.request(granted -> {
                if (granted) handleBatchOp(BatchOpsManager.OP_BACKUP_APK);
            });
        } else if (id == R.id.action_block_trackers) {
            handleBatchOp(BatchOpsManager.OP_BLOCK_TRACKERS);
        } else if (id == R.id.action_clear_data) {
            handleBatchOpWithWarning(BatchOpsManager.OP_CLEAR_DATA);
        } else if (id == R.id.action_enable) {
            handleBatchOp(BatchOpsManager.OP_ENABLE);
        } else if (id == R.id.action_disable) {
            handleBatchOpWithWarning(BatchOpsManager.OP_DISABLE);
        } else if (id == R.id.action_disable_background) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.are_you_sure)
                    .setMessage(R.string.disable_background_run_description)
                    .setPositiveButton(R.string.yes, (dialog, which) ->
                            handleBatchOp(BatchOpsManager.OP_DISABLE_BACKGROUND))
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else if (id == R.id.action_export_blocking_rules) {
            @SuppressLint("SimpleDateFormat") final String fileName = "app_manager_rules_export-" + DateUtils.formatDateTime(System.currentTimeMillis()) + ".am.tsv";
            batchExportRules.launch(fileName);
        } else if (id == R.id.action_force_stop) {
            handleBatchOp(BatchOpsManager.OP_FORCE_STOP);
        } else if (id == R.id.action_uninstall) {
            handleBatchOpWithWarning(BatchOpsManager.OP_UNINSTALL);
        } else {
            mAdapter.clearSelection();
            handleSelection();
            return false;
        }
        return true;
    }

    @Override
    public void onRefresh() {
        showProgressIndicator(true);
        if (mModel != null) mModel.loadApplicationItems();
        mSwipeRefresh.setRefreshing(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAdapter != null && mModel != null) {
            // Set observer
            mModel.getApplicationItems().observe(this, applicationItems -> {
                mAdapter.setDefaultList(applicationItems);
                showProgressIndicator(false);
                // Set title and subtitle
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(R.string.onboard);
                    actionBar.setSubtitle(R.string.packages);
                }
            });
            // Set filter
            if (mSearchView != null && !TextUtils.isEmpty(mModel.getSearchQuery())) {
                mSearchView.setIconified(false);
                mSearchView.setQuery(mModel.getSearchQuery(), false);
            }
        }
        // Show/hide app usage menu
        if (appUsageMenu != null) {
            appUsageMenu.setVisible((Boolean) AppPref.get(AppPref.PrefKey.PREF_USAGE_ACCESS_ENABLED_BOOL));
        }
        // Set sort by
        mSortBy = (int) AppPref.get(AppPref.PrefKey.PREF_MAIN_WINDOW_SORT_ORDER_INT);
        if (AppPref.isRootOrAdbEnabled()) {
            if (runningAppsMenu != null) runningAppsMenu.setVisible(true);
            if (sortByBlockedComponentMenu != null) sortByBlockedComponentMenu.setVisible(true);
        } else {
            if (mSortBy == SORT_BY_BLOCKED_COMPONENTS) mSortBy = SORT_BY_APP_LABEL;
            if (runningAppsMenu != null) runningAppsMenu.setVisible(false);
            if (sortByBlockedComponentMenu != null) sortByBlockedComponentMenu.setVisible(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mModel != null) mModel.onResume();
        registerReceiver(mBatchOpsBroadCastReceiver, new IntentFilter(BatchOpsService.ACTION_BATCH_OPS_COMPLETED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBatchOpsBroadCastReceiver);
    }

    private void checkFirstRun() {
        if (Utils.isAppInstalled()) {
            // TODO(4/1/21): Do something relevant and useful
            AppPref.set(AppPref.PrefKey.PREF_LAST_VERSION_CODE_LONG, (long) BuildConfig.VERSION_CODE);
        }
    }

    private void checkAppUpdate() {
        if (Utils.isAppUpdated()) {
            // Clean old am.jar
            IOUtils.deleteSilently(ServerConfig.getDestJarFile());
            new Thread(() -> {
                final Spanned spannedChangelog = HtmlCompat.fromHtml(IOUtils.getContentFromAssets(this, "changelog.html"), HtmlCompat.FROM_HTML_MODE_COMPACT);
                runOnUiThread(() ->
                        new ScrollableDialogBuilder(this, spannedChangelog)
                                .linkifyAll()
                                .setTitle(R.string.changelog)
                                .setNegativeButton(R.string.ok, null)
                                .setNeutralButton(R.string.instructions, (dialog, which, isChecked) -> {
                                    Intent helpIntent = new Intent(this, HelpActivity.class);
                                    startActivity(helpIntent);
                                }).show());
            }).start();
            AppPref.set(AppPref.PrefKey.PREF_LAST_VERSION_CODE_LONG, (long) BuildConfig.VERSION_CODE);
        }
    }

    void handleSelection() {
        if (mModel == null || mModel.getSelectedPackages().size() == 0) {
            mBottomAppBar.setVisibility(View.GONE);
            mMainLayout.setLayoutParams(mLayoutParamsTypical);
            mAdapter.clearSelection();
        } else {
            mBottomAppBar.setVisibility(View.VISIBLE);
            mBottomAppBarCounter.setText(getString(R.string.some_items_selected, mModel.getSelectedPackages().size()));
            mMainLayout.setLayoutParams(mLayoutParamsSelection);
        }
    }

    private void handleBatchOp(@BatchOpsManager.OpType int op) {
        if (mModel == null) return;
        showProgressIndicator(true);
        Intent intent = new Intent(this, BatchOpsService.class);
        BatchOpsManager.Result input = new BatchOpsManager.Result(mModel.getSelectedPackagesWithUsers(false));
        intent.putStringArrayListExtra(BatchOpsService.EXTRA_OP_PKG, input.getFailedPackages());
        intent.putIntegerArrayListExtra(BatchOpsService.EXTRA_OP_USERS, input.getAssociatedUserHandles());
        intent.putExtra(BatchOpsService.EXTRA_OP, op);
        ContextCompat.startForegroundService(this, intent);
        new Thread(() -> {
            mAdapter.clearSelection();
            runOnUiThread(this::handleSelection);
        }).start();
    }

    private void handleBatchOpWithWarning(@BatchOpsManager.OpType int op) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.this_action_cannot_be_undone)
                .setPositiveButton(R.string.yes, (dialog, which) -> handleBatchOp(op))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showProgressIndicator(boolean show) {
        if (show) mProgressIndicator.show();
        else mProgressIndicator.hide();
    }

    /**
     * Sort main list if provided value is valid.
     *
     * @param sortBy Must be one of SORT_*
     */
    private void setSortBy(@SortOrder int sortBy) {
        mSortBy = sortBy;
        if (mModel != null) mModel.setSortBy(sortBy);
    }

    @Override
    public boolean onQueryTextChange(String searchQuery) {
        if (mModel != null) mModel.setSearchQuery(searchQuery.toLowerCase(Locale.ROOT));
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }
}
