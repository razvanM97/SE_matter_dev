package com.matter.tv.server.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.matter.tv.server.R;
import com.matter.tv.server.model.ContentApp;
import com.matter.tv.server.receivers.ContentAppDiscoveryService;
import com.matter.tv.server.service.MatterServant;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass. Use the {@link ContentAppFragment#newInstance} factory method
 * to create an instance of this fragment.
 */
public class ContentAppFragment extends Fragment {

  private static final String TAG = "SettingsFragment";
  private BroadcastReceiver broadcastReceiver;
  private ListView pkgUpdatesView;

  public ContentAppFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment SettingsFragment.
   */
  public static ContentAppFragment newInstance() {
    ContentAppFragment fragment = new ContentAppFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {}
  }

  @Override
  public void onResume() {
    super.onResume();

    ContentAppDiscoveryService.getReceiverInstance().registerSelf(getContext());
    ArrayList<String> lst =
        new ArrayList<String>(
            ContentAppDiscoveryService.getReceiverInstance().getDiscoveredContentApps().keySet());
    ContentAppListAdapter adapter =
        new ContentAppListAdapter(getContext(), R.layout.applist_item, lst);

    pkgUpdatesView = getView().findViewById(R.id.pkgUpdates);
    pkgUpdatesView.setAdapter(adapter);
    registerReceiver(adapter);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_content_app, container, false);
  }

  private class ContentAppListAdapter extends ArrayAdapter<String> {

    private int layout;

    public ContentAppListAdapter(
        @NonNull Context context, int resource, @NonNull ArrayList<String> packages) {
      super(context, resource, packages);
      layout = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
      ViewHolder mainViewHolder;
      if (convertView == null) {
        LayoutInflater inflator = LayoutInflater.from(getContext());
        convertView = inflator.inflate(layout, parent, false);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.appName = convertView.findViewById(R.id.appNameTextView);
        viewHolder.appName.setText(getItem(position));
        viewHolder.sendMessageButton = convertView.findViewById(R.id.sendMessageButton);
        viewHolder.sendMessageButton.setText(R.string.send_command);
        viewHolder.sendMessageButton.setOnClickListener(
            view -> {
              Log.i(TAG, "Button was clicked for " + position);
              for (ContentApp app :
                  ContentAppDiscoveryService.getReceiverInstance()
                      .getDiscoveredContentApps()
                      .values()) {
                if (app.getAppName().equals(getItem(position))) {
                  MatterServant.get().sendTestMessage(app.getEndpointId(), "My Native Message");
                }
              }
            });
        convertView.setTag(viewHolder);
      } else {
        mainViewHolder = (ViewHolder) convertView.getTag();
        mainViewHolder.appName.setText(getItem(position));
      }
      return convertView;
    }
  }

  private void registerReceiver(ArrayAdapter adapter) {
    broadcastReceiver =
        new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String packageName = intent.getStringExtra("com.matter.tv.server.appagent.add.pkg");
            if (action.equals("com.matter.tv.server.appagent.add")
                || action.equals("com.matter.tv.server.appagent.remove")) {
              adapter.clear();
              adapter.addAll(
                  ContentAppDiscoveryService.getReceiverInstance()
                      .getDiscoveredContentApps()
                      .entrySet());
              adapter.notifyDataSetChanged();
            }
          }
        };
    getContext()
        .registerReceiver(broadcastReceiver, new IntentFilter("com.matter.tv.server.appagent.add"));
    getContext()
        .registerReceiver(
            broadcastReceiver, new IntentFilter("com.matter.tv.server.appagent.remove"));
  }

  public class ViewHolder {
    TextView appName;
    Button sendMessageButton;
  }
}
