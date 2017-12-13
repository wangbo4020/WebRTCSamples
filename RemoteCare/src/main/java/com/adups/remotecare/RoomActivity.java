package com.adups.remotecare;

import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import org.appspot.apprtc.HudFragment;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.PeerConnectionClient.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.appspot.apprtc.ProxyRenderer;
import org.appspot.apprtc.WebSocketRTCClient;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.adups.remotecare.model.MessageDescription.TYPE_NOTICE;
import static com.adups.remotecare.model.MessageDescription.TYPE_OTHER;
import static com.adups.remotecare.model.MessageDescription.TYPE_SELF;

/**
 * Created by Dylan on 2017/11/23.
 */

public class RoomActivity extends FragmentActivity implements SignalingEvents, PeerConnectionClient.PeerConnectionEvents, PeerConnectionClient.DataChannelObserver {

	public static final String TAG = "RoomActivity";

	public static final String EXTRA_ROOM_ID = "room_id";
	public static final String EXTRA_NICKNAME = "nickname";

	public static final String KEY_NICKNAME = "nickname";
	public static final String KEY_MESSAGE = "message";

	public static final int DC_CHATLOGS = 1;

	private static final String SERVER_ROOM_URL = "https://dev.remotecare.cn:9080";
//	private static final String SERVER_ROOM_URL = "https://appr.tc";

	private AppRTCClient mAppRTCClient;
	private PeerConnectionClient mPeerConnClient;

	private RoomConnectionParameters mRoomConnParams;
	private PeerConnectionParameters mPeerConnParams;
	private SignalingParameters mSignalingParams;
	private HudFragment mHudFragment;
	private long mCallStartedTimeMs;


	private RecyclerView mRecyclerLogs;
	private LinearLayoutManager mLogsLayout;
	private LogsAdapter mLogsAdapter;
	private EditText mEtInput;
	private Button mBtnSend;

	private CharSequence mMineNickname;
	private List<MessageDescription> mLogs = new ArrayList<MessageDescription>();
	private MessageDescription mInput = new MessageDescription();

	private boolean mIceConnected;

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

