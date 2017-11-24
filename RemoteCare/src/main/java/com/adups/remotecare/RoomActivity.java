package com.adups.remotecare;

import android.app.Activity;
import android.content.Context;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.adups.remotecare.databinding.ActivityRoomBinding;
import com.adups.remotecare.databinding.ItemLogsMineBinding;
import com.adups.remotecare.databinding.ItemLogsNoticeBinding;
import com.adups.remotecare.databinding.ItemLogsOtherBinding;
import com.adups.remotecare.model.MessageDescription;

import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.AppRTCClient.RoomConnectionParameters;
import org.appspot.apprtc.AppRTCClient.SignalingEvents;
import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.DirectRTCClient;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.PeerConnectionClient.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.appspot.apprtc.ProxyRenderer;
import org.appspot.apprtc.WebSocketRTCClient;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.FileVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;
import static com.adups.remotecare.model.MessageDescription.TYPE_NOTICE;
import static com.adups.remotecare.model.MessageDescription.TYPE_OTHER;
import static com.adups.remotecare.model.MessageDescription.TYPE_SELF;

/**
 * Created by Dylan on 2017/11/23.
 */

public class RoomActivity extends Activity implements SignalingEvents, PeerConnectionClient.PeerConnectionEvents {

	public static final String TAG = "RoomActivity";

	public static final String EXTRA_ROOM_ID = "room_id";
	public static final String EXTRA_NICKNAME = "nickname";

	private static final String SERVER_ROOM_URL = "https://dev.remotecare.cn:9080";

	private AppRTCClient mAppRTCClient;
	private PeerConnectionClient mPeerConnClient;

	private RoomConnectionParameters mRoomConnParams;
	private PeerConnectionParameters mPeerConnParams;
	private SignalingParameters mSignalingParams;
	private long mCallStartedTimeMs;


	private RecyclerView mRecyclerLogs;
	private LinearLayoutManager mLogsLayout;
	private LogsAdapter mLogsAdapter;
	private EditText mEtInput;
	private Button mBtnSend;

	private CharSequence mMineNickname;
	private List<MessageDescription> mLogs = new ArrayList<MessageDescription>();
	private MessageDescription mInput = new MessageDescription();

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Data binding
		ActivityRoomBinding binding = ActivityRoomBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		binding.setInput(mInput);

		// Obtain parameters
		Bundle data = getIntent().getExtras();
		String nickname = data.getString(EXTRA_NICKNAME);
		String roomId = data.getString(EXTRA_ROOM_ID);

