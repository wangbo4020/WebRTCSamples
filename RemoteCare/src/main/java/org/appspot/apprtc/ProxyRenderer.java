package org.appspot.apprtc;

import org.webrtc.Logging;
import org.webrtc.VideoFrame;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSink;

/**
 * Created by Dylan on 2017/11/23.
 */

public class ProxyRenderer<T extends VideoRenderer.Callbacks & VideoSink> implements VideoRenderer.Callbacks, VideoSink {

	private String TAG;
	private T target;

	public ProxyRenderer(String TAG) {
		this.TAG = TAG;
	}

	@Override
	synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
		if (target == null) {
			Logging.d(TAG, "Dropping frame in proxy because target is null.");
			VideoRenderer.renderFrameDone(frame);
			return;
		}

		target.renderFrame(frame);
	}

	@Override
	synchronized public void onFrame(VideoFrame frame) {
		if (target == null) {
			Logging.d(TAG, "Dropping frame in proxy because target is null.");
			return;
		}

		target.onFrame(frame);
	}

	synchronized public void setTarget(T target) {
		this.target = target;
	}
}