		mHudFragment = new HudFragment();
		Bundle hudArgs = new Bundle();
//		hudArgs.putBoolean(HudFragment.EXTRA_VIDEO_CALL, false);
		hudArgs.putBoolean(HudFragment.EXTRA_DISPLAY_HUD, true);
		mHudFragment.setArguments(hudArgs);
		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_container, mHudFragment, "HudFragment")
				.show(mHudFragment).commitNow();

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

		mPeerConnClient.createPeerConnectionFactory(getApplicationContext(), mPeerConnParams, this);

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
		if (mInput.getContent() == null || "".equals(mInput.getContent())) {
			return;
		}
		String nickname = (String) mMineNickname;
		String message = mInput.getContent().toString();
		JSONObject json = new JSONObject();
		try {
			json.put(KEY_NICKNAME, nickname);
			json.put(KEY_MESSAGE, message);

			if (mPeerConnClient != null) {
				ByteBuffer bbuf = encode(json.toString());

				DataChannel.Buffer buffer = new DataChannel.Buffer(bbuf, true);
				mPeerConnClient.send(DC_CHATLOGS, buffer);
				clearInputText();
			}
			logs(nickname, message, true);
			commint();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void clearInputText() {
		mEtInput.setText("");
	}

	private void setSendEnabled(boolean enabled) {
		mEtInput.setEnabled(enabled);
		mBtnSend.setEnabled(enabled);
	}

	private void notice(CharSequence msg) {
		Log.d(TAG, "notice: " + msg);
		mLogs.add(new MessageDescription("notice", msg, TYPE_NOTICE));
	}

	private void logs(CharSequence nickname, CharSequence msg, boolean self) {
		Log.d(TAG, "logs: " + nickname + " -> " + msg);
		mLogs.add(new MessageDescription(nickname, msg, self ? TYPE_SELF : TYPE_OTHER));
	}

	private void commint() {
		mLogsAdapter.notifyDataSetChanged();
		mRecyclerLogs.smoothScrollToPosition(mRecyclerLogs.getAdapter().getItemCount() - 1);
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
//				null;
				new DataChannelParameters("Demo-" + DC_CHATLOGS, true, -1, 3, "", true, DC_CHATLOGS);
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
			notice("onConnectedToRoom");

			mSignalingParams = params;

			ProxyRenderer proxy = null;//new ProxyRenderer("localProxyRenderer");
			mPeerConnClient.createPeerConnection(proxy, proxy, null, mSignalingParams, this);

			if (mSignalingParams.initiator) {
				notice("Creating OFFER...");
				// Create offer. Offer SDP will be sent to answering client in
				// PeerConnectionEvents.onLocalDescription event.
				mPeerConnClient.createOffer();
			} else {
				if (mSignalingParams.offerSdp != null) {
					mPeerConnClient.setRemoteDescription(params.offerSdp);
					notice("Creating ANSWER...");
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
		final long delta = System.currentTimeMillis() - mCallStartedTimeMs;
		Log.d(TAG, "onRemoteDescription: " + sdp);
		runOnUiThread(() -> {
			if (mPeerConnClient == null) {
				notice("Received remote SDP for non-initilized peer connection");
				return;
			}
			notice("Received remote " + sdp.type + ", delay=" + delta + "ms");
			mPeerConnClient.setRemoteDescription(sdp);
			if (!mSignalingParams.initiator) {
				notice("Creating ANSWER...");
				// Create answer. Answer SDP will be sent to offering client in
				// PeerConnectionEvents.onLocalDescription event.
				mPeerConnClient.createAnswer();
			}
			commint();
		});
	}

	@Override
	public void onRemoteIceCandidate(IceCandidate candidate) {
		Log.d(TAG, "onRemoteIceCandidate: " + candidate);
		runOnUiThread(() -> {
			if (mPeerConnClient == null) {
				Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
				return;
			}
			mPeerConnClient.addRemoteIceCandidate(candidate);
		});
	}

	@Override
	public void onRemoteIceCandidatesRemoved(IceCandidate[] candidates) {
		Log.d(TAG, "onRemoteIceCandidatesRemoved: " + candidates);
		runOnUiThread(() -> {
			if (mPeerConnClient == null) {
				Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
				return;
			}
			mPeerConnClient.removeRemoteIceCandidates(candidates);
		});
	}

	@Override
	public void onChannelStateChanged(int state, String reason) {
		Log.d(TAG, "onChannelStateChanged: state: " + state + " reason: " + reason);
		runOnUiThread(() -> {
			switch (state) {
			case SignalingEvents.STATE_RECONNECTING:
				notice("Reconnecting...");
				break;
			case SignalingEvents.STATE_CONNECTED:
				notice("Reconnecting...");
				break;
			}
		});
	}

	@Override
	public void onChannelClose() {
		Log.d(TAG, "onChannelClose: ");
		runOnUiThread(() -> {
			notice("Remote end hung up; dropping PeerConnection");
			commint();
		});
	}

	@Override
	public void onChannelError(String description) {
		Log.d(TAG, "onChannelError: " + description);
		runOnUiThread(() -> {
			notice(description);

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
				notice("Sending " + sdp.type + "， delay = " + delta + "ms");
				if (mSignalingParams.initiator) {
					mAppRTCClient.sendOfferSdp(sdp);
				} else {
					mAppRTCClient.sendAnswerSdp(sdp);
				}
			}
			commint();
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
		runOnUiThread(() -> {
			if (mAppRTCClient != null) {
				mAppRTCClient.sendLocalIceCandidateRemovals(candidates);
			}
		});
	}

	@Override
	public void onIceConnected() {
		final long delta = System.currentTimeMillis() - mCallStartedTimeMs;
		Log.d(TAG, "onIceConnected: ");
		runOnUiThread(() -> {
			notice("ICE connected, delay=" + delta + "ms");
			mIceConnected = true;
			commint();
		});
	}

	@Override
	public void onIceDisconnected() {
		Log.d(TAG, "onIceDisconnected: ");
		runOnUiThread(() -> {
			notice("ICE disconnected");
			mIceConnected = false;
			commint();
		});
	}

	@Override
	public void onPeerConnectionClosed() {
		Log.d(TAG, "onPeerConnectionClosed: ");

	}

	@Override
	public void onDataChannelAdded(DataChannel dc) {
		Log.d(TAG, "onDataChannelAdded: label: " + dc.label() + ", state: " + dc.state());
		runOnUiThread(() -> {
			notice("Peer data channel added.");
			setSendEnabled(dc.state() == DataChannel.State.OPEN);
			commint();
		});
	}

	@Override
	public void onPeerConnectionStatsReady(StatsReport[] reports) {
		Log.d(TAG, "onPeerConnectionStatsReady: " + Arrays.toString(reports));

		runOnUiThread(() -> {
			if (mIceConnected) {
				mHudFragment.updateEncoderStatistics(reports);
			}
		});
	}

	@Override
	public void onPeerConnectionError(String description) {
		Log.d(TAG, "onPeerConnectionError: " + description);
		runOnUiThread(() -> {
			notice(description);
			commint();
		});
	}

	// -------------------- DataChannelObserver --------------------

	@Override
	public void onBufferedAmountChange(DataChannel dc, long previousAmount) {
		Log.d(TAG, "Data channel buffered amount changed: " + dc.label() + ": " + dc.state());
	}

	@Override
	public void onStateChange(DataChannel dc) {
		Log.d(TAG, "Data channel state changed: " + dc.label() + ": " + dc.state());
		runOnUiThread(() -> {
			switch (dc.id()) {
				case DC_CHATLOGS:
					setSendEnabled(dc.state() == DataChannel.State.OPEN);
					break;
			}
		});
	}

	@Override
	public void onMessage(final DataChannel dc, final DataChannel.Buffer buffer) {

		runOnUiThread(() -> {
//			if (buffer.binary) {
//				Log.d(TAG, "Received binary msg over " + dc);
//				return;
//			}

			try {
				String strData = decode(buffer.data);
				Log.d(TAG, "Got msg: " + strData);
				JSONObject json = new JSONObject(strData);
				String nickname = json.getString(KEY_NICKNAME);
				String message = json.getString(KEY_MESSAGE);
				logs(nickname, message, false);
				commint();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}

	public static final int WRAP_START_LENGTH = 24;
	public static final int WRAP_END_LENGTH = 4;
	private ByteBuffer encode(String str) throws UnsupportedEncodingException {
		byte[] buf = str.getBytes();
		byte[] wrap = new byte[buf.length + WRAP_START_LENGTH + WRAP_END_LENGTH];
		System.arraycopy(buf, 0, wrap, WRAP_START_LENGTH, buf.length);
		ByteBuffer buffer = ByteBuffer.wrap(wrap);
		Log.i(TAG, "encode: remaining " + buffer.remaining() + ", position " + buffer.position() + ", limit " + buffer.limit() +
				", capacity " + buffer.capacity() + ", bytes-after" + str + " " +
				"\n" + Arrays.toString(wrap)/* + "\n" + toBinaryString(wrap) + "\n" + toHexString(wrap)*/);
		return buffer;
	}

	private String decode(ByteBuffer buffer) throws UnsupportedEncodingException {
		byte[] wrap = new byte[buffer.capacity()];
		buffer.get(wrap);
		byte[] buf = new byte[wrap.length - (WRAP_START_LENGTH + WRAP_END_LENGTH)];
		System.arraycopy(wrap, WRAP_START_LENGTH, buf, 0, buf.length);
		String str = new String(buf);
		Log.d(TAG, "decode: remaining " + buffer.remaining() + ", position " + buffer.position() + ", limit " + buffer.limit() +
				", capacity " + buffer.capacity() + ", bytes-after " + str +
				"\n" + Arrays.toString(wrap)/* + "\n" + toBinaryString(wrap) + "\n" + toHexString(wrap)*/);
		return str;
	}

	private String toHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length);
		for (byte b : bytes) {
			String hex = Integer.toHexString(b);
			if (hex.length() > 2) {
				sb.append("-").append(hex.substring(hex.length() - 2, hex.length()));
			} else {
				sb.append(hex);
			}
			sb.append(" ");
		}
		return sb.toString();
	}

	private String toBinaryString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length);
		for (byte b : bytes) {
			String binary = Integer.toBinaryString(b);
			if (binary.length() < 8) {
				sb.append("00000000".substring(binary.length()));
				sb.append(binary);
			} else if (binary.length() > 8) {
				sb.append(binary.substring(binary.length() - 8, binary.length()));
			}
			sb.append(" ");
		}
		return sb.toString();
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
