package com.connriver.connrail;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

public class ConsistListActivity extends AppCompatActivity {

    private ListView lv;
    private ConsistList cl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_add);

        lv = (ListView) findViewById(R.id.lvMain);
        cl = new ConsistList(lv, getBaseContext(), MainActivity.getConsistList());

        cl.resetList();

        // ListView Item Click Listener
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                manageConsist(position);
            }

        });

        final Button btnConsistAdd = (Button) findViewById(R.id.btnAdd);
        btnConsistAdd.setText(R.string.button_add_train);
        btnConsistAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addConsist();
            }
        });
    }

    @Override
    protected void onResume() {
        cl.resetList();
        super.onResume();
    }

    private boolean dupFound(String sName) {
        // check for duplicate and message if found
        for (ConsistData cd :  MainActivity.getConsistList()) {
            if (sName.equals(cd.getName())) {
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

    private void addConsist() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_consist_info, null);
        builder.setView(dialogView);
        builder.setTitle(R.string.new_consist);

        final TextInputEditText etName = (TextInputEditText) dialogView.findViewById(R.id.etConsistName);
        final TextInputEditText etDesc = (TextInputEditText) dialogView.findViewById(R.id.etConsistDesc);
        builder.setPositiveButton(R.string.button_ok, null);

        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        final AlertDialog ad = builder.create();
        ad.show();

        ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        Button ok = ad.getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sName = Utils.trim(etName);
                if (badName(sName) || dupFound(sName)) {
                    return;
                }
                ConsistData cd = new ConsistData(sName, Utils.trim(etDesc));
                MainActivity.consistAddEditDelete(cd, false);
                cl.resetList();
                ad.dismiss();
            }
        });
    }

    private void manageConsist(int index) {
        Intent intent = new Intent(getBaseContext(), ConsistActivity.class);
        intent.putExtra(MainActivity.CONSIST_DATA, cl.getConsistData(index));
        startActivity(intent);
    }
}
