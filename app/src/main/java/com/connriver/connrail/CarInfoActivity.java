package com.connriver.connrail;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by bbrown on 3/16/2018.
 */

public class CarInfoActivity extends AppCompatActivity implements DialogFragmentSpotList.EditDialogListener {

    private CarData cd = null;
    private String sParent = null;
    private String sCurrentTown = null;

    @Override
    public Intent getSupportParentActivityIntent() {
        return getParentActivity();
    }

    @Override
    public Intent getParentActivityIntent() {
        return getParentActivity();
    }

    private Intent getParentActivity() {
        Intent i;

        if (sParent != null && sParent.equals(MainActivity.PARENT_YARD)) {
            i = new Intent(this, YardListActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            i = new Intent(this, ConsistActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
         }
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_info);

        // get the passed in CarData
        Intent intent = getIntent();
        sParent = intent.getStringExtra(MainActivity.CAR_INFO_PARENT);
        sCurrentTown = intent.getStringExtra(MainActivity.TOWN_NAME);
        cd = (CarData) intent.getSerializableExtra(MainActivity.CAR_DATA);
        if (cd == null) {
            return;
        }

        setTitle(cd.getInfo());

        showCurrent();

        // destination location block
        LinearLayout llDest = (LinearLayout) findViewById(R.id.llDestination);
        LinearLayout llHold = (LinearLayout) findViewById(R.id.llHold);
        LinearLayout llErrDest = (LinearLayout) findViewById(R.id.llErrDest);
        if (cd.invalidSpots()) { // bad spots defined
            // hide the target spot data and hold message
            llDest.setVisibility(View.GONE);
            llErrDest.setVisibility(View.VISIBLE);
        } else if (MainActivity.getSessionNumber() < cd.getHoldUntilDay()) { // if not enough sessions/days have expired since the car was dropped off, display hold message
            llDest.setVisibility(View.GONE);
            llHold.setVisibility(View.VISIBLE);
        } else {
            TextView tvTownDest = (TextView) findViewById(R.id.dest_town);
            TextView tvIndustryDest = (TextView) findViewById(R.id.dest_industry);
            TextView tvTrackDest = (TextView) findViewById(R.id.dest_track);
            Button btnDeliver = (Button) findViewById(R.id.btnDeliver);
            CarSpotData csd = cd.getNextSpot();
            if (csd != null) {
                final SpotData dest_sd = Utils.getSpotFromID(csd.getID());
                if (dest_sd != null) {

                    tvTownDest.setText(dest_sd.getTown());
                    tvIndustryDest.setText(dest_sd.getIndustry());
                    tvTrackDest.setText(dest_sd.getTrack());

                    btnDeliver.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            cd.setCurrentLoc(dest_sd.getID());
                            done();
                        }
                    });
                }
                String sx = csd.getLading();
                if (sx != null && !sx.isEmpty()) {
                    LinearLayout llLading = (LinearLayout) findViewById(R.id.llDestLading);
                    TextView tvLadingDest = (TextView) findViewById(R.id.tvDestLading);
                    llLading.setVisibility(View.VISIBLE);
                    tvLadingDest.setText(sx);
                }
                sx = csd.getInstructions();
                if (sx != null && !sx.isEmpty()) {
                    LinearLayout llInst = (LinearLayout) findViewById(R.id.llDestInstructions);
                    TextView tvInstructDest = (TextView) findViewById(R.id.tvDestInstructions);
                    llInst.setVisibility(View.VISIBLE);
                    tvInstructDest.setText(sx);
                }
            }
        }

        Button btnSetout = (Button) findViewById(R.id.btnSetout);
        btnSetout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setout();
            }
        });
    }

    private void showCurrent() {
        LinearLayout llCurr = (LinearLayout) findViewById(R.id.llCurrent);

        // first check if this car is in a consist
        ConsistData cond = Utils.getConsistFromID(cd.getConsist());
        if (cond != null) {
            llCurr.setVisibility(View.GONE);
            LinearLayout llConsist = (LinearLayout) findViewById(R.id.llConsist);
            llConsist.setVisibility(View.VISIBLE);
            TextView tvName = (TextView) findViewById(R.id.tvConsistName);
            TextView tvDesc = (TextView) findViewById(R.id.tvConsistDescription);
            tvName.setText(cond.getName());
            tvDesc.setText(cond.getDescription());
            return;
        }

        // then check if there is no current spot
        SpotData cur_sd = Utils.getSpotFromID(cd.getCurrentLoc());
        if (cur_sd == null) {
            LinearLayout llNoCurr = (LinearLayout) findViewById(R.id.llNoCurrent);
            llCurr.setVisibility(View.GONE);
            llNoCurr.setVisibility(View.VISIBLE);
            return;
        }

        // we have a current - show it
        TextView tvTownCurr = (TextView) findViewById(R.id.cur_town);
        TextView tvIndustryCurr = (TextView) findViewById(R.id.cur_industry);
        TextView tvTrackCurr = (TextView) findViewById(R.id.cur_track);

        tvTownCurr.setText(cur_sd.getTown());
        tvIndustryCurr.setText(cur_sd.getIndustry());
        tvTrackCurr.setText(cur_sd.getTrack());
    }

    private void setout() {
        // launch the spot list dialog and set the current car data and town selected (if any)
        DialogFragmentSpotList dFrag = DialogFragmentSpotList.newInstance();
        dFrag.setCarData(cd);
        dFrag.setShowCurrentTown(true);
        dFrag.setTown(sCurrentTown);
        dFrag.show(getFragmentManager(), "dlgSpotList");
    }

    @Override
    public void updateResult() {
        done();
    }

    // send the updated CarData back and finish
    private void done() {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.CAR_DATA, cd);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
