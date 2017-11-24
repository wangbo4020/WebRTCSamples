package com.adups.remotecare;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void onClickGoRoom(View view) {
		EditText etNickname = findViewById(R.id.et_nickname);
		EditText etRoomId = findViewById(R.id.et_room_id);

		CharSequence nickname = etNickname.getText().toString();
		CharSequence roomId = etRoomId.getText().toString();

		if (TextUtils.isEmpty(nickname)) {
			Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
			etNickname.requestFocus();
			return;
		}
		if (TextUtils.isEmpty(roomId)) {
			Toast.makeText(this, "请输入房间号", Toast.LENGTH_SHORT).show();
			etRoomId.requestFocus();
			return;
		}

		Intent intent = new Intent(this, RoomActivity.class);
		intent.putExtra(RoomActivity.EXTRA_NICKNAME, nickname);
		intent.putExtra(RoomActivity.EXTRA_ROOM_ID, roomId);
		startActivity(intent);
	}
}
