package com.connriverlines.connrail;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import com.connriverlines.connrail.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ConsistActivity extends AppCompatActivity implements DialogFragmentCarList.DialogListener {

    private ListView lv;
    private CarList carsInConsist;
    private ConsistData consistData;
    private String sCurrentTown = null;
    private int idConsist;

    private static final int SET_CAR_INFO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consist);

        lv = (ListView) findViewById(R.id.consistView);

        //get the consist ID and name
        consistData = (ConsistData) getIntent().getSerializableExtra(MainActivity.CONSIST_DATA);
        if (consistData == null) {
            return;
        }
        idConsist = consistData.getID();

        Spinner spTown = (Spinner) findViewById(R.id.spTown);
        //fill the spinner list of towns
        final ArrayList<String> townList = Utils.getTownList(true, this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, townList);
        spTown.setAdapter(adapter);

        spTown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position == 0) {
                    sCurrentTown = null;
                } else {
                    sCurrentTown = townList.get(position);
                }
                updateView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // ListView Item Click Listener
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                manageCar(position);
            }
        });

        final Button btnConsistAdd = (Button) findViewById(R.id.btnConsistAdd);
        btnConsistAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addToConsist2();
            }
        });

        updateView();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int msgType = intent.getIntExtra(MainActivity.MSG_TYPE_TAG, -1);
            if (msgType == MainActivity.MSG_DELETE_CONSIST_DATA || msgType == MainActivity.MSG_UPDATE_CONSIST_DATA) {
                String sMsgData = intent.getStringExtra(MainActivity.MSG_DATA_TAG);
                try {
                    ConsistData cd =  new ConsistData(new JSONObject(sMsgData));
                    if (cd.getID() == consistData.getID()) {
                        if (msgType == MainActivity.MSG_DELETE_CONSIST_DATA) {
                            finish();
                        } else {
                            consistData = cd;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            updateView();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(MainActivity.INTENT_UPDATE_DATA));
        updateView();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.consist, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        menu.findItem(R.id.action_delete).setEnabled(Utils.getConsistSize(idConsist) == 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_edit:
                editConsist();
                break;
            case R.id.action_delete:
                messageDelete();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateView() {
        setTitle(consistData.getName());
        carsInConsist = new CarList(lv, this, Utils.getCarsInConsist(idConsist, sCurrentTown));
        carsInConsist.setShowCurr(false);

        carsInConsist.resetList();
    }

    private void editConsist() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View dialogView = View.inflate(this, R.layout.dialog_consist_info, null);
        builder.setView(dialogView);
        builder.setTitle(R.string.edit_consist);

        final TextInputEditText etName = (TextInputEditText) dialogView.findViewById(R.id.etConsistName);
        final TextInputEditText etDesc = (TextInputEditText) dialogView.findViewById(R.id.etConsistDesc);
        etName.setText(consistData.getName());
        etDesc.setText(consistData.getDescription());

        builder.setPositiveButton(R.string.button_ok, null);

        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        final AlertDialog ad = builder.create();
        ad.show();
        Window win = ad.getWindow();
        if (win != null) {
            win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        Button ok = ad.getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sName = Utils.trim(etName);
                if (badName(sName) || dupFound(sName)) {
                    return;
                }
                consistData.setName(sName);
                consistData.setDescription(Utils.trim(etDesc));
                MainActivity.consistAddEditDelete(consistData, false);
                updateView();
                ad.dismiss();
            }
        });
    }

    private boolean dupFound(String sName) {
        // check for duplicate and message if found
        for (ConsistData cd :  MainActivity.getConsistList()) {
            if (cd.getID() != consistData.getID() && sName.equals(cd.getName())) {
                Utils.messageBox(getResources().getString(R.string.error), getResources().getString(R.string.msg_duplicate_consist), this) ;
                return true;
            }
        }
        return false;
    }

    private boolean badName(String sName) {
        if (sName.isEmpty()) {
            Utils.messageBox(getResources().getString(R.string.error), getResources().getString(R.string.msg_bad_consist_name), this);
            return true;
        }
        return false;
    }

    private void deleteConsist() {
        // only delete empty consists
        if (Utils.getConsistSize(idConsist) != 0) {
            return;
        }
        MainActivity.consistAddEditDelete(consistData, true);
        finish();
    }

    private void addToConsist2() {
        // launch the car list dialog
        DialogFragmentCarList dFrag = DialogFragmentCarList.newInstance();
        dFrag.setTown(sCurrentTown);
        dFrag.setConsistID(idConsist);
        dFrag.show(getFragmentManager(), "dlgCarList");
    }

    @Override
    public void updateResult() {
        updateView();
    }

    private void messageDelete() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setMessage(getResources().getString(R.string.msg_delete_sure));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.button_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteConsist();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();
    }

    // launch dialog to select the drop spot
    private void manageCar(int index) {
        CarData cdSelected = carsInConsist.getCarData(index);

        // launch the car info screen
        Intent intent = new Intent(this, CarInfoActivity.class);
        intent.putExtra(MainActivity.CAR_DATA, cdSelected);
        intent.putExtra(MainActivity.CAR_INFO_PARENT, MainActivity.PARENT_TRAIN);
        intent.putExtra(MainActivity.TOWN_NAME, sCurrentTown);
        startActivityForResult(intent, SET_CAR_INFO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SET_CAR_INFO && resultCode == RESULT_OK) {
            CarData cd = (CarData) data.getSerializableExtra(MainActivity.CAR_DATA);

            MainActivity.carAddEditDelete(cd, false);

            updateView();
        }
    }

}
