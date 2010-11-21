/**
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 * 
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 * 
 */

package org.puredata.android.fifths;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;
import org.puredata.core.utils.PdUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.Toast;


public class CircleOfFifths extends Activity implements OnClickListener {

	private static final String PD_CIRCLE = "Pd Circle Of Fifths";
	private static final String TOP = "top";
	private static final int SAMPLE_RATE = 44100;
	private String patch;
	private RadioGroup options;
	private int option = 0;

	private void post(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), PD_CIRCLE + ": " + msg, Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initGui();
		initPd();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (AudioParameters.suggestSampleRate() < SAMPLE_RATE) {
			post("required sample rate not available; exiting");
			finish();
			return;
		}
		int nOut = Math.min(AudioParameters.suggestOutputChannels(), 2);
		if (nOut == 0) {
			post("audio output not available; exiting");
			finish();
			return;
		}
		try {
			PdAudio.initAudio(SAMPLE_RATE, 0, nOut, 1, true);
			PdAudio.startAudio(this);
		} catch (IOException e) {
			Log.e(PD_CIRCLE, e.toString());
		}
	}
	
	@Override
	protected void onPause() {
		PdAudio.stopAudio();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		cleanup();
		super.onDestroy();
	}
	
	@Override
	public void finish() {
		cleanup();
		super.finish();
	}

	private void initGui() {
		setContentView(R.layout.main);
		CircleView circle = (CircleView) findViewById(R.id.circleview);
		circle.setOwner(this);
		int top = getPreferences(MODE_PRIVATE).getInt(TOP, 0);
		circle.setTop(top);
		options = (RadioGroup) findViewById(R.id.options);
		findViewById(R.id.domdim).setOnClickListener(this);
		findViewById(R.id.majmin).setOnClickListener(this);
		findViewById(R.id.sixth).setOnClickListener(this);
		findViewById(R.id.susp).setOnClickListener(this);
	}

	private void initPd() {
		File dir = getFilesDir();
		File patchFile = new File(dir, "chords.pd");
		try {
			IoUtils.extractZipResource(getResources().openRawResource(R.raw.patch), dir, true);
			patch = PdUtils.openPatch(patchFile.getAbsolutePath());
		} catch (IOException e) {
			Log.e(PD_CIRCLE, e.toString() + "; exiting now");
			finish();
		}
	}

	private void cleanup() {
		// make sure to release all resources
		PdAudio.stopAudio();
		PdUtils.closePatch(patch);
		PdBase.release();
	}

	public void playChord(boolean major, int n) {
		PdBase.sendList("playchord", option + (major ? 1 : 0), n);
	}
	
	public void endChord() {
		PdBase.sendBang("endchord");
		resetOptions();
	}

	public void setTop(int top) {
		PdBase.sendFloat("shift", top);
		getPreferences(MODE_PRIVATE).edit().putInt(TOP, top).commit();
	}

	@Override
	public void onClick(View v) {
		int newOption;
		switch (v.getId()) {
		case R.id.domdim:
			newOption = 2;
			break;
		case R.id.majmin:
			newOption = 4;
			break;
		case R.id.sixth:
			newOption = 6;
			break;
		case R.id.susp:
			newOption = 8;
			break;
		default:
			newOption = 0;
			break;
		}
		if (option == newOption) {
			resetOptions();
		} else {
			option = newOption;
		}
	}

	private void resetOptions() {
		option = 0;
		options.clearCheck();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.circle_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		switch (item.getItemId()) {
		case R.id.about_item:
			ad.setTitle(R.string.about_title);
			ad.setMessage(R.string.about_msg);
			break;
		case R.id.help_item:
			ad.setTitle(R.string.help_title);
			ad.setMessage(R.string.help_msg);
			break;
		default:
			break;
		}
		ad.setNeutralButton(android.R.string.ok, null);
		ad.setCancelable(true);
		ad.show();
		return true;
	}
}