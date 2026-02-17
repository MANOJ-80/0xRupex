package com.rupex.app.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.rupex.app.data.remote.ApiClient;
import com.rupex.app.data.remote.RupexApi;
import com.rupex.app.data.remote.model.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FinanceRepository {

    private static final String TAG = "FinanceRepository";
    private static FinanceRepository instance;
    private final RupexApi api;

    private FinanceRepository(Context context) {
        api = ApiClient.getInstance(context).getApi();
    }

    public static synchronized FinanceRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FinanceRepository(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<Resource<List<TransactionDto>>> getTransactions(
            int page, int limit, String type, String startDate, String endDate) {

        MutableLiveData<Resource<List<TransactionDto>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        api.getTransactions(page, limit, type, startDate, endDate).enqueue(
                new Callback<PaginatedApiResponse<TransactionDto>>() {
                    @Override
                    public void onResponse(Call<PaginatedApiResponse<TransactionDto>> call,
                                         Response<PaginatedApiResponse<TransactionDto>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().success) {
                            result.setValue(Resource.success(response.body().data));
                        } else {
                            result.setValue(Resource.error("Failed to load transactions"));
                        }
                    }

                    @Override
                    public void onFailure(Call<PaginatedApiResponse<TransactionDto>> call, Throwable t) {
                        Log.e(TAG, "Failed to load transactions", t);
                        result.setValue(Resource.error(t.getMessage()));
                    }
                });

        return result;
    }

    public LiveData<Resource<List<AccountDto>>> getAccounts() {
        MutableLiveData<Resource<List<AccountDto>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        api.getAccounts().enqueue(new Callback<RupexApi.AccountsResponse>() {
            @Override
            public void onResponse(Call<RupexApi.AccountsResponse> call,
                                 Response<RupexApi.AccountsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    result.setValue(Resource.success(response.body().accounts));
                } else {
                    result.setValue(Resource.error("Failed to load accounts"));
                }
            }

            @Override
            public void onFailure(Call<RupexApi.AccountsResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load accounts", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });

        return result;
    }

    public LiveData<Resource<List<CategoryDto>>> getCategories() {
        MutableLiveData<Resource<List<CategoryDto>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        api.getCategories().enqueue(new Callback<RupexApi.CategoriesResponse>() {
            @Override
            public void onResponse(Call<RupexApi.CategoriesResponse> call,
                                 Response<RupexApi.CategoriesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    result.setValue(Resource.success(response.body().categories));
                } else {
                    result.setValue(Resource.error("Failed to load categories"));
                }
            }

            @Override
            public void onFailure(Call<RupexApi.CategoriesResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load categories", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });

        return result;
    }

    public LiveData<Resource<List<CategoryDto>>> getCategoriesByType(String type) {
        MutableLiveData<Resource<List<CategoryDto>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        api.getCategoriesByType(type).enqueue(new Callback<RupexApi.CategoriesResponse>() {
            @Override
            public void onResponse(Call<RupexApi.CategoriesResponse> call,
                                 Response<RupexApi.CategoriesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    result.setValue(Resource.success(response.body().categories));
                } else {
                    result.setValue(Resource.error("Failed to load categories"));
                }
            }

            @Override
            public void onFailure(Call<RupexApi.CategoriesResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load categories", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });

        return result;
    }

    public LiveData<Resource<RupexApi.MonthlySummaryDto>> getMonthlySummary(int year, int month) {
        MutableLiveData<Resource<RupexApi.MonthlySummaryDto>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        api.getMonthlySummary(year, month).enqueue(new Callback<ApiResponse<RupexApi.SummaryResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<RupexApi.SummaryResponse>> call,
                                 Response<ApiResponse<RupexApi.SummaryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData().summary));
                } else {
                    result.setValue(Resource.error("Failed to load summary"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RupexApi.SummaryResponse>> call, Throwable t) {
                Log.e(TAG, "Failed to load summary", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });

        return result;
    }

    public LiveData<Resource<List<RupexApi.CategoryStatDto>>> getCategoryStats(
            String startDate, String endDate) {

        MutableLiveData<Resource<List<RupexApi.CategoryStatDto>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        api.getCategoryStats(startDate, endDate).enqueue(
                new Callback<ApiResponse<RupexApi.CategoryStatsResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<RupexApi.CategoryStatsResponse>> call,
                                         Response<ApiResponse<RupexApi.CategoryStatsResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            result.setValue(Resource.success(response.body().getData().stats));
                        } else {
                            result.setValue(Resource.error("Failed to load stats"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<RupexApi.CategoryStatsResponse>> call, Throwable t) {
                        Log.e(TAG, "Failed to load stats", t);
                        result.setValue(Resource.error(t.getMessage()));
                    }
                });

        return result;
    }

    public LiveData<Resource<TransactionDto>> createTransaction(CreateTransactionRequest request) {
        MutableLiveData<Resource<TransactionDto>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        api.createTransaction(request).enqueue(new Callback<ApiResponse<TransactionDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<TransactionDto>> call,
                                 Response<ApiResponse<TransactionDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData()));
                } else {
                    String error = response.body() != null ? response.body().getError() : "Failed to create transaction";
                    result.setValue(Resource.error(error));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TransactionDto>> call, Throwable t) {
                Log.e(TAG, "Failed to create transaction", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });

        return result;
    }

    public static class Resource<T> {
        public enum Status { LOADING, SUCCESS, ERROR }

        public final Status status;
        public final T data;
        public final String error;

        private Resource(Status status, T data, String error) {
            this.status = status;
            this.data = data;
            this.error = error;
        }

        public static <T> Resource<T> loading() {
            return new Resource<>(Status.LOADING, null, null);
        }

        public static <T> Resource<T> success(T data) {
            return new Resource<>(Status.SUCCESS, data, null);
        }

        public static <T> Resource<T> error(String error) {
            return new Resource<>(Status.ERROR, null, error);
        }

        public boolean isLoading() { return status == Status.LOADING; }
        public boolean isSuccess() { return status == Status.SUCCESS; }
        public boolean isError() { return status == Status.ERROR; }
    }
}
