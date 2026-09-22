package com.chickubrowser;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout root, tabBar, toolbar, content, home;
    EditText address;

    boolean dark = false;

    ArrayList<WebView> tabs = new ArrayList<>();
    ArrayList<String> urls = new ArrayList<>();

    int current = 0;

    android.content.SharedPreferences prefs;

    int bg() {
        return dark ? Color.rgb(25,25,25) : Color.WHITE;
    }

    int fg() {
        return dark ? Color.WHITE : Color.rgb(25,25,25);
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        prefs = getSharedPreferences("browser", 0);
        dark = prefs.getBoolean("dark", false);

        buildUI();

        String last = prefs.getString(
                "lastUrl",
                prefs.getString("homepage", "https://www.google.com")
        );

        addTab(last);
    }

    TextView button(String t) {

        TextView v = new TextView(this);

        v.setText(t);
        v.setTextSize(18);
        v.setTextColor(fg());
        v.setGravity(Gravity.CENTER);
        v.setPadding(10,4,10,4);

        return v;
    }

    void buildUI() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg());

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(4,4,4,2);

        tabBar.setBackgroundColor(
                dark ? Color.rgb(35,35,35) : Color.rgb(245,245,245)
        );

        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(4,4,4,4);
        toolbar.setBackgroundColor(bg());

        TextView back = button("‹");
        TextView forward = button("›");
        TextView reload = button("↻");
        TextView homeBtn = button("⌂");
        TextView more = button("⋮");

        address = new EditText(this);

        address.setSingleLine(true);
        address.setHint("Search or enter website");
        address.setTextColor(fg());
        address.setHintTextColor(
                dark ? Color.LTGRAY : Color.GRAY
        );

        address.setBackgroundColor(
                dark ? Color.rgb(50,50,50)
                     : Color.rgb(240,240,240)
        );

        toolbar.addView(back,
                new LinearLayout.LayoutParams(42,52));

        toolbar.addView(forward,
                new LinearLayout.LayoutParams(42,52));

        toolbar.addView(reload,
                new LinearLayout.LayoutParams(42,52));

        toolbar.addView(address,
                new LinearLayout.LayoutParams(0,52,1));

        toolbar.addView(homeBtn,
                new LinearLayout.LayoutParams(42,52));

        toolbar.addView(more,
                new LinearLayout.LayoutParams(42,52));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        root.addView(tabBar);

        root.addView(toolbar);

        root.addView(
                content,
                new LinearLayout.LayoutParams(-1,0,1)
        );

        setContentView(root);

        back.setOnClickListener(v -> {

            if (!tabs.isEmpty() &&
                    tabs.get(current).canGoBack()) {

                tabs.get(current).goBack();
            }
        });

        forward.setOnClickListener(v -> {

            if (!tabs.isEmpty() &&
                    tabs.get(current).canGoForward()) {

                tabs.get(current).goForward();
            }
        });

        reload.setOnClickListener(v -> {

            if (!tabs.isEmpty())
                tabs.get(current).reload();
        });

        homeBtn.setOnClickListener(v -> showHome());

        more.setOnClickListener(v -> menu(more));

        address.setOnEditorActionListener(
                (v,id,e) -> {

                    loadAddress();
                    return true;
                }
        );
    }

    void addTab(String url) {

        WebView w = new WebView(this);

        w.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(
                    WebView view,
                    String u) {

                if (current < urls.size())
                    urls.set(current,u);

                address.setText(u);

                prefs.edit()
                        .putString("lastUrl",u)
                        .apply();

                addHistory(u);

                showTabs();
            }
        });

        w.setWebChromeClient(
                new WebChromeClient()
        );

        w.setDownloadListener(
                (u,ua,cd,mime,len) -> {

                    DownloadManager.Request r =
                            new DownloadManager.Request(
                                    Uri.parse(u)
                            );

                    r.setTitle(
                            URLUtil.guessFileName(
                                    u,cd,mime
                            )
                    );

                    r.setNotificationVisibility(
                            DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    );

                    DownloadManager dm =
                            (DownloadManager)
                            getSystemService(
                                    DOWNLOAD_SERVICE
                            );

                    dm.enqueue(r);

                    Toast.makeText(
                            this,
                            "Download started",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setDomStorageEnabled(true);

        w.getSettings().setBuiltInZoomControls(false);
        w.getSettings().setDisplayZoomControls(false);

        tabs.add(w);
        urls.add(url);

        current = tabs.size()-1;

        showTabs();
        showWeb();

        w.loadUrl(url);
    }

    void showWeb() {

        content.removeAllViews();

        if (tabs.isEmpty())
            return;

        content.addView(
                tabs.get(current),
                new LinearLayout.LayoutParams(-1,-1)
        );

        address.setText(
                urls.get(current)
        );
    }

    void showTabs() {

        tabBar.removeAllViews();

        for (int i=0; i<tabs.size(); i++) {

            final int x = i;

            TextView t =
                    button("Tab " + (i+1) + "  ×");

            t.setTextColor(
                    i == current
                    ? Color.rgb(21,101,192)
                    : fg()
            );

            t.setOnClickListener(v -> {

                current = x;

                showWeb();
                showTabs();
            });

            t.setOnLongClickListener(v -> {

                closeTab(x);

                return true;
            });

            tabBar.addView(
                    t,
                    new LinearLayout.LayoutParams(
                            0,46,1
                    )
            );
        }

        TextView plus = button("+");

        plus.setOnClickListener(
                v -> addTab(
                        prefs.getString(
                                "homepage",
                                "https://www.google.com"
                        )
                )
        );

        tabBar.addView(
                plus,
                new LinearLayout.LayoutParams(
                        46,46
                )
        );
    }

    void closeTab(int i) {

        if (tabs.size() == 1) {

            showHome();
            return;
        }

        tabs.get(i).destroy();

        tabs.remove(i);
        urls.remove(i);

        current =
                Math.min(
                        current,
                        tabs.size()-1
                );

        showTabs();
        showWeb();
    }

    void loadAddress() {

        String s =
                address.getText()
                        .toString()
                        .trim();

        if (s.isEmpty())
            return;

        if (!s.startsWith("http://") &&
            !s.startsWith("https://")) {

            if (s.contains(".") &&
                !s.contains(" ")) {

                s = "https://" + s;

            } else {

                s =
                    "https://www.google.com/search?q="
                    + Uri.encode(s);
            }
        }

        urls.set(current,s);

        tabs.get(current)
                .loadUrl(s);
    }

    void showHome() {

        content.removeAllViews();

        home = new LinearLayout(this);

        home.setOrientation(
                LinearLayout.VERTICAL
        );

        home.setGravity(Gravity.CENTER);

        home.setPadding(30,30,30,30);

        TextView logo =
                button("CHICKU BROWSER");

        logo.setTextSize(30);

        logo.setTextColor(
                Color.rgb(21,101,192)
        );

        TextView sub =
                button(
                        "Your browser. Your homepage."
                );

        sub.setTextSize(16);

        Button go =
                new Button(this);

        go.setText("Open Homepage");

        Button bookmarks =
                new Button(this);

        bookmarks.setText("Bookmarks");

        Button history =
                new Button(this);

        history.setText("History");

        home.addView(logo);
        home.addView(sub);
        home.addView(go);
        home.addView(bookmarks);
        home.addView(history);

        content.addView(
                home,
                new LinearLayout.LayoutParams(-1,-1)
        );

        go.setOnClickListener(v -> {

            String h =
                    prefs.getString(
                            "homepage",
                            "https://www.google.com"
                    );

            urls.set(current,h);

            showWeb();

            tabs.get(current)
                    .loadUrl(h);
        });

        bookmarks.setOnClickListener(
                v -> listSaved(
                        "Bookmarks",
                        "bookmarks"
                )
        );

        history.setOnClickListener(
                v -> listSaved(
                        "History",
                        "history"
                )
        );
    }

    void addHistory(String u) {

        if (u == null || u.isEmpty())
            return;

        String old =
                prefs.getString(
                        "history",
                        ""
                );

        if (!old.contains(u)) {

            prefs.edit()
                    .putString(
                            "history",
                            old + "\n" + u
                    )
                    .apply();
        }
    }

    void bookmark() {

        String u =
                address.getText()
                        .toString();

        if (u.isEmpty())
            return;

        String old =
                prefs.getString(
                        "bookmarks",
                        ""
                );

        if (!old.contains(u)) {

            prefs.edit()
                    .putString(
                            "bookmarks",
                            old + "\n" + u
                    )
                    .apply();
        }

        Toast.makeText(
                this,
                "Bookmarked",
                Toast.LENGTH_SHORT
        ).show();
    }

    void listSaved(
            String title,
            String key
    ) {

        String[] a =
                prefs.getString(
                        key,
                        ""
                ).split("\n");

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20,20,20,20
        );

        TextView h =
                button(title);

        h.setTextSize(24);

        box.addView(h);

        for (String u : a) {

            if (!u.trim().isEmpty()) {

                Button b =
                        new Button(this);

                b.setText(u);

                box.addView(b);

                b.setOnClickListener(
                        v -> {

                            urls.set(
                                    current,
                                    u
                            );

                            showWeb();

                            tabs.get(current)
                                    .loadUrl(u);
                        }
                );
            }
        }

        content.removeAllViews();

        content.addView(box);
    }

    void menu(View anchor) {

        PopupMenu p =
                new PopupMenu(
                        this,
                        anchor
                );

        p.getMenu().add(
                "Add bookmark"
        );

        p.getMenu().add(
                "Bookmarks"
        );

        p.getMenu().add(
                "History"
        );

        p.getMenu().add(
                "Dark mode"
        );

        p.getMenu().add(
                "Custom homepage"
        );

        p.getMenu().add(
                "New tab"
        );

        p.setOnMenuItemClickListener(
                item -> {

                    String x =
                            item.getTitle()
                            .toString();

                    if (x.equals(
                            "Add bookmark")) {

                        bookmark();

                    } else if (x.equals(
                            "Bookmarks")) {

                        listSaved(
                                "Bookmarks",
                                "bookmarks"
                        );

                    } else if (x.equals(
                            "History")) {

                        listSaved(
                                "History",
                                "history"
                        );

                    } else if (x.equals(
                            "Dark mode")) {

                        toggleDark();

                    } else if (x.equals(
                            "Custom homepage")) {

                        setHomepage();

                    } else {

                        addTab(
                                prefs.getString(
                                        "homepage",
                                        "https://www.google.com"
                                )
                        );
                    }

                    return true;
                }
        );

        p.show();
    }

    void toggleDark() {

        dark = !dark;

        prefs.edit()
                .putBoolean(
                        "dark",
                        dark
                )
                .apply();

        recreate();
    }

    void setHomepage() {

        final EditText e =
                new EditText(this);

        e.setHint(
                "https://example.com"
        );

        e.setText(
                prefs.getString(
                        "homepage",
                        "https://www.google.com"
                )
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        "Custom homepage"
                )
                .setView(e)
                .setPositiveButton(
                        "Save",
                        (d,w) ->
                                prefs.edit()
                                .putString(
                                        "homepage",
                                        e.getText()
                                        .toString()
                                )
                                .apply()
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    @Override
    public void onBackPressed() {

        if (!tabs.isEmpty() &&
            tabs.get(current).canGoBack()) {

            tabs.get(curent).goBack();

        } else {

            super.onBackPressed();
        }
    }
                      }
