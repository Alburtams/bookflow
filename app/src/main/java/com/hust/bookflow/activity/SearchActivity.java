package com.hust.bookflow.activity;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.hust.bookflow.MyApplication;
import com.hust.bookflow.R;
import com.hust.bookflow.adapter.SearchBookAdapter;
import com.hust.bookflow.adapter.SpinnerAdapter;
import com.hust.bookflow.adapter.base.BaseSearchAdapter;
import com.hust.bookflow.model.bean.BookListBeans;
import com.hust.bookflow.model.bean.SpinnerBean;
import com.hust.bookflow.model.httputils.BookFlowHttpMethods;
import com.hust.bookflow.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;

/**
 * Created by ChinaLHR on 2016/12/13.
 * Email:13435500980@163.com
 */

public class SearchActivity extends AppCompatActivity {
    private android.support.v7.widget.SearchView search;
    private Spinner spinner;
    private int SEARCH_NAME = 0;
    private int SEARCH_TAG = 0;
    private final int COUNT = 10;
    private int mStart = 0;
    private String q;


    private SearchBookAdapter mBookAdapter;
    private RecyclerView searchrv;

    //private Subscriber<List<BooksBean>> moviesub;
    private Subscriber<List<BookListBeans>> booksub;

    private List<BookListBeans> mBookBean;
    private RecyclerView.LayoutManager mLayoutManager;
    private View mFootView;
    private ProgressBar searchpb;
    private FloatingActionButton searchfab;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        this.searchfab = (FloatingActionButton) findViewById(R.id.search_fab);
        this.searchpb = (ProgressBar) findViewById(R.id.search_pb);
        this.searchrv = (RecyclerView) findViewById(R.id.search_rv);
        this.spinner = (Spinner) findViewById(R.id.spinner);
        this.search = (SearchView) findViewById(R.id.search);
        initView();
        initListener();
    }

    private void initListener() {
        //查询监听
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            //点击查询按钮
            @Override
            public boolean onQueryTextSubmit(String query) {
                q = query;
                query(query);
                return true;
            }
            //查询框文字发送发生变化
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    SEARCH_TAG = SEARCH_NAME;
                } else if (i == 1) {
                    //SEARCH_TAG = SEARCH_BOOKID;
                    SEARCH_TAG = SEARCH_NAME;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        searchrv.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1) && MyApplication.isNetworkAvailable(SearchActivity.this) && mBookBean.size() == 10) {
                    updateBook();
                    mFootView.setVisibility(View.VISIBLE);
                    searchrv.scrollToPosition(mBookAdapter.getItemCount() - 1);
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        searchfab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchrv.scrollToPosition(0);
            }
        });
    }

    private void updateBook() {
        if (mBookAdapter.getStart() == mStart) return;
        mStart = mBookAdapter.getStart();
        booksub = new Subscriber<List<BookListBeans>>() {
            @Override
            public void onCompleted() {
                mFootView.setVisibility(View.GONE);
            }
            @Override
            public void onError(Throwable e) {
                mFootView.setVisibility(View.GONE);
            }
            @Override
            public void onNext(List<BookListBeans> list) {
                if (!list.isEmpty()) {
                    mBookAdapter.addData(list);
                }
            }
        };
        BookFlowHttpMethods.getInstance().getBookByName(booksub, q, mStart, COUNT);
    }

    private void initView() {
        SearchManager mSearchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        search.setSearchableInfo(mSearchManager.getSearchableInfo(getComponentName()));
        //开启输入文字的清除与查询按钮
        search.setSubmitButtonEnabled(true);
        List<SpinnerBean> list = new ArrayList<>();
        list.add(new SpinnerBean("书名", R.drawable.search_movie));
        //list.add(new SpinnerBean("编号", R.drawable.search_book));

        SpinnerAdapter madapter = new SpinnerAdapter(this, list);
        spinner.setAdapter(madapter);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        searchrv.setLayoutManager(mLayoutManager);
        mFootView = LayoutInflater.from(this).inflate(R.layout.item_footer, searchrv, false);
    }

    private void query(String query) {
        SearchByName(query);
        search.setQuery("", false);
        search.onActionViewCollapsed();
    }

    private void SearchByName(String query) {
        showProgressbar();
        booksub = new Subscriber<List<BookListBeans>>() {
            @Override
            public void onCompleted() {
                closeProgressbar();
            }
            @Override
            public void onError(Throwable e) {
                closeProgressbar();
                ToastUtils.show(SearchActivity.this, "没有搜索到结果");
            }
            @Override
            public void onNext(List<BookListBeans> subjectsBeen) {
                if (!subjectsBeen.isEmpty()) {
                    mBookBean = subjectsBeen;
                    initRecyclerView();
                } else {
                    ToastUtils.show(SearchActivity.this, "没有搜索到结果");
                }
            }
        };
        BookFlowHttpMethods.getInstance().getBookByName(booksub, query,0,COUNT);
    }

    private void initRecyclerView() {
//        searchfab.setVisibility(View.VISIBLE);
        mBookAdapter = new SearchBookAdapter(this, mBookBean);
        mBookAdapter.setFooterView(mFootView);
        searchrv.setAdapter(mBookAdapter);
        mBookAdapter.setOnItemClickListener(new BaseSearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String id, String url) {
                BookDetailsActivity.toActivity(SearchActivity.this, id, url);
            }
        });
    }

    private void showProgressbar() {
        searchpb.setVisibility(View.VISIBLE);
    }

    private void closeProgressbar() {
        searchpb.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        if (booksub!=null) booksub.unsubscribe();
        super.onDestroy();
    }
}
