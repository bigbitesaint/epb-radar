package tw.com.mycompany.maptest;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.zip.Inflater;

public class MyDialog extends DialogFragment{
    MapMarker mMyMarker=null;

    public MyDialog setMarker(MapMarker MapMarker)
    {
        mMyMarker = MapMarker;
        return this;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstance)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.info_content,null);
        View loading = (View) content.findViewById(R.id.loading_panel);
        String reporter="";
        String reason="";
        String cellphone="";
        boolean needCompany=false;
        boolean needReply=false;
        if (mMyMarker != null && mMyMarker.hasData()) {
            loading.setVisibility(View.GONE);
            reporter=mMyMarker.getReporter();
            reason=mMyMarker.getReason();
            cellphone=mMyMarker.getCellphone();
            needCompany=mMyMarker.getNeedCompany();
            needReply = mMyMarker.getNeedReply();
        }else
        {
            View tableView = (View) content.findViewById(R.id.dialog_table);
            tableView.setVisibility(View.GONE);
        }

        TextView viewReporter = (TextView) content.findViewById(R.id.reporter);
        TextView viewReason = (TextView) content.findViewById(R.id.reason);
        TextView viewCellphone = (TextView) content.findViewById(R.id.cellphone);
        CheckBox checkBoxNeedCompany = (CheckBox) content.findViewById(R.id.need_company_checkbox);
        CheckBox checkBoxNeedReply = (CheckBox) content.findViewById(R.id.need_reply_checkbox);


        viewReporter.setText(reporter);
        viewReason.setText(reason);
        viewCellphone.setText(cellphone);
        checkBoxNeedCompany.setChecked(needCompany);
        checkBoxNeedReply.setChecked(needReply);


        builder.setView(content)
                .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        if (MapMarker.markerSynced.get()) {
            // if a marker has been attended, then only the attendee can modify its status
            if (mMyMarker.getType() == MapMarker.TYPE.ATTENDING && mMyMarker.equals( ((MapsActivity)getActivity()).getMap().getAttendingMarker()) ) {
                builder.setPositiveButton(getString(R.string.stop_mission), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mMyMarker.finishMission();
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel_mission),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            mMyMarker.stopMission();
                            }
                        });
            } else if ( mMyMarker.getType() == MapMarker.TYPE.UNATTENDED
                    && getActivity() != null // in case this dialogue has been detached
                    && !((MapsActivity)getActivity()).getMap().hasMission()
                    ){
                builder.setPositiveButton(getString(R.string.start_mission), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMyMarker.startMission(((MapsActivity) getActivity()).getUserId());
                    }
                });
            }
        }
        return builder.create();
    }
}
