package com.rupex.app.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.rupex.app.R;
import com.rupex.app.data.model.CategoryStat;
import com.rupex.app.ui.CategoryStatAdapter;
import com.rupex.app.ui.MainViewModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment showing spending charts and category breakdown
 */
public class ChartsFragment extends Fragment {

    private MainViewModel viewModel;
    private CategoryStatAdapter categoryAdapter;
    
    private PieChart pieChart;
    private TextView tvPeriod;
    private TextView tvTotalIncome;
    private TextView tvTotalExpenses;
    private TextView tvSavings;
    private RecyclerView recyclerCategories;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private LinearLayout tabIncome;
    private LinearLayout tabExpense;
    private View expenseIndicator;
    private View incomeIndicator;
    
    private Calendar currentMonth;
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
    private final NumberFormat currencyFormat;
    
    private boolean showingExpenses = true; // Track which tab is active
    
    // Category emoji icons
    private static final Map<String, String> CATEGORY_ICONS = new HashMap<String, String>() {{
        put("food", "🍕");
        put("other", "📦");
        put("education", "📚");
        put("social life", "🎉");
        put("friends", "👥");
        put("transport", "🚗");
        put("bills", "📄");
        put("grocery", "🛒");
        put("shopping", "🛍️");
        put("entertainment", "🎬");
        put("uncategorized", "❓");
    }};
    
    private List<CategoryStat> currentStats = new ArrayList<>();

    public ChartsFragment() {
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setMaximumFractionDigits(2);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_charts_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        currentMonth = Calendar.getInstance();
        
        // Initialize views
        pieChart = view.findViewById(R.id.pieChart);
        tvPeriod = view.findViewById(R.id.tvPeriod);
        tvTotalIncome = view.findViewById(R.id.tvChartIncome);
        tvTotalExpenses = view.findViewById(R.id.tvChartExpense);
        tvSavings = view.findViewById(R.id.tvChartSavings);
        recyclerCategories = view.findViewById(R.id.rvCategories);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        tabIncome = view.findViewById(R.id.tabIncome);
        tabExpense = view.findViewById(R.id.tabExpense);
        expenseIndicator = view.findViewById(R.id.expenseIndicator);
        incomeIndicator = view.findViewById(R.id.incomeIndicator);

        setupPieChart();
        setupRecyclerView();
        setupNavigation();
        setupTabs();
        setupViewModel();
        updatePeriodDisplay();
    }

    private void setupTabs() {
        // Set initial state - expenses selected
        updateTabState();
        
        tabExpense.setOnClickListener(v -> {
            if (!showingExpenses) {
                showingExpenses = true;
                updateTabState();
                loadCategoryStats();
            }
        });
        
        tabIncome.setOnClickListener(v -> {
            if (showingExpenses) {
                showingExpenses = false;
                updateTabState();
                loadCategoryStats();
            }
        });
    }
    
    private void updateTabState() {
        if (showingExpenses) {
            tabExpense.setAlpha(1.0f);
            tabIncome.setAlpha(0.6f);
            if (expenseIndicator != null) expenseIndicator.setVisibility(View.VISIBLE);
            if (incomeIndicator != null) incomeIndicator.setVisibility(View.INVISIBLE);
        } else {
            tabExpense.setAlpha(0.6f);
            tabIncome.setAlpha(1.0f);
            if (expenseIndicator != null) expenseIndicator.setVisibility(View.INVISIBLE);
            if (incomeIndicator != null) incomeIndicator.setVisibility(View.VISIBLE);
        }
    }
    
    private void loadCategoryStats() {
        viewModel.loadCategoryStatsForType(showingExpenses ? "expense" : "income");
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(20, 20, 20, 20);
        
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(58f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#333333"));
        
        pieChart.setRotationAngle(270);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        pieChart.getLegend().setEnabled(false);
        
        // Handle slice selection
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry entry = (PieEntry) e;
                    String category = entry.getLabel();
                    String icon = CATEGORY_ICONS.getOrDefault(category.toLowerCase(), "📦");
                    pieChart.setCenterText(icon + " " + category + "\n" + currencyFormat.format(entry.getValue()));
                }
            }

            @Override
            public void onNothingSelected() {
                updateCenterText();
            }
        });
    }
    
    private void updateCenterText() {
        if (!currentStats.isEmpty()) {
            double total = 0;
            for (CategoryStat stat : currentStats) {
                total += stat.getTotal();
            }
            pieChart.setCenterText("Total\n" + currencyFormat.format(total));
        } else {
            pieChart.setCenterText(showingExpenses ? "No expenses" : "No income");
        }
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryStatAdapter();
        recyclerCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCategories.setAdapter(categoryAdapter);
        recyclerCategories.setNestedScrollingEnabled(false);
    }

    private void setupNavigation() {
        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updatePeriodDisplay();
            loadData();
        });

        btnNextMonth.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            if (currentMonth.before(now)) {
                currentMonth.add(Calendar.MONTH, 1);
                updatePeriodDisplay();
                loadData();
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Observe category stats
        viewModel.getCategoryStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && !stats.isEmpty()) {
                currentStats = stats;
                updatePieChart(stats);
                categoryAdapter.submitList(stats);
                updateCenterText();
            } else {
                currentStats = new ArrayList<>();
                pieChart.clear();
                pieChart.setCenterText("No expenses");
                categoryAdapter.submitList(new ArrayList<>());
            }
        });

        // Observe monthly summary
        viewModel.getMonthlySummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                tvTotalIncome.setText(currencyFormat.format(summary.getIncome()));
                tvTotalExpenses.setText(currencyFormat.format(summary.getExpenses()));
                if (tvSavings != null) {
                    double savings = summary.getIncome() - summary.getExpenses();
                    tvSavings.setText(currencyFormat.format(savings));
                }
            }
        });
    }

    private void updatePieChart(List<CategoryStat> stats) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (int i = 0; i < stats.size(); i++) {
            CategoryStat stat = stats.get(i);
            entries.add(new PieEntry((float) stat.getTotal(), stat.getCategoryName()));
            colors.add(CategoryStatAdapter.CHART_COLORS[i % CategoryStatAdapter.CHART_COLORS.length]);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(8f);
        
        // Draw labels on the slices
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueLineColor(Color.parseColor("#666666"));
        dataSet.setValueTextColor(Color.parseColor("#333333"));
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.parseColor("#333333"));

        pieChart.setData(data);
        pieChart.highlightValues(null);
        pieChart.invalidate();
        pieChart.animateY(800);
    }

    private void updatePeriodDisplay() {
        tvPeriod.setText(monthFormat.format(currentMonth.getTime()));
    }

    private void loadData() {
        int year = currentMonth.get(Calendar.YEAR);
        int month = currentMonth.get(Calendar.MONTH) + 1;
        viewModel.loadMonthlySummary(year, month);
        loadCategoryStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}
