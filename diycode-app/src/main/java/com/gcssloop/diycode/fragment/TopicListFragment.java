/*
 * Copyright 2017 GcsSloop
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last modified 2017-04-08 23:15:33
 *
 * GitHub:  https://github.com/GcsSloop
 * Website: http://www.gcssloop.com
 * Weibo:   http://weibo.com/GcsSloop
 */

package com.gcssloop.diycode.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gcssloop.diycode.R;
import com.gcssloop.diycode.base.recyclerview.SpeedyLinearLayoutManager;
import com.gcssloop.diycode.fragment.provider.TopicProvider;
import com.gcssloop.diycode.utils.Config;
import com.gcssloop.diycode.utils.DataCache;
import com.gcssloop.diycode_sdk.api.Diycode;
import com.gcssloop.diycode_sdk.api.topic.bean.Topic;
import com.gcssloop.diycode_sdk.api.topic.event.GetTopicsListEvent;
import com.gcssloop.diycode_sdk.log.Logger;
import com.gcssloop.recyclerview.adapter.multitype.HeaderFooterAdapter;

import java.util.ArrayList;
import java.util.List;

public class TopicListFragment extends RefreshRecyclerFragment<Topic, GetTopicsListEvent> {

    private boolean isFirstLaunch = true;
    // 数据
    private Config mConfig;         // 配置(状态信息)
    private Diycode mDiycode;       // 在线(服务器)
    private DataCache mDataCache;   // 缓存(本地)

    public static TopicListFragment newInstance() {
        Bundle args = new Bundle();
        TopicListFragment fragment = new TopicListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConfig = Config.getSingleInstance();
        mDiycode = Diycode.getSingleInstance();
        mDataCache = new DataCache(getContext());
    }

    @Override
    public void initData(HeaderFooterAdapter adapter) {
        List<Topic> topics = mDataCache.getTopicsList();
        if (null != topics && topics.size() > 0) {
            Logger.e("topics : " + topics.size());
            pageIndex = mConfig.getTopicListPageIndex();
            adapter.addDatas(topics);
            if (isFirstLaunch) {
                int lastPosition = mConfig.getTopicListLastPosition();
                int lastOffset = mConfig.getTopicListLastOffset();
                if (lastPosition != 0) {
                    mRecyclerView.getLayoutManager().scrollToPosition(lastPosition);
                }
                isFirstAddFooter = false;
                isFirstLaunch = false;
            }
        } else {
            loadMore();
        }
    }

    @Override
    protected void setRecyclerViewAdapter(Context context, RecyclerView recyclerView,
                                          HeaderFooterAdapter adapter) {
        TopicProvider topicProvider = new TopicProvider(getContext(), R.layout.item_topic);
        adapter.register(Topic.class, topicProvider);
    }

    @NonNull
    @Override
    protected RecyclerView.LayoutManager getRecyclerViewLayoutManager() {
        return new SpeedyLinearLayoutManager(getContext());
    }


    @NonNull
    @Override
    protected String request(int offset, int limit) {
        return mDiycode.getTopicsList(null, null, offset, limit);
    }

    @Override
    protected void onRefresh(GetTopicsListEvent event, HeaderFooterAdapter adapter) {
        adapter.clearDatas();
        adapter.addDatas(event.getBean());
        toast("刷新成功");
        mDataCache.saveTopicsList(convert(adapter.getDatas()));
    }

    @Override
    protected void onLoadMore(GetTopicsListEvent event, HeaderFooterAdapter adapter) {
        adapter.addDatas(event.getBean());
        toast("加载更多成功");
        mDataCache.saveTopicsList(convert(adapter.getDatas()));
    }

    @Override
    protected void onError(GetTopicsListEvent event, String postType) {
        if (postType.equals(POST_LOAD_MORE)) {
            toast("加载更多失败");
        } else if (postType.equals(POST_REFRESH)) {
            toast("刷新数据失败");
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Topic> convert(List<Object> objects) {
        ArrayList<Topic> topics = new ArrayList<>();
        for (Object obj : objects) {
            if (obj instanceof Topic)
                topics.add((Topic) obj);
        }
        return topics;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveState();
    }

    // 保存状态
    private void saveState() {
        // 存储 PageIndex
        mConfig.saveTopicListPageIndex(pageIndex);
        // 存储 RecyclerView 滚动位置
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        View view = layoutManager.getChildAt(0);
        int lastPosition = layoutManager.getPosition(view);
        int lastOffset = view.getTop();
        mConfig.saveTopicListState(lastPosition, lastOffset);
        Logger.e("onDestroyView", lastPosition + " : " + lastOffset);
    }
}