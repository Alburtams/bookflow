package com.hust.bookflow.model.bean;

/**
 * Created by ChinaLHR on 2016/12/24.
 * Email:13435500980@163.com
 */

public class BookHttpResult<T> {

    private int count;
    private T books;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public T getBooks() {
        return books;
    }

    public void setBooks(T books) {
        this.books = books;
    }


}
