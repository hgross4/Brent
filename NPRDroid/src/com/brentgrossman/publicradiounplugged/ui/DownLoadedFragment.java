package com.brentgrossman.publicradiounplugged.ui;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ToggleButton;

import com.brentgrossman.publicradiounplugged.R;
import com.brentgrossman.publicradiounplugged.data.CProvider;
import com.brentgrossman.publicradiounplugged.internet.DownloadStories;
import com.brentgrossman.publicradiounplugged.playback.MusicService;
import com.brentgrossman.publicradiounplugged.playback.MusicService.State;

public class DownLoadedFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        OnClickListener, OnSeekBarChangeListener {
    private static DownloadedCursorAdapter adapter = null;
    private static String TAG = DownLoadedFragment.class.getSimpleName();
    private ImageButton rewindButton, playButton, nextButton;
    private static MusicService musicService;
    boolean mBound = false, downloading = false;
    private int mInterval = 1000; // 1 second updates
    private Handler mHandler;
    private SeekBar seekBar;
    static SharedPreferences pref;
    private int playerPosition;
    private int duration;
    private TextView textRemaining;
    private TextView textDuration;
    private int seekBarProgress;
    private TextView selectAllText;
    private CheckBox selectAllCheckBox;
    private ToggleButton selectAllToggle;
    private long id = -1;
    private enum DeleteType { PLAYED, SELECTED};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)  {
        View rootView = inflater.inflate(R.layout.downloaded_fragment, container, false);
        pref = this.getActivity().getSharedPreferences("NPRDownloadPreferences", Context.MODE_PRIVATE);
        adapter = new DownloadedCursorAdapter(getActivity(), R.layout.row, null,
                new String[] { CProvider.Stories.TITLE, CProvider.Stories.PERCENTAGE_PLAYED },
                new int[] { R.id.story, R.id.percentage_played }, 0);
        setListAdapter(adapter);
        getLoaderManager().initLoader(0, null, this);
        rewindButton = (ImageButton) rootView.findViewById(R.id.rewindButton);
        playButton = (ImageButton) rootView.findViewById(R.id.playButton);
        nextButton = (ImageButton) rootView.findViewById(R.id.nextButton);
        rewindButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        mHandler = new Handler();
        textRemaining = (TextView) rootView.findViewById(R.id.textRemaining);
        textDuration = (TextView) rootView.findViewById(R.id.textDuration);
        Button deleteSelected = (Button) rootView.findViewById(R.id.delete_selected_button);
        deleteSelected.setOnClickListener(this);
        Button deletePlayed = (Button) rootView.findViewById(R.id.delete_played_button);
        deletePlayed.setOnClickListener(this);
        selectAllToggle = (ToggleButton) rootView.findViewById(R.id.select_all_for_deletion_toggle);
        selectAllToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final ListView listView = getListView();
                for(int i=0; i < getListAdapter().getCount(); i++){
                    listView.setItemChecked(i, isChecked);
                }
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Bind to MusicService
        Intent intent = new Intent(this.getActivity(), MusicService.class);
        // intent.putExtra("messenger", new Messenger(handler));
        this.getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        startRepeatingTask();
        IntentFilter filter = new IntentFilter(DownloadStories.downloading);
        this.getActivity().registerReceiver(download, filter);
        getListView().smoothScrollToPosition(pref.getInt("listPosition", 0));
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRepeatingTask();
        this.getActivity().unregisterReceiver(download);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            this.getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    static class messageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MusicService.NEXT_ITEM) {
                SharedPreferences.Editor editor = pref.edit();
                int newPosition = musicService.mRetriever.listPosition - 1;
                // make newly playing story highlighted in list, others default color:
                adapter.notifyDataSetChanged();
                // save this position so its list item can be changed later:
                editor.putInt("listPosition", newPosition);
                editor.commit();
            }
        }
    }

    final Messenger mMessenger = new Messenger(new messageHandler());

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to MusicService, cast the IBinder and get
            // MusicService instance
            musicService = MusicService.getService();
            mBound = true;
            try {
                Message msg = Message.obtain(null, MusicService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                (new Messenger(service)).send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    Runnable mStatusChecker = new Runnable() {

        @Override
        public void run() {
            mHandler.postDelayed(mStatusChecker, mInterval);
            if (musicService != null) {
                switch (musicService.mState) {
                    case Playing:
                        playButton.setImageResource(R.drawable.pause_button_normal);
                        break;
                    case Paused:
                        playButton.setImageResource(R.drawable.play_button_normal);
                        break;
                    case Preparing:
                        playButton.setImageResource(R.drawable.play_button_pressed);
                        break;
                    default:
                        playButton.setImageResource(R.drawable.play_button_normal);
                        break;
                }
                duration = musicService.getDuration();
                seekBar.setMax(duration);
                playerPosition = musicService.getPlayerPosition();
                seekBar.setProgress(playerPosition);
                int remaining = (duration - playerPosition)/1000;
                textRemaining.setText(timeString(remaining));
                textDuration.setText(timeString(duration / 1000));
                float percentagePlayed = ((float)(duration - remaining*1000))/((float) duration);
                ContentValues values = new ContentValues();
                values.put(CProvider.Stories.PERCENTAGE_PLAYED, percentagePlayed);
                long selectedId = getListAdapter().getItemId(pref.getInt("listPosition", 0));
                DownLoadedFragment.this.getActivity().getContentResolver()
                        .update(CProvider.Stories.CONTENT_URI, values,
                                CProvider.Stories._ID + " = ? ",
                                new String[]{String.valueOf(selectedId)});
            } else
                Log.e(TAG, "musicService is NULL!");
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    private String timeString(int totalSeconds) {
        int minutes = (totalSeconds) / 60;
        int intSeconds = (totalSeconds) % 60;
        String seconds = intSeconds > 9 ? "" + intSeconds : "0" + intSeconds;
        return (minutes + ":" + seconds);
    }

    @Override
    public void onClick(View v) {
        if (v == playButton) {
            if (musicService.mState == State.Playing) {
                this.getActivity().startService(new Intent(MusicService.ACTION_PAUSE));
            } else
                this.getActivity().startService(new Intent(MusicService.ACTION_PLAY));
        } else if (v == rewindButton) {
            musicService.processSeekRequest((playerPosition - 30000));
        } else if (v == nextButton) {
            this.getActivity().startService(new Intent(MusicService.ACTION_SKIP));
            nextButton.setImageResource(R.drawable.next_button_pressed);
            nextButton.setImageResource(0); // restores button to "normal",
            // where
            // R.drawable.next_button_normal
            // produces weird effects
            playButton.setImageResource(R.drawable.play_button_pressed);
        }
        else if (v.getId() == R.id.delete_selected_button) deleteSelectedStories();
        else if (v.getId() == R.id.delete_played_button) deletePlayedStories();
    }

    private void deleteSelectedStories() {
        new DeleteStoriesTask(this.getActivity(), DeleteType.SELECTED).execute();
    }

    private void deletePlayedStories() {
        new DeleteStoriesTask(this.getActivity(), DeleteType.PLAYED).execute();
    }

    class DeleteStoriesTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private DeleteType deleteType;
        public DeleteStoriesTask(Context context, DeleteType deleteType){
            this.context = context;
            this.deleteType = deleteType;
        }
        @Override
        protected Void doInBackground(Void... params) {
            // In the following 2 lines get the id of the currently playing or paused story
            // so that that story can be maintained as the current story after deletion,
            // if it's not one of the stories being deleted
            int listPosition = pref.getInt("listPosition", 0);
            id = adapter.getItemId(listPosition);
            switch (deleteType) {
                case SELECTED:
                    long[] selectedStories = getListView().getCheckedItemIds();
                    final int length = selectedStories.length;
                    final String[] selectedStoriesStrings = new String[length];
                    for (int i = 0; i < length; i++) {
                        long selectedStory = selectedStories[i];
                        selectedStoriesStrings[i] = String.valueOf(selectedStory);
                        // Don't save the id if the currently playing/highlighted story is being deleted,
                        // stop playback, and set the list position to the first item
                        if (id == selectedStory) {
                            id = -1;
                            if (musicService.mState != null) {
                                DownLoadedFragment.this.getActivity()
                                        .startService(new Intent(MusicService.ACTION_STOP));
                            }
                            SharedPreferences.Editor editor = pref.edit();
                            if (musicService != null && musicService.mRetriever != null) {
                                musicService.mRetriever.listPosition = 1;
                            }
                            // save this position so its list item can be changed later:
                            editor.putInt("listPosition", 0);
                            editor.commit();
                        }
                    }
                    if (length > 0) {
                        if (length == adapter.getCount()) {
                            // Delete everything in files directory, just to make sure no files are "sticking around"
                            String sdPath = context.getExternalFilesDir(null).getAbsolutePath() + "/";
                            File sdPathFile = new File(sdPath);
                            File[] files = sdPathFile.listFiles();
                            if (files.length > 0) {
                                for (File file : files) {
                                    file.delete();
                                }
                            }
                            context.getContentResolver().delete(CProvider.Stories.CONTENT_URI,
                                    CProvider.Stories.DOWNLOADED + " = ? ", new String[]{"1"});
                        }
                        else {
                            Cursor cursor = context.getContentResolver().query(CProvider.Stories.CONTENT_URI, new String[]{CProvider.Stories.FILE_NAME},
                                    CProvider.Stories._ID + " IN (" + makePlaceholders(length) + ")", selectedStoriesStrings, null);
                            if (cursor != null) {
                                while (cursor.moveToNext()) {
                                    String filePath = context.getExternalFilesDir(null).getAbsolutePath() + "/" + cursor.getString(cursor.getColumnIndex(CProvider.Stories.FILE_NAME));
                                    File file = new File(filePath);
                                    file.delete();
                                }
                            }
                            context.getContentResolver().delete(CProvider.Stories.CONTENT_URI,
                                    CProvider.Stories._ID + " IN (" + makePlaceholders(length) + ")", selectedStoriesStrings);
                        }
                    }
                    else {
                        DownLoadedFragment.this.getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(context, "No stories selected.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    break;
                case PLAYED:
                    String where = CProvider.Stories.PERCENTAGE_PLAYED + " = ?";
                    String[] whereArgs = new String[] {"1"};
                    Cursor cursor = context.getContentResolver().query(CProvider.Stories.CONTENT_URI,
                            new String[] { CProvider.Stories.FILE_NAME }, where, whereArgs, null);
                    if (cursor != null && cursor.getCount() > 0) {
                        if (cursor.getCount() == adapter.getCount()) {
                            // Delete everything in files directory, just to make sure no files are "sticking around"
                            String sdPath = context.getExternalFilesDir(null).getAbsolutePath() + "/";
                            File sdPathFile = new File(sdPath);
                            File[] files = sdPathFile.listFiles();
                            if (files.length > 0) {
                                for (File file : files) {
                                    file.delete();
                                }
                            }
                            context.getContentResolver().delete(CProvider.Stories.CONTENT_URI,
                                    CProvider.Stories.DOWNLOADED + " = ? ", new String[]{"1"});
                        }
                        else {
                            while (cursor.moveToNext()) {
                                String filePath = context.getExternalFilesDir(null).getAbsolutePath() + "/" +
                                        cursor.getString(cursor.getColumnIndex(CProvider.Stories.FILE_NAME));
                                File file = new File(filePath);
                                file.delete();
                            }
                        }
                        context.getContentResolver().delete(CProvider.Stories.CONTENT_URI, where, whereArgs);
                    }
                    else {
                        DownLoadedFragment.this.getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(context, "There are no completely played stories.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            selectAllToggle.setChecked(false);
        }
    }

    private String makePlaceholders(int length) {
        if (length < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(length * 2 - 1);
            sb.append("?");
            for (int i = 1; i < length; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(MusicService.ACTION_URL);
        Cursor cursor = this.getActivity().getContentResolver().query(CProvider.Stories.CONTENT_URI,
                new String[] {CProvider.Stories.FILE_NAME},
                CProvider.Stories._ID + " = ? ", new String[] {Long.toString(id)}, null);
        if (cursor != null && cursor.moveToFirst()) {
            String fileName = cursor.getString(cursor.getColumnIndex(CProvider.Stories.FILE_NAME));
            intent.putExtra("fileName", fileName);
            intent.putExtra("listPosition", position);
            playButton.setImageResource(R.drawable.play_button_pressed);
            this.getActivity().startService((intent));
            cursor.close();
        }
        getListView().setItemChecked(position, false); // selecting an item is only done for deletion, by checking the checkboxes or "select all"
    }

    private BroadcastReceiver download = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent i) {
            if (i.getBooleanExtra(DownloadStories.downloadDone, false)) {
                downloading = false;
            }
            // Leaving this here for now, since not sure why it was here before of if it may still be needed, but it generates error now:
            //				removeStickyBroadcast(i);
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser == true) {
            seekBarProgress = progress;
            int remaining = (duration - progress)/1000;
            textRemaining.setText(timeString(remaining));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        musicService.processSeekRequest(seekBarProgress);	// Was causing mediaplayer to "hiccough" when in onProgressChanged
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(this.getActivity(), CProvider.Stories.CONTENT_URI,
                new String[] { CProvider.Stories._ID, CProvider.Stories.TITLE, CProvider.Stories.PERCENTAGE_PLAYED },
                CProvider.Stories.DOWNLOADED + " = ? ", new String[] {"1"}, CProvider.Stories._ID);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
        adapter.swapCursor(arg1);
        // If onLoadFinished called because 1 or more stories deleted,
        // make sure the highlighted/playing story is still highlighted
        if (id != -1) {
            int listPosition = 0;
            getLoaderManager().restartLoader(0, null, DownLoadedFragment.this);
            adapter.notifyDataSetChanged();
            for (int position = 0; position < adapter.getCount(); position++) {
                if (adapter.getItemId(position) == id) {
                    listPosition = position;
                    break;
                }
            }
            // Set the current story to be what it was before deletion (or the first one if current story was deleted)
            SharedPreferences.Editor editor = pref.edit();
            if (musicService != null && musicService.mRetriever != null) {
                musicService.mRetriever.listPosition = listPosition + 1;
            }
            // save this position so its list item can be changed later:
            editor.putInt("listPosition", listPosition);
            editor.commit();
            // Scroll to the currently playing story
            getListView().smoothScrollToPosition(pref.getInt("listPosition", 0));
            id = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        adapter.swapCursor(null);

    }

    class DownloadedCursorAdapter extends SimpleCursorAdapter {

        DownloadedCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = super.getView(position, convertView, parent);
            ViewHolder holder = (ViewHolder)row.getTag();
            if (holder == null) {
                holder = new ViewHolder(row);
                row.setTag(holder);
            }
            if (position == pref.getInt("listPosition", 0)) {
                holder.story.setTextColor(getResources().getColor(R.color.npr_blue));
            }
            else {
                holder.story.setTextColor(Color.DKGRAY);
            }
            long rowId = this.getItemId(position);
            Cursor cursor = DownLoadedFragment.this.getActivity().getContentResolver()
                    .query(CProvider.Stories.CONTENT_URI,
                            new String[]{CProvider.Stories.PERCENTAGE_PLAYED},
                            CProvider.Stories._ID + " = ? ", new String[]{Long.toString(rowId)}, null);
            if (cursor != null && cursor.moveToFirst()) {
                float percentagePlayed =
                        cursor.getFloat(cursor.getColumnIndex(CProvider.Stories.PERCENTAGE_PLAYED));
                if (percentagePlayed > 0) {
                    holder.percentagePlayed.setText(Integer.toString((int) (percentagePlayed * 100)) +"%");
                }
                if (percentagePlayed == 1) {
                    row.setBackgroundColor(Color.LTGRAY);
                }
                else {
                    row.setBackgroundColor(Color.TRANSPARENT);
                }
                cursor.close();
            }
            // Set list items as properly selected or not based on the state of their corresponding checkboxes
            // (see http://stackoverflow.com/questions/25026347/check-and-uncheck-all-checkboxes-in-listview)
            holder.storyCheckBox.setChecked(false);
            long [] checkedIds = getListView().getCheckedItemIds();
            if (checkedIds != null) {
                for (int i = 0; i < checkedIds.length; i++) {
                    if (checkedIds[i] == getListAdapter().getItemId(position)) {
                        holder.storyCheckBox.setChecked(true);
                        break;
                    }
                }
            }
            final boolean isChecked = holder.storyCheckBox.isChecked();
            holder.storyCheckBox.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    getListView().setItemChecked(position, !isChecked);
                }
            });
            return(row);
        }
    }

    class ViewHolder {
        CheckBox storyCheckBox;
        TextView percentagePlayed;
        TextView story = null;

        ViewHolder(View row) {
            storyCheckBox = (CheckBox) row.findViewById(R.id.story_check_box);
            percentagePlayed = (TextView) row.findViewById(R.id.percentage_played);
            story = (TextView) row.findViewById(R.id.story);
        }
    }
}