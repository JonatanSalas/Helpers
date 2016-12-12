package io.blackbox_vision.helpers.ui.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

import io.blackbox_vision.helpers.R;
import io.blackbox_vision.helpers.helper.AppConstants;
import io.blackbox_vision.helpers.helper.DrawableUtils;
import io.blackbox_vision.helpers.logic.error.TaskException;
import io.blackbox_vision.helpers.logic.factory.TaskListPresenterFactory;
import io.blackbox_vision.helpers.logic.model.Task;
import io.blackbox_vision.helpers.logic.presenter.TaskListPresenter;
import io.blackbox_vision.helpers.logic.view.TaskListView;
import io.blackbox_vision.helpers.ui.activity.AddTaskActivity;
import io.blackbox_vision.helpers.ui.adapter.TaskListAdapter;
import io.blackbox_vision.helpers.ui.behaviour.RecyclerViewScrollBehaviour;
import io.blackbox_vision.mvphelpers.logic.factory.PresenterFactory;
import io.blackbox_vision.mvphelpers.ui.fragment.BaseFragment;

import static android.support.v4.view.MenuItemCompat.*;
import static android.support.v7.widget.SearchView.*;


public final class TaskListFragment extends BaseFragment<TaskListPresenter>
        implements TaskListView, OnQueryTextListener, OnActionExpandListener {

    private TaskListAdapter taskListAdapter;

    @BindView(R.id.taskListView)
    RecyclerView taskListView;

    @BindView(R.id.newTaskButton)
    FloatingActionButton newTaskButton;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.errorTextView)
    TextView errorTextView;

    public TaskListFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        taskListAdapter = new TaskListAdapter(getApplicationContext(), new ArrayList<>());
        taskListAdapter.setOnItemSelectedListener(this::handleItemSelected);

        taskListView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        taskListView.addOnScrollListener(new RecyclerViewScrollBehaviour(newTaskButton));
        taskListView.setItemViewCacheSize(1024 * 24);
        taskListView.setAdapter(taskListAdapter);
        taskListView.setHasFixedSize(true);

        taskListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.task_list_menu, menu);
        final MenuItem item = menu.findItem(R.id.action_search);
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (null != actionBar) {
            final SearchView searchView = new SearchView(actionBar.getThemedContext());

            MenuItemCompat.setActionView(item, searchView);

            searchView.setOnQueryTextListener(this);
            searchView.setIconifiedByDefault(false);

            searchView.setOnSearchClickListener(v -> newTaskButton.hide());
            searchView.setOnCloseListener(this::onClose);

            MenuItemCompat.setOnActionExpandListener(item, this);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_all:
                if (null != getPresenter()) {
                    getPresenter().removeAllTasks();
                }

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onClose() {
        if (null != getPresenter()) {
            getPresenter().getTasks();
        }

        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (null != getPresenter()) {
            getPresenter().getTasks();
        }

        newTaskButton.show();

        return true;
    }

    @NonNull
    @Override
    public PresenterFactory<TaskListPresenter> createPresenterFactory() {
        return TaskListPresenterFactory.newInstance();
    }

    @Override
    public int getLayout() {
        return R.layout.fragment_task_list;
    }

    @Override
    public void onPresenterCreated(@NonNull TaskListPresenter presenter) {
        presenter.attachView(this);
        presenter.getTasks();
    }

    @OnClick(R.id.newTaskButton)
    public void onClick(@NonNull View v) {
        if (null != getPresenter()) {
            getPresenter().newTask();
        }
    }

    public void handleItemSelected(@NonNull View v, @NonNull Task task, int position) {
        if (null != getPresenter()) {
            getPresenter().showTask(task.getId());
        }
    }

    @Override
    public void onNewTaskRequest() {
        final Intent intent = new Intent(getApplicationContext(), AddTaskActivity.class);

        intent.putExtra(AppConstants.LAUNCH_MODE, AppConstants.MODE_CREATE);

        startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void onTaskDetailRequest(@NonNull Long id) {
        final Intent intent = new Intent(getApplicationContext(), AddTaskActivity.class);

        intent.putExtra(AppConstants.LAUNCH_MODE, AppConstants.MODE_EDIT);
        intent.putExtra(AppConstants.TASK_ID, id);

        startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void onTaskListFetched(@NonNull List<Task> tasks) {
        taskListAdapter.setItems(tasks);
    }

    @Override
    public void onError(@NonNull Throwable error) {
        switch (error.getMessage()) {
            case TaskException.EMPTY_LIST:
                if (null != getPresenter()) {
                    getPresenter().showEmptyView();
                }

                break;
        }
    }

    @Override
    public void onTasksRemoved() {
        if (null != getPresenter()) {
            getPresenter().showEmptyView();
        }
    }

    @Override
    public void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showTaskList() {
        taskListView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideTaskList() {
        taskListView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showErrorView() {
        errorTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideErrorView() {
        errorTextView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showEmptyView() {
        final Drawable d = DrawableUtils.applyColorFilter(getApplicationContext(), R.drawable.ic_assignment_turned_in_black_48dp);

        errorTextView.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        errorTextView.setText(getString(R.string.empty_task_list));
        errorTextView.setTextSize(16F);
    }
}
