package com.gss.countrycodepicker;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.gss.countrycodepickerlib.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 选择国家
 * 用法：
 *
 * 1、每次打开要使用的页面时调用 CountryCodePicker.preCacheCountryList(application, api) 缓存最新的国家列表
 * 2、调用CountryCodePicker.pick(activity/fragment, listener) 打开选择器
 */
public class CountryCodePicker extends BottomSheetDialogFragment {

    private ArrayList<Country> selectedCountries = new ArrayList<>();
    private ArrayList<Country> allCountries = new ArrayList<>();

    private BottomSheetBehavior mBehavior;

    private ArrayList<Country> countries;

    private boolean isDarkMode;

    private Listener listener;
    private EditText searchInput;

    public static abstract class Listener {
        public void onGet(Country country){}
        public boolean isDarkMode(){return false;}
    }

    private CountryCodePicker(Context context, Listener listener) {
        this.listener = listener;
        if(listener != null){
            isDarkMode = listener.isDarkMode();
        }
        // 取出换粗
        this.countries = getCountryList(context);
    }

    public static void pick(FragmentActivity activity, Listener listener){
        new CountryCodePicker(activity, listener).show(activity.getSupportFragmentManager(), "dialog");
    }

    public static void pick(Fragment fragment, Listener listener){
        new CountryCodePicker(fragment.getContext(), listener).show(fragment.getChildFragmentManager(), "dialog");
    }

    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.activity_pick, null);
        dialog.setContentView(view);
        mBehavior = BottomSheetBehavior.from((View) view.getParent());

        // 设置初始镂空高度
        int defaultPeekheight = (int) (getActivityHeight(getContext()) * 0.88);
        if (defaultPeekheight < dip2px(getContext(), 500)) {
            defaultPeekheight = dip2px(getContext(), 500);
        }
        if(Build.VERSION.RELEASE.contains("8.0.0")){
            defaultPeekheight = getActivityHeight(getContext());
        }
        mBehavior.setPeekHeight(defaultPeekheight);

        if(isDarkMode){
            view.setBackgroundColor(Color.parseColor("#000000"));
            ((TextView) view.findViewById(R.id.tv_title)).setTextColor(Color.parseColor("#ffffff"));
        }
        TextView titleView = view.findViewById(R.id.tv_title);
        searchInput = view.findViewById(R.id.et_search);
        TextView searchBtn = view.findViewById(R.id.search_btn);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if("搜索".equals(searchBtn.getText())){
                    searchInput.setVisibility(View.VISIBLE);
                    titleView.setVisibility(View.GONE);
                    searchBtn.setText("取消");
                    searchInput.requestFocus();
                    openKeyboard(searchInput, getContext());
                } else {
                    searchInput.setVisibility(View.GONE);
                    titleView.setVisibility(View.VISIBLE);
                    searchBtn.setText("搜索");
                    searchInput.clearFocus();
                    closeKeyboard(searchInput, getContext());
                }
            }
        });
        // 设置字母索引控件的边距
        int leftHeight = getActivityHeight(getContext()) - defaultPeekheight;
        int marginTop = dip2px(getContext(), 30);
        int marginBottom = leftHeight + marginTop;
        SideBar sideBar = view.findViewById(R.id.side);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) sideBar.getLayoutParams();
        layoutParams.bottomMargin = marginBottom;
        layoutParams.topMargin = marginTop;
        sideBar.setLayoutParams(layoutParams);

        RecyclerView rvPick = (RecyclerView) view.findViewById(R.id.rv_pick);
        SideBar side = (SideBar) view.findViewById(R.id.side);
        EditText etSearch = (EditText) view.findViewById(R.id.et_search);
        TextView tvLetter = (TextView) view.findViewById(R.id.tv_letter);
        allCountries.clear();
        allCountries.addAll(countries != null && countries.size() > 0 ? countries : Country.getAll(getContext(), null));
        selectedCountries.clear();
        selectedCountries.addAll(allCountries);
        final CAdapter adapter = new CAdapter(selectedCountries);
        rvPick.setAdapter(adapter);
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        rvPick.setLayoutManager(manager);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String string = s.toString();
                if(!TextUtils.isEmpty(string)){
                    selectedCountries.clear();
                    for (Country country : allCountries) {
                        boolean matchCode =("+" + country.code).contains(string.toLowerCase());
                        if (country.name.toLowerCase().contains(string.toLowerCase()) || matchCode){
                            if(matchCode && (("+" + country.code).equals(string.toLowerCase()) || String.valueOf(country.code).equals(string.toLowerCase()))){
                                // 完全匹配
                                selectedCountries.add(0, country);
                            } else {
                                selectedCountries.add(country);
                            }
                        }
                    }
                    adapter.update(selectedCountries, true);
                } else {
                    adapter.update(allCountries, false);
                }

            }
        });

        side.addIndex("#", side.indexes.size());
        side.setOnLetterChangeListener(new SideBar.OnLetterChangeListener() {
            @Override
            public void onLetterChange(String letter) {
                tvLetter.setVisibility(View.VISIBLE);
                tvLetter.setText(letter);
                int position = "常".equals(letter) ? 0 : adapter.getLetterPosition(letter);
                if (position != -1) {
                    manager.scrollToPositionWithOffset(position, 0);
                }
            }

            @Override
            public void onReset() {
                tvLetter.setVisibility(View.GONE);
            }
        });

        return dialog;
    }

    class CAdapter extends PyAdapter<RecyclerView.ViewHolder> {

        public CAdapter(List<? extends PyEntity> entities) {
            super(entities);
        }

        @Override
        public RecyclerView.ViewHolder onCreateLetterHolder(ViewGroup parent, int viewType) {
            return new LetterHolder(getLayoutInflater().inflate(R.layout.item_letter, parent, false));
        }

        @Override
        public RecyclerView.ViewHolder onCreateHolder(ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_country_large_padding, parent, false));
        }

        @Override
        public void onBindHolder(RecyclerView.ViewHolder holder, PyEntity entity, int position) {
            VH vh = (VH) holder;
            final Country country = (Country) entity;
//            vh.ivFlag.setImageResource(country.flag);
            vh.tvName.setText(country.name);
            vh.tvCode.setText("+" + country.code);
            holder.itemView.setOnClickListener(v -> {
                Intent data = new Intent();
                data.putExtra("country", country.toJson());
//                setResult(AppCompatActivity.RESULT_OK, data);
//                finish();
                if(listener != null){
                    listener.onGet(country);
                }
                closeKeyboard(searchInput, getContext());
                dismiss();
            });

            if(isDarkMode){
                vh.tvName.setTextColor(Color.parseColor("#ffffff"));
                vh.tvCode.setTextColor(Color.parseColor("#ffffff"));
                vh.itemView.findViewById(R.id.v_divider).setBackgroundColor(Color.parseColor("#242424"));
            }
        }

        @Override
        public void onBindLetterHolder(RecyclerView.ViewHolder holder, LetterEntity entity, int position) {
            ((LetterHolder) holder).textView.setText(entity.letter.toUpperCase());
            if(isDarkMode){
                ((LetterHolder) holder).textView.setTextColor(Color.parseColor("#ffffff"));
                ((LetterHolder) holder).textView.setBackgroundColor(Color.parseColor("#242424"));
            }

        }
    }

    private static int getActivityHeight(Context activity) {
        return activity.getResources().getDisplayMetrics().heightPixels;
    }
    private static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 打开软键盘
     *
     * @param view    当前焦点视图
     * @param context 上下文
     */
    private static void openKeyboard(View view, Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.RESULT_SHOWN);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    /**
     * 关闭软键盘
     *
     * @param activity 上下文
     */
    private static void closeKeyboard(final Activity activity) {
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        closeKeyboard(view, activity);
    }

    /**
     * 关闭软键盘
     *
     * @param view    当前焦点视图
     * @param context 上下文
     */
    private static void closeKeyboard(View view, Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private static ArrayList<Country> getCountryList(Context context) {
        String listCachePath = FileUtils.getSmsCountryCodeList(context);
        String listJson = null;
        try {
            listJson = FileUtils.getStringCache(listCachePath);
        } catch (Exception e) {
        }
        if (!TextUtils.isEmpty(listJson)) {
            ArrayList<Country> countries = new ArrayList<>();
            try {
                JSONArray arrays = new JSONArray(listJson);
                for (int i = 0; i < arrays.length(); i++){
                    JSONObject jsonObject = arrays.getJSONObject(i);
                    Country country = new Country(jsonObject.optInt("code"), jsonObject.optString("name"));
                    countries.add(country);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return countries;
        }
        return null;
    }

}