		if (TextUtils.isEmpty(roomId)) {
			Toast.makeText(this, "房间号不正确", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		mInput.setNickname(mMineNickname = nickname);

		// Initialize view
		mLogsLayout = new LinearLayoutManager(this);
		mLogsAdapter = new LogsAdapter(getLayoutInflater(), mLogs);

		mRecyclerLogs = findViewById(R.id.recycler_logs);
		mRecyclerLogs.setLayoutManager(mLogsLayout);
		mRecyclerLogs.setAdapter(mLogsAdapter);

		mEtInput = findViewById(R.id.et_input);
		mBtnSend = findViewById(R.id.btn_send);
		setSendEnabled(false);

		// Create peer connection client.
		mAppRTCClient = createAppRTCClient(roomId, this);
		mPeerConnClient = new PeerConnectionClient();

		mRoomConnParams = createRoomConnectionParameters(SERVER_ROOM_URL, roomId);
		mPeerConnParams = createPeerConnectionParameters();

		mPeerConnClient.createPeerConnectionFactory(this, mPeerConnParams, this);

		mCallStartedTimeMs = System.currentTimeMillis();
		mAppRTCClient.connectToRoom(mRoomConnParams);
	}

	@Override
	protected void onDestroy() {

		if (mAppRTCClient != null) {
			mAppRTCClient.disconnectFromRoom();
		}
		if (mPeerConnClient != null) {
			mPeerConnClient.close();
		}
		super.onDestroy();
	}

	public void onClickSend(View view) {

	}

	private void setSendEnabled(boolean enabled) {
		mEtInput.setEnabled(enabled);
		mBtnSend.setEnabled(enabled);
	}

	private void addNotice(CharSequence msg) {
		Log.d(TAG, "addNotice: " + msg);
		mLogs.add(new MessageDescription("notice", msg, TYPE_NOTICE));
	}

	private void commint() {
		mLogsAdapter.notifyDataSetChanged();
	}

	private AppRTCClient createAppRTCClient(String roomId, SignalingEvents events) {

		// Create connection client. Use DirectRTCClient if room name is an IP otherwise use the
		// standard WebSocketRTCClient.
		if (DirectRTCClient.IP_PATTERN.matcher(roomId).matches()) {
			Log.i(TAG, "Using DirectRTCClient because room name looks like an IP.");
			return new DirectRTCClient(events);
		} else {
			return new WebSocketRTCClient(events);
		}
	}

	private RoomConnectionParameters createRoomConnectionParameters(String roomUrl, String roomId) {
		return new RoomConnectionParameters(roomUrl, roomId, false);
	}

	private PeerConnectionParameters createPeerConnectionParameters() {
		DataChannelParameters dataChannelParameters =
				new DataChannelParameters(true, -1, 3, "", true, 1);
		return new PeerConnectionParameters(
				false, false, false,
				0, 0, 0, 0,
				null, false, false,
				0, null, true, false,
				false, false, false,
				false, false, false,
				dataChannelParameters);
	}

	// -------------------- SignalingEvents --------------------

	@Override
	public void onConnectedToRoom(final SignalingParameters params) {
		Log.d(TAG, "onConnectedToRoom: " + params);
		runOnUiThread(() -> {
			addNotice("onConnectedToRoom");

			mSignalingParams = params;

			ProxyRenderer proxy = new ProxyRenderer("localProxyRenderer");
			mPeerConnClient.createPeerConnection(proxy, proxy, null, mSignalingParams);

			if (mSignalingParams.initiator) {
				addNotice("Create Offer");
				// Create offer. Offer SDP will be sent to answering client in
				// PeerConnectionEvents.onLocalDescription event.
				mPeerConnClient.createOffer();
			} else {
				if (mSignalingParams.offerSdp != null) {
					mPeerConnClient.setRemoteDescription(params.offerSdp);
					addNotice("Create Answer");
					// Create answer. Answer SDP will be sent to offering client in
					// PeerConnectionEvents.onLocalDescription event.
					mPeerConnClient.createAnswer();
				}
				if (mSignalingParams.iceCandidates != null) {
					// Add remote ICE candidates from room.
					for (IceCandidate iceCandidate : params.iceCandidates) {
						mPeerConnClient.addRemoteIceCandidate(iceCandidate);
					}
				}
			}

			commint();
		});
	}

	@Override
	public void onRemoteDescription(SessionDescription sdp) {
		Log.d(TAG, "onRemoteDescription: " + sdp);
	}

	@Override
	public void onRemoteIceCandidate(IceCandidate candidate) {
		Log.d(TAG, "onRemoteIceCandidate: " + candidate);

	}

	@Override
	public void onRemoteIceCandidatesRemoved(IceCandidate[] candidates) {
		Log.d(TAG, "onRemoteIceCandidatesRemoved: " + candidates);

	}

	@Override
	public void onChannelClose() {
		Log.d(TAG, "onChannelClose: ");

	}

	@Override
	public void onChannelError(String description) {
		Log.d(TAG, "onChannelError: " + description);
		runOnUiThread(() -> {
			addNotice(description);

			commint();
		});
	}

	// -------------------- PeerConnectionEvents --------------------

	@Override
	public void onLocalDescription(SessionDescription sdp) {
		Log.d(TAG, "onLocalDescription: " + sdp);
		final long delta = System.currentTimeMillis() - mCallStartedTimeMs;
		runOnUiThread(() -> {
			if (mAppRTCClient != null) {
				addNotice("Sending " + sdp.type + "， delay = " + delta + "ms");
				if (mSignalingParams.initiator) {
					mAppRTCClient.sendOfferSdp(sdp);
				} else {
					mAppRTCClient.sendAnswerSdp(sdp);
				}
			}
		});
	}

	@Override
	public void onIceCandidate(IceCandidate candidate) {
		Log.d(TAG, "onIceCandidate: " + candidate);
		runOnUiThread(() -> {
			if (mAppRTCClient != null) {
				mAppRTCClient.sendLocalIceCandidate(candidate);
			}
		});
	}

	@Override
	public void onIceCandidatesRemoved(IceCandidate[] candidates) {
		Log.d(TAG, "onIceCandidatesRemoved: " + candidates);

	}

	@Override
	public void onIceConnected() {
		Log.d(TAG, "onIceConnected: ");

	}

	@Override
	public void onIceDisconnected() {
		Log.d(TAG, "onIceDisconnected: ");

	}

	@Override
	public void onPeerConnectionClosed() {
		Log.d(TAG, "onPeerConnectionClosed: ");

	}

	@Override
	public void onPeerConnectionStatsReady(StatsReport[] reports) {
		Log.d(TAG, "onPeerConnectionStatsReady: " + Arrays.toString(reports));

	}

	@Override
	public void onPeerConnectionError(String description) {
		Log.d(TAG, "onPeerConnectionError: " + description);
		runOnUiThread(() -> {
			addNotice(description);
			commint();
		});
	}

	static class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogsHolder> {

		private LayoutInflater mInflater;
		private List<MessageDescription> mLogs;

		public LogsAdapter(LayoutInflater inflater, List<MessageDescription> logs) {
			this.mInflater = inflater;
			this.mLogs = logs;
		}

		@Override
		public LogsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			switch (viewType) {
				case TYPE_NOTICE:
					return new LogsHolder(ItemLogsNoticeBinding.inflate(mInflater, parent, false));
				case TYPE_OTHER:
					return new LogsHolder(ItemLogsOtherBinding.inflate(mInflater, parent, false));
				case TYPE_SELF:
					return new LogsHolder(ItemLogsMineBinding.inflate(mInflater, parent, false));
				default:
					return null;
			}
		}

		@Override
		public int getItemViewType(int position) {
			return mLogs.get(position).getType();
		}

		@Override
		public void onBindViewHolder(LogsHolder holder, int position) {
			holder.setData(mLogs.get(position));
		}

		@Override
		public int getItemCount() {
			return mLogs.size();
		}

		class LogsHolder extends ViewHolder {

			private ViewDataBinding mBinding;

			public LogsHolder(ItemLogsNoticeBinding binding) {
				super(binding.getRoot());
				mBinding = binding;
			}

			public LogsHolder(ItemLogsMineBinding binding) {
				super(binding.getRoot());
				mBinding = binding;
			}

			public LogsHolder(ItemLogsOtherBinding binding) {
				super(binding.getRoot());
				mBinding = binding;
			}

			public void setData(MessageDescription msgDesc) {
				if (mBinding instanceof ItemLogsNoticeBinding) {
					((ItemLogsNoticeBinding) mBinding).setMsg(msgDesc);
				} else if (mBinding instanceof ItemLogsOtherBinding) {
					((ItemLogsOtherBinding) mBinding).setMsg(msgDesc);
				} else if (mBinding instanceof ItemLogsMineBinding) {
					((ItemLogsMineBinding) mBinding).setMsg(msgDesc);
				}
			}
		}
	}
}