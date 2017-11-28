package com.adups.remotecare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

	private SharedPreferences mPref;
	EditText mEtNickname;
	EditText mEtRoomId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mPref = getSharedPreferences(getPackageName() + "_pref", Context.MODE_PRIVATE);

		mEtNickname = findViewById(R.id.et_nickname);
		mEtRoomId = findViewById(R.id.et_room_id);

		if (mPref.contains(RoomActivity.EXTRA_NICKNAME)) {
			mEtNickname.setText(mPref.getString(RoomActivity.EXTRA_NICKNAME, ""));
		}
		if (mPref.contains(RoomActivity.EXTRA_ROOM_ID)) {
			mEtRoomId.setText(mPref.getString(RoomActivity.EXTRA_ROOM_ID, ""));
		}
	}

	public void onClickGoRoom(View view) {

		CharSequence nickname = mEtNickname.getText().toString();
		CharSequence roomId = mEtRoomId.getText().toString();

		if (TextUtils.isEmpty(nickname)) {
			Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
			mEtNickname.requestFocus();
			return;
		}
		if (TextUtils.isEmpty(roomId)) {
			Toast.makeText(this, "请输入房间号", Toast.LENGTH_SHORT).show();
			mEtRoomId.requestFocus();
			return;
		}

		mPref.edit().putString(RoomActivity.EXTRA_NICKNAME, (String) nickname)
				.putString(RoomActivity.EXTRA_ROOM_ID, (String) roomId).commit();

		Intent intent = new Intent(this, RoomActivity.class);
		intent.putExtra(RoomActivity.EXTRA_NICKNAME, nickname);
		intent.putExtra(RoomActivity.EXTRA_ROOM_ID, roomId);
		startActivity(intent);
	}
}
